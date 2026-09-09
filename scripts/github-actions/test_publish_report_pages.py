import argparse
import importlib.util
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("publish-report-pages.py")
SPEC = importlib.util.spec_from_file_location("publish_report_pages", SCRIPT)
publish_report_pages = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(publish_report_pages)


class ReportDestinationTest(unittest.TestCase):
    def test_standalone_run_uses_existing_runs_directory(self):
        self.assertEqual(Path("runs/42"), publish_report_pages.report_destination("42", None))

    def test_pull_request_uses_stable_pull_request_directory(self):
        self.assertEqual(Path("pr-17"), publish_report_pages.report_destination("42", "17"))

    def test_pull_request_number_must_be_positive(self):
        for invalid in ("", "0", "-1", "17/../../main"):
            with self.subTest(invalid=invalid), self.assertRaises(argparse.ArgumentTypeError):
                publish_report_pages.positive_pr_number(invalid)


if __name__ == "__main__":
    unittest.main()
