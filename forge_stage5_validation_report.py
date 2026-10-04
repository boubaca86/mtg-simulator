from __future__ import annotations

import argparse
import json
import math
from pathlib import Path


def parse_log(path: Path) -> dict[str, int]:
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = [line for line in text.splitlines() if line.startswith("Game Result: Game ")]
    return {
        "games": len(lines),
        "st_wins": sum("Ai(1)-ST Forge Full has won!" in line for line in lines),
        "benchmark_wins": sum("Ai(2)-Benchmark Red Forge has won!" in line for line in lines),
        "draws": sum("ended in a Draw!" in line for line in lines),
        "timeouts": text.count("Stopping slow match as draw"),
    }


def wilson(wins: int, n: int, z: float = 1.959963984540054) -> tuple[float, float]:
    if n == 0:
        return 0.0, 1.0
    p = wins / n
    d = 1 + z*z/n
    c = (p + z*z/(2*n)) / d
    h = z * math.sqrt((p*(1-p) + z*z/(4*n))/n) / d
    return max(0.0, c-h), min(1.0, c+h)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--results", required=True)
    ap.add_argument("--games", type=int, required=True)
    ap.add_argument("--seed", type=int, required=True)
    args = ap.parse_args()
    root = Path(args.results)
    arms = ["single_full_default", "ensemble_full_default", "single_default_full", "ensemble_default_full"]
    rows = {a: parse_log(root / f"{a}.log") for a in arms}
    for a, r in rows.items():
        if r["games"] != args.games:
            raise SystemExit(f"{a}: {r['games']}/{args.games} games completed")
        r["win_rate"] = r["st_wins"] / r["games"]
        lo, hi = wilson(r["st_wins"], r["games"])
        r["wilson95"] = [lo, hi]

    status = {"experiment":"forge_stage5_validation", "seed":args.seed, "games_per_arm":args.games, "arms":rows,
              "interpretation":"Validation run only. Do not claim ensemble superiority unless the larger sample supports it."}
    (root / "validation_status.json").write_text(json.dumps(status, indent=2)+"\n", encoding="utf-8")

    out = ["# Forge Expert AI — Stage 5 Validation", "", f"Games per arm: {args.games}  ", f"Seed: {args.seed}", "",
           "This run increases the sample size without changing card scripts or gameplay semantics. Confidence intervals are Wilson 95% intervals for the S.T win rate.", "",
           "| Arm | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate | 95% CI |", "|---|---:|---:|---:|---:|---:|---:|"]
    for a in arms:
        r=rows[a]; lo,hi=r["wilson95"]
        out.append(f"| {a} | {r['st_wins']} | {r['benchmark_wins']} | {r['draws']} | {r['timeouts']} | {100*r['win_rate']:.1f}% | {100*lo:.1f}–{100*hi:.1f}% |")
    out += ["", "## Decision rule", "", "Treat this as evidence about stability and direction, not proof of expert-human strength. If three-sample search is stable and competitive, the next engineering stage should improve evaluation/opponent modelling rather than merely increasing search width."]
    (root / "validation_report.md").write_text("\n".join(out)+"\n", encoding="utf-8")
    print((root / "validation_report.md").read_text(encoding="utf-8"))

if __name__ == "__main__":
    main()
