import argparse
from pathlib import Path

def patch(java_root, picker):
    text = picker.read_text()
    anchor = '                    System.out.println("EXPERT_STAGE7_DATA: " + legalJson);'
    if text.count(anchor) != 1:
        raise RuntimeError("Missing Stage 7 capture anchor")
    extra = '''
                    if (Boolean.getBoolean("forge.expert.stage8v.top_level")) {
                        System.out.println("EXPERT_STAGE8V_TOP_LEVEL: "
                            + Stage8vTopLevelTelemetry.export(
                                deterministicSimulationSeed(), decisionIndex,
                                candidateSAs.size(), bestIndex));
                    }'''
    target = java_root / "forge/ai/simulation/Stage8vTopLevelTelemetry.java"
    if target.exists():
        raise RuntimeError("Already installed")
    helper = (Path(__file__).parent / "forge_ai/Stage8vTopLevelTelemetry.java").read_text()
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(helper)
    picker.write_text(text.replace(anchor, anchor + extra, 1))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--forge-ai-java", type=Path, required=True)
    parser.add_argument("--spell-picker", type=Path, required=True)
    a = parser.parse_args()
    patch(a.forge_ai_java, a.spell_picker)
