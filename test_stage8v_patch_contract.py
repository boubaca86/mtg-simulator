import tempfile
import unittest
from pathlib import Path
from patch_forge_stage8v_top_level_capture import patch


class SourceHookTests(unittest.TestCase):
    def test_install_once(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            picker = root / "SpellAbilityPicker.java"
            picker.write_text('System.out.println("EXPERT_STAGE7_DATA: " + legalJson);')
            # Use the exact indentation expected by the pinned Forge patch.
            picker.write_text("                    " + picker.read_text())
            patch(root, picker)
            self.assertIn("Stage8vTopLevelTelemetry.export(", picker.read_text())
            with self.assertRaises(RuntimeError):
                patch(root, picker)

    def test_missing_anchor(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            picker = root / "SpellAbilityPicker.java"
            picker.write_text("not a picker")
            with self.assertRaises(RuntimeError):
                patch(root, picker)


if __name__ == "__main__":
    unittest.main()
