"""Unique UTC release identity, independently testable without Android tooling."""
import json
import os
from datetime import datetime, timezone

EPOCH = datetime(2020, 1, 1, tzinfo=timezone.utc)

def metadata(now, run_number, attempt):
    if now.tzinfo is None:
        raise ValueError('Timezone is required')
    utc = now.astimezone(timezone.utc)
    version_code = int((utc - EPOCH).total_seconds())
    if not 1 <= version_code <= 2_100_000_000:
        raise ValueError('Android version code is out of range')
    if run_number < 1 or attempt < 1:
        raise ValueError('Positive workflow run identifiers are required')
    stamp = utc.strftime('%Y-%m-%d_%H-%M-%S')
    identity = f'{stamp}-r{run_number}-a{attempt}'
    return {'tag': f'android-{identity}', 'filename': f'ZenithGameBox-{identity}.apk',
            'version_name': identity, 'version_code': str(version_code)}

if __name__ == '__main__':
    values = metadata(datetime.now(timezone.utc), int(os.environ['GITHUB_RUN_NUMBER']), int(os.environ['GITHUB_RUN_ATTEMPT']))
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        for key, value in values.items():
            output.write(f'{key}={value}\n')
