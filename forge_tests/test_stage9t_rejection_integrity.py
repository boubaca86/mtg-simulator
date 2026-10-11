"""Synthetic evidence-gate tests; these are not real Forge gameplay results."""
from pathlib import Path
import tempfile
import unittest
from xml.etree import ElementTree as ET

from forge_tests.stage9t_rejection_integrity import (
    CLASS, EXPECTED, MARKER, REJECTIONS, disabled_validator, verify_detected_mutation,
)


class Stage9TRejectionEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.folder = Path(self.tmp.name)
        self.tree = ET.Element("testsuite", name=CLASS, tests=str(len(EXPECTED)))
        self.cases = {}
        for name in sorted(EXPECTED):
            case = ET.SubElement(self.tree, "testcase", classname=CLASS, name=name)
            self.cases[name] = case
            if name in REJECTIONS:
                ET.SubElement(case, "failure", type="java.lang.AssertionError", message=MARKER + " synthetic command")

    def write(self, name="TEST-mutation.xml"):
        ET.ElementTree(self.tree).write(self.folder / name, encoding="utf-8")

    def test_exact_three_assertions_and_twenty_passes_prove_detection(self):
        self.write()
        result = verify_detected_mutation(self.folder, 1)
        self.assertEqual(result["gate"], "PASS")
        self.assertEqual(result["executed_methods"], 23)
        self.assertEqual(result["unaffected_methods_passed"], 20)
        self.assertEqual(set(result["mutation_detected_by"]), REJECTIONS)

    def test_surviving_mutation_is_rejected(self):
        self.write()
        with self.assertRaisesRegex(ValueError, "survived"):
            verify_detected_mutation(self.folder, 0)

    def test_process_crash_does_not_count_as_detection(self):
        self.write()
        for exit_code in (-9, 137, 127):
            with self.subTest(exit_code=exit_code), self.assertRaisesRegex(ValueError, "process failure"):
                verify_detected_mutation(self.folder, exit_code)

    def test_missing_reports_and_empty_reports_are_rejected(self):
        for folder in (self.folder, self.folder / "absent"):
            with self.subTest(folder=folder), self.assertRaises(ValueError):
                verify_detected_mutation(folder, 1)

    def test_unexecuted_method_is_rejected(self):
        self.tree.remove(self.cases[sorted(EXPECTED)[0]])
        self.write()
        with self.assertRaises(ValueError):
            verify_detected_mutation(self.folder, 1)

    def test_partial_detection_is_rejected(self):
        case = self.cases[sorted(REJECTIONS)[0]]
        case.remove(case[0])
        self.write()
        with self.assertRaises(ValueError):
            verify_detected_mutation(self.folder, 1)

    def test_wrong_assertion_reason_is_rejected(self):
        self.cases[sorted(REJECTIONS)[0]][0].set("message", "unrelated assertion")
        self.write()
        with self.assertRaisesRegex(ValueError, "wrong failure"):
            verify_detected_mutation(self.folder, 1)

    def test_wrong_exception_class_is_rejected(self):
        self.cases[sorted(REJECTIONS)[0]][0].set("type", "java.lang.NullPointerException")
        self.write()
        with self.assertRaisesRegex(ValueError, "wrong failure"):
            verify_detected_mutation(self.folder, 1)

    def test_error_and_skip_are_not_expected_assertions(self):
        problem = self.cases[sorted(REJECTIONS)[0]][0]
        for tag in ("error", "skipped"):
            problem.tag = tag
            self.write()
            with self.subTest(tag=tag), self.assertRaisesRegex(ValueError, "wrong failure"):
                verify_detected_mutation(self.folder, 1)

    def test_unrelated_regression_failure_is_rejected(self):
        case = self.cases[sorted(EXPECTED - REJECTIONS)[0]]
        ET.SubElement(case, "failure", type="java.lang.AssertionError", message=MARKER)
        self.write()
        with self.assertRaisesRegex(ValueError, "unrelated regression"):
            verify_detected_mutation(self.folder, 1)

    def test_unknown_method_or_class_is_rejected(self):
        case = ET.SubElement(self.tree, "testcase", classname=CLASS, name="unexpected")
        self.write()
        with self.assertRaises(ValueError):
            verify_detected_mutation(self.folder, 1)
        case.set("classname", "different.Class")
        self.write()
        with self.assertRaisesRegex(ValueError, "unexpected class"):
            verify_detected_mutation(self.folder, 1)

    def test_duplicate_method_across_reports_is_rejected(self):
        self.write()
        self.write("TEST-duplicate.xml")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            verify_detected_mutation(self.folder, 1)

    def test_malformed_xml_is_rejected(self):
        (self.folder / "TEST-bad.xml").write_text("<testsuite>")
        with self.assertRaises(ET.ParseError):
            verify_detected_mutation(self.folder, 1)

    def test_validator_mutation_changes_only_the_selected_body(self):
        source = '''prefix
    private static void validateReply(JsonObject reply) {
        if (true) {
            Assert.fail("reject");
        }
    }
suffix
'''
        mutated = disabled_validator(source)
        self.assertTrue(mutated.startswith("prefix\n    private static void validateReply(JsonObject reply) {"))
        self.assertTrue(mutated.endswith("    }\nsuffix\n"))
        self.assertIn("MUTATION CANARY", mutated)
        self.assertNotIn('Assert.fail("reject")', mutated)
        for invalid in ("no method", source + source):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                disabled_validator(invalid)


if __name__ == "__main__":
    unittest.main()
