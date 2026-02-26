import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
VALIDATION_DIR = ROOT / "tools" / "validation"
if str(VALIDATION_DIR) not in sys.path:
    sys.path.insert(0, str(VALIDATION_DIR))

import generate_test_telemetry_summary as telemetry  # noqa: E402


class TestGenerateTestTelemetrySummary(unittest.TestCase):
    def test_collect_report_parses_module_stats_and_failures(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            report_file = (
                root
                / "customer-context"
                / "customer-domain"
                / "build"
                / "test-results"
                / "test"
                / "TEST-com.bank.CustomerTest.xml"
            )
            report_file.parent.mkdir(parents=True, exist_ok=True)
            report_file.write_text(
                "\n".join(
                    [
                        '<testsuite tests="2" failures="1" errors="0" skipped="0" time="1.25">',
                        '  <testcase classname="com.bank.CustomerTest" name="shouldPass" time="0.30"/>',
                        '  <testcase classname="com.bank.CustomerTest" name="shouldFail" time="0.40">',
                        '    <failure message="expected true was false"/>',
                        "  </testcase>",
                        "</testsuite>",
                    ]
                )
                + "\n",
                encoding="utf-8",
            )

            report = telemetry.collect_report(root)
            self.assertEqual(len(report.result_files), 1)
            self.assertIn("customer-context/customer-domain", report.module_stats)
            stats = report.module_stats["customer-context/customer-domain"]
            self.assertEqual(stats.tests, 2)
            self.assertEqual(stats.failures, 1)
            self.assertEqual(stats.errors, 0)
            self.assertEqual(stats.skipped, 0)
            self.assertAlmostEqual(stats.duration_seconds, 1.25, places=2)
            self.assertEqual(len(report.failures), 1)
            self.assertIn("com.bank.CustomerTest#shouldFail", report.failures[0].testcase)

    def test_format_markdown_handles_empty_results(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            report = telemetry.collect_report(Path(tmp_dir))
            markdown = telemetry.format_markdown(report, top_failures=5)
            self.assertIn("No JUnit XML test result files were found", markdown)

    def test_format_markdown_includes_module_breakdown_and_hotspots(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            report_file = (
                root / "loan-context" / "loan-domain" / "build" / "test-results" / "test" / "TEST-suite.xml"
            )
            report_file.parent.mkdir(parents=True, exist_ok=True)
            report_file.write_text(
                "\n".join(
                    [
                        '<testsuite tests="3" failures="0" errors="1" skipped="1" time="2.50">',
                        '  <testcase classname="com.bank.loan.LoanTest" name="ok" time="0.10"/>',
                        '  <testcase classname="com.bank.loan.LoanTest" name="error" time="0.20">',
                        '    <error message="NullPointerException"/>',
                        "  </testcase>",
                        "</testsuite>",
                    ]
                )
                + "\n",
                encoding="utf-8",
            )

            report = telemetry.collect_report(root)
            markdown = telemetry.format_markdown(report, top_failures=3)
            self.assertIn("## CI Test Telemetry", markdown)
            self.assertIn("### Module Breakdown", markdown)
            self.assertIn("loan-context/loan-domain", markdown)
            self.assertIn("### Top Failure Hotspots", markdown)
            self.assertIn("com.bank.loan.LoanTest#error", markdown)


if __name__ == "__main__":
    unittest.main()
