"""Offline adversarial tests of Stage 9B's fail-closed Surefire XML gate.

These synthetic XML fixtures validate the *evidence checker*, not Forge.
They must not be reported as live Java regressions or gameplay results.
"""
import tempfile
import unittest
from pathlib import Path
from xml.etree import ElementTree as ET

from forge_tests.verify_stage9b_surefire import CLASS, REQUIRED, check_reports


class Stage9BVerifierContractTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.names = sorted(REQUIRED)

    def write(self, filename, cases, classname=CLASS):
        root = ET.Element("testsuite", name="Stage9B", tests=str(len(cases)))
        for name, outcome in cases:
            case = ET.SubElement(root, "testcase", classname=classname, name=name)
            if outcome:
                ET.SubElement(case, outcome, message="synthetic diagnostic")
        ET.ElementTree(root).write(self.directory / filename, encoding="utf-8")

    def test_exact_two_pass(self):
        self.write("TEST-pass.xml", [(name, None) for name in self.names])
        result = check_reports(self.directory)
        self.assertEqual("PASS", result["gate"])
        self.assertEqual(2, result["executed_methods"])
        self.assertEqual(set(self.names), set(result["outcomes"]))

    def test_empty_directory_rejected(self):
        with self.assertRaisesRegex(ValueError, "zero reports"):
            check_reports(self.directory)

    def test_missing_method_rejected(self):
        self.write("TEST-missing.xml", [(self.names[0], None)])
        with self.assertRaisesRegex(ValueError, "missing"):
            check_reports(self.directory)

    def test_duplicate_method_across_reports_rejected(self):
        self.write("TEST-one.xml", [(name, None) for name in self.names])
        self.write("TEST-two.xml", [(self.names[0], None)])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            check_reports(self.directory)

    def test_unexpected_method_rejected(self):
        self.write("TEST-extra.xml", [(name, None) for name in self.names] + [("extra", None)])
        with self.assertRaisesRegex(ValueError, "unexpected"):
            check_reports(self.directory)

    def test_failure_rejected(self):
        self.write("TEST-fail.xml", [(self.names[0], "failure"), (self.names[1], None)])
        with self.assertRaisesRegex(ValueError, "failure"):
            check_reports(self.directory)

    def test_error_rejected(self):
        self.write("TEST-error.xml", [(self.names[0], "error"), (self.names[1], None)])
        with self.assertRaisesRegex(ValueError, "error"):
            check_reports(self.directory)

    def test_skipped_rejected(self):
        self.write("TEST-skip.xml", [(self.names[0], "skipped"), (self.names[1], None)])
        with self.assertRaisesRegex(ValueError, "skipped"):
            check_reports(self.directory)

    def test_unrelated_class_not_counted(self):
        self.write("TEST-other.xml", [(name, None) for name in self.names], classname="forge.ai.UnrelatedTest")
        with self.assertRaisesRegex(ValueError, "missing"):
            check_reports(self.directory)

    def test_malformed_xml_rejected(self):
        (self.directory / "TEST-bad.xml").write_text("<testsuite><testcase>", encoding="utf-8")
        with self.assertRaises(ET.ParseError):
            check_reports(self.directory)

    def test_duplicate_method_in_single_report_rejected(self):
        self.write("TEST-duplicate.xml", [(self.names[0], None), (self.names[0], None), (self.names[1], None)])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            check_reports(self.directory)

    def test_short_class_name_accepted(self):
        self.write("TEST-short.xml", [(name, None) for name in self.names], classname=CLASS.rsplit(".", 1)[-1])
        self.assertEqual("PASS", check_reports(self.directory)["gate"])


if __name__ == "__main__":
    unittest.main()
