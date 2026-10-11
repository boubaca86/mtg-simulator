"""Synthetic evidence checks; these are not Forge gameplay tests."""
from pathlib import Path
import tempfile
import unittest
from xml.etree import ElementTree as ET

from forge_tests.verify_stage9u_surefire import CLASS, EXPECTED, verify


class Stage9UEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.root = ET.Element("testsuite")
        for name in sorted(EXPECTED):
            ET.SubElement(self.root, "testcase", classname=CLASS, name=name)

    def write(self):
        ET.ElementTree(self.root).write(self.folder / "TEST-stage9u.xml")

    def test_exact_named_execution_passes(self):
        self.write()
        self.assertEqual(verify(self.folder)["executed_methods"], 31)

    def test_missing_or_empty_reports_fail(self):
        for folder in (self.folder, self.folder / "absent"):
            with self.subTest(folder=folder), self.assertRaises(ValueError):
                verify(folder)

    def test_missing_regression_or_trigger_fails(self):
        for index in (0, len(self.root) - 1):
            case = self.root[index]
            self.root.remove(case)
            self.write()
            with self.assertRaises(ValueError):
                verify(self.folder)
            self.root.insert(index, case)

    def test_failure_error_and_skip_fail(self):
        for kind in ("failure", "error", "skipped"):
            problem = ET.SubElement(self.root[0], kind)
            self.write()
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                verify(self.folder)
            self.root[0].remove(problem)

    def test_wrong_or_short_class_name_fails(self):
        for name in ("forge.ai.Stage9TGuardedConfirmTest", "Stage9UTriggerBoundaryTest", ""):
            self.root[0].set("classname", name)
            self.write()
            with self.subTest(name=name), self.assertRaises(ValueError):
                verify(self.folder)

    def test_duplicate_across_files_fails(self):
        self.write()
        duplicate = ET.Element("testsuite")
        duplicate.append(self.root[0])
        ET.ElementTree(duplicate).write(self.folder / "TEST-duplicate.xml")
        with self.assertRaises(ValueError):
            verify(self.folder)

    def test_extra_or_anonymous_method_fails(self):
        for name in ("unexpected", ""):
            extra = ET.SubElement(self.root, "testcase", classname=CLASS, name=name)
            self.write()
            with self.subTest(name=name), self.assertRaises(ValueError):
                verify(self.folder)
            self.root.remove(extra)

    def test_malformed_xml_fails(self):
        (self.folder / "TEST-broken.xml").write_text("<testsuite>")
        with self.assertRaises(ET.ParseError):
            verify(self.folder)


if __name__ == "__main__":
    unittest.main()
