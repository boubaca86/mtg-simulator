"""Stage 8X guard: prohibit simulations that sample actual secret enemy cards.

Emergency conservative boundary, NOT a completed learned observed-only planner.
Full/hybrid simulation selection falls back to stock Forge heuristics. Those
heuristics require a separate information-flow audit before certification.
"""
from __future__ import annotations
import argparse
from pathlib import Path

def replace_once(text: str, old: str, new: str, label: str) -> str:
    hits=text.count(old)
    if hits != 1:
        raise ValueError(f"{label}: expected exactly one patch anchor; got {hits}")
    return text.replace(old,new,1)

def patch_ai_controller(source: str) -> str:
    return replace_once(source,
        """    public boolean usesHybridSimulation() {
        return simMode == AIOption.USE_HYBRID_SIMULATION;
    }
    public boolean usesFullSimulation() {
        return simMode == AIOption.USE_FULL_SIMULATION;
    }
""",
        """    // Stage 8X: old copied-world search knows the opponent's actual hidden
    // deck composition. Do not execute it in observed-only mode.
    // The regular heuristic fallback is not yet certified noninterfering.
    private static boolean expertObservedOnly() {
        return Boolean.parseBoolean(System.getProperty("forge.expert.observedOnly", "true"));
    }

    public boolean usesHybridSimulation() {
        return !expertObservedOnly() && simMode == AIOption.USE_HYBRID_SIMULATION;
    }
    public boolean usesFullSimulation() {
        return !expertObservedOnly() && simMode == AIOption.USE_FULL_SIMULATION;
    }
""", "AiController mode gate")

def patch_game_simulator(source: str) -> str:
    return replace_once(source,
        """    public GameSimulator(SimulationController controller, Game origGame, Player origAiPlayer, PhaseType advanceToPhase) {
        this.controller = controller;
""",
        """    public GameSimulator(SimulationController controller, Game origGame, Player origAiPlayer, PhaseType advanceToPhase) {
        if (Boolean.parseBoolean(System.getProperty("forge.expert.observedOnly", "true"))) {
            throw new IllegalStateException("Stage 8X: old opponent-hidden-card simulations are forbidden in observed-only mode");
        }
        this.controller = controller;
""", "GameSimulator direct invocation guard")

def patch_sources(ai_controller: Path, game_simulator: Path) -> None:
    # Prepare both before any write, fail closed on source drift.
    ai_new=patch_ai_controller(ai_controller.read_text(encoding="utf-8"))
    sim_new=patch_game_simulator(game_simulator.read_text(encoding="utf-8"))
    ai_controller.write_text(ai_new,encoding="utf-8")
    game_simulator.write_text(sim_new,encoding="utf-8")

if __name__ == "__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--ai-controller",required=True,type=Path)
    p.add_argument("--game-simulator",required=True,type=Path)
    a=p.parse_args()
    patch_sources(a.ai_controller,a.game_simulator)
    print("Stage 8X guard installed; hidden-deck search disabled by default")
