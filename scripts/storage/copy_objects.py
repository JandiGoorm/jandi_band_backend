"""Copy a verified current-object backup to S3-compatible storage; never delete source."""
import argparse
import hashlib
import json
from pathlib import Path
import sys

import boto3
from botocore.config import Config


def client(config):
    return boto3.client('s3', aws_access_key_id=config['access_key'],
                        aws_secret_access_key=config['secret_key'], region_name=config['region'],
                        endpoint_url=config.get('endpoint'),
                        config=Config(retries={'max_attempts': 0}, connect_timeout=20, read_timeout=90,
                                      s3={'addressing_style': config.get('addressing_style', 'auto')},
                                      request_checksum_calculation='when_required',
                                      response_checksum_validation='when_required'))


def inventory(s3, bucket):
    return {x['Key']: {'size': x['Size'], 'etag': x['ETag']}
            for page in s3.get_paginator('list_objects_v2').paginate(Bucket=bucket)
            for x in page.get('Contents', [])}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('backup', type=Path)
    parser.add_argument('receipt', type=Path)
    args = parser.parse_args()
    config = json.load(sys.stdin)
    source, target = client(config['source']), client(config['target'])
    src_bucket, dst_bucket = config['source']['bucket'], config['target']['bucket']
    manifest = json.loads((args.backup / 'manifest.json').read_text(encoding='utf-8'))
    assert manifest['status'] == 'complete' and manifest['bucket'] == src_bucket
    assert not args.receipt.exists(), 'Receipt already exists'
    expected = {x['key']: {'size': x['size'], 'etag': x['etag']} for x in manifest['objects']}
    assert inventory(source, src_bucket) == expected, 'Source changed since backup; create fresh backup'
    existing = inventory(target, dst_bucket)
    assert not (existing.keys() - expected.keys()), 'Destination contains unexpected objects'
    receipt = {'status': 'in-progress', 'source': src_bucket, 'target': dst_bucket, 'objects': []}
    metadata_fields = ['ContentType', 'CacheControl', 'ContentDisposition', 'ContentEncoding',
                       'ContentLanguage', 'Expires', 'Metadata']
    try:
        for i, item in enumerate(manifest['objects'], 1):
            path = (args.backup / item['file']).resolve()
            assert path.is_relative_to(args.backup.resolve()), 'Invalid backup path'
            with path.open('rb') as stream:
                assert hashlib.file_digest(stream, 'sha256').hexdigest() == item['sha256']
            source_meta = source.head_object(Bucket=src_bucket, Key=item['key'], IfMatch=item['etag'])
            metadata = {k: source_meta[k] for k in metadata_fields if k in source_meta}
            if item['key'] not in existing:
                with path.open('rb') as stream:
                    target.put_object(Bucket=dst_bucket, Key=item['key'], Body=stream,
                                      ContentLength=item['size'], IfNoneMatch='*', **metadata)
            response = target.get_object(Bucket=dst_bucket, Key=item['key'])
            with response['Body'] as stream:
                digest = hashlib.sha256()
                for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                    digest.update(chunk)
            assert digest.hexdigest() == item['sha256'], 'Destination checksum mismatch'
            assert response['ContentLength'] == item['size'], 'Destination size mismatch'
            assert all(response.get(k) == value for k, value in metadata.items()), 'Metadata mismatch'
            receipt['objects'].append({'key': item['key'], 'size': item['size'], 'sha256': item['sha256']})
            if i % 25 == 0:
                print(f'Copied and verified {i}/{len(expected)}', flush=True)
        assert inventory(source, src_bucket) == expected, 'Source changed during copy'
        actual = inventory(target, dst_bucket)
        assert {k: v['size'] for k, v in actual.items()} == {k: v['size'] for k, v in expected.items()}
        receipt['status'] = 'verified'
        print(json.dumps({'target': dst_bucket, 'objects': len(expected),
                          'bytes': sum(x['size'] for x in expected.values()),
                          'sha256_and_metadata_verified': True}), flush=True)
    finally:
        args.receipt.write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding='utf-8')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Copy stopped:', getattr(error, 'response', {}).get('Error', {}).get('Code', type(error).__name__),
              str(error) if isinstance(error, AssertionError) else '', file=sys.stderr)
        sys.exit(1)
