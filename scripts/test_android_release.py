import unittest
from datetime import datetime, timezone, timedelta
from importlib.util import spec_from_file_location, module_from_spec
from pathlib import Path

spec = spec_from_file_location('release_rules', Path(__file__).with_name('android-release.py'))
rules = module_from_spec(spec)
spec.loader.exec_module(rules)

class ReleaseRules(unittest.TestCase):
    def test_filename_has_exact_utc_date_and_time(self):
        local = datetime(2026, 11, 7, 6, 24, tzinfo=timezone(timedelta(hours=1)))
        result = rules.metadata(local, 12, 1)
        self.assertEqual(result['filename'], 'ZenithGameBox-2026-11-07_05-24-00-r12-a1.apk')
    def test_rerun_is_never_same_release(self):
        now = datetime(2026, 11, 7, 5, 24, tzinfo=timezone.utc)
        self.assertNotEqual(rules.metadata(now, 12, 1)['tag'], rules.metadata(now, 12, 2)['tag'])
    def test_newer_generation_has_higher_android_update_number(self):
        now = datetime(2026, 11, 7, 5, 24, tzinfo=timezone.utc)
        first = int(rules.metadata(now, 12, 1)['version_code'])
        second = int(rules.metadata(now + timedelta(seconds=1), 13, 1)['version_code'])
        self.assertEqual(first, 216277440)
        self.assertEqual(second, first + 1)
    def test_ambiguous_local_time_is_rejected(self):
        with self.assertRaises(ValueError): rules.metadata(datetime(2026, 11, 7), 1, 1)

if __name__ == '__main__': unittest.main()
