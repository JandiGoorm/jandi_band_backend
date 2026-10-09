"""Plan by default; apply/rollback only reviewed, exact-value MySQL URL mappings."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import quote, unquote, urlsplit

import pymysql

TABLES = {'user_photo': 'user_photo_id', 'club_photo': 'club_photo_id',
          'club_gal_photo': 'club_gal_photo_id', 'notice': 'notice_id', 'promo_photo': 'promo_photo_id'}
DEFAULT_KEY = 'club-photo/rhythmeet.webp'


def key_path(value):
    if not value or '\\' in value or any(ord(c) < 32 or 127 <= ord(c) <= 159 for c in value):
        raise ValueError('Invalid object key')
    if any(p in ('', '.', '..') for p in value.split('/')):
        raise ValueError('Invalid object key segments')
    return value


def parsed(value):
    if re.search(r'%(?![0-9A-Fa-f]{2})', value) or any(c.isspace() for c in value):
        raise ValueError('Invalid URL encoding')
    u = urlsplit(value)
    if u.scheme not in ('https', 'http') or not u.hostname or u.username or u.password or '?' in value or '#' in value:
        raise ValueError('Invalid public URL')
    return (u.scheme, u.hostname, u.port or (443 if u.scheme == 'https' else 80)), unquote(u.path, errors='strict')


def managed_key(value, base):
    if not value:
        return None
    try:
        identity, path = parsed(value)
        base_identity, prefix = parsed(base.rstrip('/'))
        if identity != base_identity or not path.startswith(prefix + '/'):
            return None
        return key_path(path[len(prefix) + 1:])
    except (ValueError, UnicodeError):
        return None


def public_url(base, key):
    parsed(base)
    return base.rstrip('/') + '/' + quote(key_path(key), safe="/!$&'()*+,-.:;=@_~")


def connect():
    url = urlsplit(os.environ['DB_URL'].removeprefix('jdbc:'))
    if url.scheme != 'mysql' or url.username or url.password:
        raise ValueError('Expected credential-free JDBC MySQL URL')
    return pymysql.connect(host=url.hostname, port=url.port or 3306, database=url.path.lstrip('/'),
                           user=os.environ['DB_USERNAME'], password=os.environ['DB_PASSWORD'],
                           charset='utf8mb4', autocommit=False, connect_timeout=20,
                           read_timeout=60, write_timeout=60, cursorclass=pymysql.cursors.DictCursor)


def schema_check(db):
    with db.cursor() as cursor:
        for table, pk in TABLES.items():
            cursor.execute('SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=%s', (table,))
            if cursor.fetchone()['ENGINE'] != 'InnoDB':
                raise ValueError('Transactional InnoDB tables required')
            cursor.execute(f'SHOW COLUMNS FROM `{table}`')
            columns = {x['Field']: x for x in cursor.fetchall()}
            if columns[pk]['Key'] != 'PRI' or columns['image_url']['Type'].lower() != 'varchar(512)':
                raise ValueError('Unexpected image URL schema')


def plan(db, source, target, keys):
    parsed(source)
    parsed(target)
    if parsed(source) == parsed(target):
        raise ValueError('Source and target must differ')
    if DEFAULT_KEY not in keys:
        raise ValueError('Default club image missing')
    schema_check(db)
    result = {'version': 1, 'database': db.db.decode() if isinstance(db.db, bytes) else db.db,
              'source': source, 'target': target, 'created_utc': datetime.now(timezone.utc).isoformat(),
              'rows': [], 'missing': [], 'summary': {}}
    with db.cursor() as cursor:
        for table, pk in TABLES.items():
            cursor.execute(f'SELECT * FROM `{table}` WHERE image_url IS NOT NULL')
            counts = Counter()
            for row in cursor.fetchall():
                counts['total'] += 1
                old = row['image_url']
                key = managed_key(old, source)
                if key is None:
                    counts['external_or_unmanaged'] += 1
                    continue
                inactive = row.get('deleted_at') is not None or row.get('is_current') == 0
                if key not in keys:
                    result['missing'].append({'table': table, 'pk': row[pk], 'key': key, 'inactive': inactive})
                    counts['missing_inactive' if inactive else 'missing_active'] += 1
                    continue
                new = public_url(target, key)
                if len(new) > 512:
                    raise ValueError('New URL exceeds column size')
                result['rows'].append({'table': table, 'pk': row[pk], 'old': old, 'new': new, 'key': key})
                counts['planned'] += 1
                counts['planned_inactive' if inactive else 'planned_active'] += 1
            result['summary'][table] = dict(counts)
    db.rollback()
    result['applicable'] = not any(not x['inactive'] for x in result['missing'])
    return result


def apply(db, manifest, expected, rollback=False):
    if manifest.get('version') != 1 or not manifest.get('applicable') or len(manifest['rows']) != expected:
        raise ValueError('Manifest not applicable or expected count mismatch')
    name = db.db.decode() if isinstance(db.db, bytes) else db.db
    if name != manifest['database']:
        raise ValueError('Database does not match manifest')
    schema_check(db)
    changed = already = 0
    seen = set()
    try:
        with db.cursor() as cursor:
            for row in manifest['rows']:
                table, identity = row['table'], (row['table'], row['pk'])
                if table not in TABLES or identity in seen:
                    raise ValueError('Invalid or duplicate mapping')
                seen.add(identity)
                if managed_key(row['old'], manifest['source']) != row['key'] or public_url(manifest['target'], row['key']) != row['new']:
                    raise ValueError('Invalid URL mapping')
                pk = TABLES[table]
                old, new = (row['new'], row['old']) if rollback else (row['old'], row['new'])
                cursor.execute(f'SELECT image_url FROM `{table}` WHERE `{pk}`=%s FOR UPDATE', (row['pk'],))
                current = cursor.fetchone()
                if current and current['image_url'] == new:
                    already += 1
                    continue
                if not current or current['image_url'] != old:
                    raise ValueError('Row changed since plan; entire transaction rolled back')
                cursor.execute(f'UPDATE `{table}` SET image_url=%s WHERE `{pk}`=%s AND BINARY image_url=BINARY %s',
                               (new, row['pk'], old))
                if cursor.rowcount != 1:
                    raise ValueError('Conditional update count mismatch')
                changed += 1
        db.commit()
        return {'changed': changed, 'already_applied': already, 'rollback': rollback}
    except Exception:
        db.rollback()
        raise


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--mode', choices=['plan', 'apply', 'rollback'], default='plan')
    p.add_argument('--manifest', required=True, type=Path)
    p.add_argument('--objects', type=Path)
    p.add_argument('--source')
    p.add_argument('--target')
    p.add_argument('--expected', type=int)
    args = p.parse_args()
    os.umask(0o077)
    with connect() as db:
        if args.mode == 'plan':
            if not args.objects or not args.source or not args.target:
                p.error('plan requires --objects, --source and --target')
            objects = json.loads(args.objects.read_text(encoding='utf-8'))
            if objects.get('status') != 'verified':
                raise ValueError('Verified target object receipt required')
            result = plan(db, args.source, args.target, {x['key'] for x in objects['objects']})
            with args.manifest.open('x', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            print(json.dumps({'database': result['database'], 'applicable': result['applicable'],
                              'rows': len(result['rows']), 'summary': result['summary']}))
        else:
            if args.expected is None:
                p.error('apply/rollback requires --expected')
            result = apply(db, json.loads(args.manifest.read_text(encoding='utf-8')), args.expected,
                           args.mode == 'rollback')
            print(json.dumps(result))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('URL migration stopped:', type(error).__name__,
              str(error) if isinstance(error, ValueError) else '', file=sys.stderr)
        sys.exit(1)
