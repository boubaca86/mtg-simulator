"""Prove Stage 9T rejection tests detect a disabled validator in disposable Forge.

The old source is a pinned negative control, never a production dependency.
Only the test class copied into a pinned upstream checkout is mutated. The
checked-in controller, worker, card definitions and seed protocols are untouched.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
from xml.etree import ElementTree as ET

from forge_tests.verify_stage9t_surefire import CLASS, EXPECTED, verify

FORGE_SHA = "4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676"
LEGACY_SHA = "d3b831d6e6ddfc4e4d17df6fb8c5c44b091b7aa7"
SOURCE_PATH = "forge_tests/Stage9TGuardedConfirmTest.java"
REJECTIONS = {
    "illegalExtraActorCommandIsRejectedBeforeForgeCanChangeState",
    "forgedCreatureCommandsAndUnsupportedCardAreRejected",
    "forgedAttackPayloadsAndOutOfTurnAccessFailClosed",
}
MARKER = "FORGED_PAYLOAD_ACCEPTED:"


def disabled_validator(source):
    """Replace only the uniquely identified test validator body, or stop."""
    pattern = re.compile(r"(    private static void validateReply\(JsonObject reply\) \{).*?^    \}", re.S | re.M)
    matches = list(pattern.finditer(source))
    if len(matches) != 1:
        raise ValueError("expected exactly one test validator method")
    return pattern.sub(lambda match: match[1] +
                       "\n        // MUTATION CANARY: accept all replies in disposable test copy only.\n    }", source)


def verify_detected_mutation(folder, maven_exit):
    """Require the exact three assertion failures; build/infra failures do not count."""
    if maven_exit == 0:
        raise ValueError("disabled-validator mutation survived")
    if maven_exit != 1:
        raise ValueError("unexpected Maven process failure")
    if not folder.is_dir():
        raise ValueError("mutation Surefire reports missing")
    seen = {}
    rejected = set()
    for path in sorted(folder.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if case.get("classname") not in (CLASS, CLASS.rsplit(".", 1)[-1]):
                raise ValueError("unexpected class in mutation reports")
            name = case.get("name")
            if not name or name in seen:
                raise ValueError("duplicate or anonymous mutation method")
            problems = [e for e in case if e.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
            seen[name] = problems
            if name in REJECTIONS:
                if len(problems) != 1:
                    raise ValueError(f"mutation was not detected exactly once by {name}")
                problem = problems[0]
                if (problem.tag.rsplit("}", 1)[-1] != "failure"
                        or problem.get("type") != "java.lang.AssertionError"
                        or MARKER not in problem.get("message", "")):
                    raise ValueError(f"wrong failure for mutation method {name}")
                rejected.add(name)
            elif problems:
                raise ValueError(f"unrelated regression failed or skipped: {name}")
    if set(seen) != EXPECTED or rejected != REJECTIONS:
        raise ValueError("mutation reports have missing or unexpected methods")
    return {"gate": "PASS", "executed_methods": len(seen),
            "mutation_detected_by": sorted(rejected),
            "unaffected_methods_passed": len(seen) - len(rejected)}


def run_variant(forge_root, evidence, name, source):
    destination = forge_root / "forge-gui-desktop/src/test/java/forge/ai/Stage9TGuardedConfirmTest.java"
    reports = forge_root / "forge-gui-desktop/target/surefire-reports"
    output = evidence / name
    output.mkdir(parents=True, exist_ok=False)
    (output / "source.java").write_text(source)
    (output / "source-sha256.txt").write_text(hashlib.sha256(source.encode()).hexdigest() + "\n")
    destination.write_text(source)
    if reports.exists():
        shutil.rmtree(reports)
    # Force recompilation rather than relying on source timestamp granularity.
    classes = forge_root / "forge-gui-desktop/target/test-classes/forge/ai"
    for compiled in classes.glob("Stage9TGuardedConfirmTest*.class"):
        compiled.unlink()
    with (output / "maven.log").open("w") as log:
        result = subprocess.run(
            ["xvfb-run", "-a", "mvn", "-B", "-ntp", "-pl", "forge-gui-desktop", "-am", "test",
             "-Dtest=Stage9TGuardedConfirmTest", "-Dsurefire.failIfNoSpecifiedTests=false"],
            cwd=forge_root, stdout=log, stderr=subprocess.STDOUT, timeout=1800,
        )
    (output / "maven-exit.txt").write_text(str(result.returncode) + "\n")
    if reports.exists():
        shutil.copytree(reports, output / "surefire")
    return result.returncode, output / "surefire"


def run_integrity(forge_root, evidence, baseline_reports):
    repo = Path(__file__).resolve().parents[1]
    source = (repo / SOURCE_PATH).read_text()
    upstream = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=forge_root, text=True).strip()
    if upstream != FORGE_SHA:
        raise ValueError("mutation check requires exact pinned Forge commit")
    baseline = verify(baseline_reports)
    legacy = subprocess.check_output(["git", "show", f"{LEGACY_SHA}:{SOURCE_PATH}"], cwd=repo, text=True)
    destination = forge_root / "forge-gui-desktop/src/test/java/forge/ai/Stage9TGuardedConfirmTest.java"
    if destination.read_text() != source:
        raise ValueError("baseline test copy must match checked-in source before mutation")
    try:
        old_exit, old_reports = run_variant(forge_root, evidence, "legacy-disabled-validator", disabled_validator(legacy))
        if old_exit != 0:
            raise ValueError("legacy negative control did not reproduce the historical false pass")
        old_gate = verify(old_reports)
        new_exit, new_reports = run_variant(forge_root, evidence, "corrected-disabled-validator", disabled_validator(source))
        mutation_gate = verify_detected_mutation(new_reports, new_exit)
    finally:
        destination.write_text(source)
    result = {"gate": "PASS", "baseline": baseline,
              "legacy_control_commit": LEGACY_SHA,
              "legacy_disabled_validator": old_gate,
              "corrected_disabled_validator": mutation_gate,
              "forge_sha": upstream, "reserved_games_run": False}
    (evidence / "gate.json").write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--forge-root", type=Path, required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--baseline-reports", type=Path, required=True)
    args = parser.parse_args()
    result = run_integrity(args.forge_root.resolve(), args.evidence.resolve(), args.baseline_reports.resolve())
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
