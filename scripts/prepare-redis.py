"""Derive the local Redis password file from the manually maintained app .env."""

import json
import os
from pathlib import Path
import sys


def main():
    root = Path(sys.argv[1]).resolve()
    if root not in (Path('/opt/jandi-band/production'), Path('/opt/jandi-band/development')):
        raise ValueError('Unexpected deployment directory')
    values = dict(line.split('=', 1) for line in (root / '.env').read_text().splitlines()
                  if line and not line.startswith('#') and '=' in line)
    if values.get('SPRING_DATA_REDIS_HOST') != f'jandi-band-redis-{root.name}':
        raise ValueError('Redis host must match the deployment environment')
    if values.get('SPRING_DATA_REDIS_PORT') != '6379':
        raise ValueError('Redis port must be 6379')
    password = values['REDIS_PASSWORD']
    if len(password) < 32 or not all(33 <= ord(c) <= 126 for c in password):
        raise ValueError('Use a Redis password of at least 32 printable ASCII characters without spaces')
    path = root / 'redis-auth.conf'
    content = 'requirepass ' + json.dumps(password) + '\n'
    if path.exists() and path.read_text() != content:
        raise ValueError('Password rotation requires a coordinated Redis and application restart')
    if not path.exists():
        # Compose uses Redis UID 999 and the deployment user's group.
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o640)
        with os.fdopen(fd, 'w') as target:
            target.write(content)
        path.chmod(0o640)


if __name__ == '__main__':
    main()
