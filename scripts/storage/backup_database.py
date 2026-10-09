"""One-time consistent logical backup; credentials stay in the child process environment."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlsplit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    os.umask(0o077)
    url = urlsplit(os.environ['DB_URL'].removeprefix('jdbc:'))
    if url.scheme != 'mysql' or url.username or url.password:
        raise ValueError('Expected credential-free JDBC URL')
    environment = dict(os.environ, MYSQL_PWD=os.environ['DB_PASSWORD'])
    command = ['mariadb-dump', '--host', url.hostname, '--port', str(url.port or 3306),
               '--user', os.environ['DB_USERNAME'], '--single-transaction', '--skip-lock-tables',
               '--no-tablespaces', '--hex-blob', '--databases', url.path.lstrip('/')]
    with args.output.open('xb') as output:
        result = subprocess.run(command, env=environment, stdout=output, stderr=subprocess.PIPE)
    if result.returncode:
        raise RuntimeError('Dump failed; partial file retained, no secrets printed')
    with args.output.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    receipt = {'database': url.path.lstrip('/'), 'bytes': args.output.stat().st_size,
               'sha256': digest, 'scope': 'single-transaction schema and data; no server users or grants'}
    args.output.with_suffix('.receipt.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
    print(json.dumps(receipt))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Backup stopped:', type(error).__name__, file=sys.stderr)
        sys.exit(1)
