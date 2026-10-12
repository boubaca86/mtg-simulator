"""Synthetic evidence checks; these are not Forge gameplay tests."""
from pathlib import Path
import tempfile
import unittest
from xml.etree import ElementTree as ET

from forge_tests.verify_stage10u_surefire import CLASS, EXPECTED, REJECTIONS, verify, verify_mutation
from forge_tests.stage11h_rejection_integrity import disabled_validator


class Stage11HEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.root = ET.Element("testsuite")
        for name in sorted(EXPECTED):
            ET.SubElement(self.root, "testcase", classname=CLASS, name=name)

    def write(self):
        ET.ElementTree(self.root).write(self.folder / "TEST-stage11h.xml")

    def test_exact_named_execution_passes(self):
        self.write()
        self.assertEqual(verify(self.folder)["executed_methods"], 108)

    def test_missing_or_empty_reports_fail(self):
        for folder in (self.folder, self.folder / "absent"):
            with self.subTest(folder=folder), self.assertRaises(ValueError):
                verify(folder)

    def test_missing_regression_or_spread_block_case_fails(self):
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
        for name in ("forge.ai.Stage9TGuardedConfirmTest", "Stage10UUncuratedDrawTest", ""):
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


    def mutation_report(self):
        self.root = ET.Element("testsuite")
        for name in sorted(REJECTIONS):
            case = ET.SubElement(self.root, "testcase", classname=CLASS, name=name)
            ET.SubElement(case, "failure", type="java.lang.AssertionError",
                          message="FORGED_PAYLOAD_ACCEPTED: forged input")
        self.write()

    def test_exact_mutation_failures_prove_detection(self):
        self.mutation_report()
        self.assertEqual(verify_mutation(self.folder, 1)["gate"], "PASS")

    def test_mutation_success_or_crash_never_counts_as_detection(self):
        self.mutation_report()
        for code in (0, 2, -9, 137):
            with self.subTest(code=code), self.assertRaises(ValueError):
                verify_mutation(self.folder, code)

    def test_mutation_skip_error_or_wrong_assertion_fails(self):
        for tag, kind, message in (
            ("skipped", "java.lang.AssertionError", "FORGED_PAYLOAD_ACCEPTED:"),
            ("error", "java.lang.AssertionError", "FORGED_PAYLOAD_ACCEPTED:"),
            ("failure", "java.lang.NullPointerException", "FORGED_PAYLOAD_ACCEPTED:"),
            ("failure", "java.lang.AssertionError", "unrelated failure"),
        ):
            self.mutation_report()
            self.root[0][0].tag = tag
            self.root[0][0].set("type", kind)
            self.root[0][0].set("message", message)
            self.write()
            with self.subTest(tag=tag, kind=kind, message=message), self.assertRaises(ValueError):
                verify_mutation(self.folder, 1)

    def test_surviving_or_missing_mutation_case_fails(self):
        self.mutation_report()
        self.root[0].remove(self.root[0][0])
        self.write()
        with self.assertRaises(ValueError):
            verify_mutation(self.folder, 1)
        self.root.remove(self.root[0])
        self.write()
        with self.assertRaises(ValueError):
            verify_mutation(self.folder, 1)

    def test_mutation_requires_one_validator_and_preserves_other_code(self):
        source = "before\n    private static void validateReply(JsonObject reply) {\n        validate(reply);\n    }\nafter"
        result = disabled_validator(source)
        self.assertTrue(result.startswith("before\n"))
        self.assertTrue(result.endswith("\nafter"))
        self.assertNotIn("validate(reply)", result)
        for invalid in ("no validator", source + "\n" + source):
            with self.assertRaises(ValueError):
                disabled_validator(invalid)


if __name__ == "__main__":
    unittest.main()
