"""Mutation check of the repaired inherited Java assertions in disposable Forge."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess

from forge_tests.verify_stage10u_surefire import REJECTIONS, verify, verify_mutation

FORGE_SHA = "4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676"
SOURCE = "forge_tests/Stage10UUncuratedDrawTest.java"


def disabled_validator(source):
    pattern = re.compile(r"(    private static void validateReply\(JsonObject reply\) \{).*?^    \}", re.M | re.S)
    if len(list(pattern.finditer(source))) != 1:
        raise ValueError("expected exactly one reply validator")
    return pattern.sub(lambda m: m[1] + "\n        // DISPOSABLE MUTATION: accept all replies.\n    }", source)


def run(forge_root, baseline, evidence):
    repo = Path(__file__).resolve().parents[1]
    forge_root, evidence = forge_root.resolve(), evidence.resolve()
    if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=forge_root, text=True).strip() != FORGE_SHA:
        raise ValueError("requires exact pinned upstream Forge")
    verify(baseline)
    source = (repo / SOURCE).read_text()
    target = forge_root / "forge-gui-desktop/src/test/java/forge/ai/Stage10UUncuratedDrawTest.java"
    if target.read_text() != source:
        raise ValueError("disposable source differs from tested source")
    evidence.mkdir(parents=True, exist_ok=False)
    mutated = disabled_validator(source)
    (evidence / "mutated-source.java").write_text(mutated)
    (evidence / "mutated-source-sha256.txt").write_text(hashlib.sha256(mutated.encode()).hexdigest() + "\n")
    reports = forge_root / "forge-gui-desktop/target/surefire-reports"
    try:
        target.write_text(mutated)
        if reports.exists():
            shutil.rmtree(reports)
        classes = forge_root / "forge-gui-desktop/target/test-classes/forge/ai"
        for name in ("Stage10UUncuratedDrawTest",):
            for compiled in classes.glob(name + "*.class"):
                compiled.unlink()
        selection = "Stage10UUncuratedDrawTest#" + "+".join(sorted(REJECTIONS))
        with (evidence / "maven.log").open("w") as log:
            result = subprocess.run(
                ["xvfb-run", "-a", "mvn", "-B", "-ntp", "-pl", "forge-gui-desktop", "-am", "test",
                 "-Dtest=" + selection, "-Dsurefire.failIfNoSpecifiedTests=false"],
                cwd=forge_root, stdout=log, stderr=subprocess.STDOUT, timeout=1800)
        (evidence / "maven-exit.txt").write_text(str(result.returncode) + "\n")
        if reports.exists():
            shutil.copytree(reports, evidence / "surefire")
        print("Mutation Maven exit:", result.returncode, flush=True)
        print("\n".join((evidence / "maven.log").read_text().splitlines()[-90:]), flush=True)
        gate = verify_mutation(evidence / "surefire", result.returncode)
        (evidence / "gate.json").write_text(json.dumps(gate, sort_keys=True) + "\n")
        print(json.dumps(gate, sort_keys=True))
    finally:
        target.write_text(source)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--forge-root", type=Path, required=True)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    run(args.forge_root, args.baseline, args.evidence)
