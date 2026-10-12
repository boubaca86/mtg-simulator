"""Evidence integrity for the inherited 84-method Stage10T suite."""
from pathlib import Path
import tempfile
import unittest
from xml.etree import ElementTree as ET
from forge_tests.verify_stage10t_surefire import CLASS, EXPECTED, NEW, PREVIOUS, verify


class Stage10TEvidenceTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)
        self.root = ET.Element("testsuite")
        for name in sorted(EXPECTED):
            ET.SubElement(self.root, "testcase", classname=CLASS, name=name)

    def write(self):
        ET.ElementTree(self.root).write(self.folder / "TEST-stage10t.xml")

    def test_exact_84_and_inherited_inventory(self):
        self.assertEqual(len(PREVIOUS), 78)
        self.assertEqual(len(NEW), 6)
        self.write()
        self.assertEqual(verify(self.folder)["executed_methods"], 84)

    def test_omitted_new_or_inherited_case_fails(self):
        for name in (sorted(NEW)[0], sorted(PREVIOUS)[0]):
            case = next(c for c in self.root if c.get("name") == name)
            self.root.remove(case)
            self.write()
            with self.assertRaises(ValueError):
                verify(self.folder)
            self.root.append(case)

    def test_wrong_class_or_missing_reports_fails(self):
        with self.assertRaises(ValueError):
            verify(self.folder)
        for name in ("Stage10TOwnHandCleanupTest", "forge.ai.Stage10SNaturalTwentyLifeTest"):
            self.root[0].set("classname", name)
            self.write()
            with self.assertRaises(ValueError):
                verify(self.folder)

    def test_duplicate_extra_or_anonymous_execution_fails(self):
        for name in (self.root[0].get("name"), "unknown", ""):
            case = ET.SubElement(self.root, "testcase", classname=CLASS, name=name)
            self.write()
            with self.assertRaises(ValueError):
                verify(self.folder)
            self.root.remove(case)

    def test_failure_error_and_skip_fails(self):
        for tag in ("failure", "error", "skipped"):
            problem = ET.SubElement(self.root[0], tag)
            self.write()
            with self.assertRaises(ValueError):
                verify(self.folder)
            self.root[0].remove(problem)


if __name__ == "__main__":
    unittest.main()
