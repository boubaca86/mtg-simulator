from __future__ import annotations

import argparse
import json
import math
import re
from pathlib import Path

ARMS = {
    "baseline": "ST Balance Baseline",
    "no_discount": "ST Balance Drop Pod No Discount",
    "no_ping": "ST Balance Drop Pod No Ping",
    "blank": "ST Balance Drop Pod Blank",
}


def wilson_interval(wins: int, total: int, z: float = 1.96) -> tuple[float, float]:
    if total <= 0:
        return (0.0, 0.0)
    p = wins / total
    denom = 1 + z * z / total
    centre = (p + z * z / (2 * total)) / denom
    margin = z * math.sqrt((p * (1 - p) / total) + (z * z / (4 * total * total))) / denom
    return (max(0.0, centre - margin), min(1.0, centre + margin))


def diff_interval(p1: float, n1: int, p2: float, n2: int, z: float = 1.96) -> tuple[float, float]:
    if n1 <= 0 or n2 <= 0:
        return (0.0, 0.0)
    diff = p1 - p2
    se = math.sqrt(p1 * (1 - p1) / n1 + p2 * (1 - p2) / n2)
    return (diff - z * se, diff + z * se)


def parse_log(path: Path, deck_name: str) -> dict[str, int]:
    text = path.read_text(encoding="utf-8", errors="replace")
    result_lines = [line for line in text.splitlines() if line.startswith("Game Result: Game ")]
    st_wins = sum(f"Ai(1)-{deck_name} has won!" in line for line in result_lines)
    benchmark_wins = sum("Ai(2)-Benchmark Red Forge has won!" in line for line in result_lines)
    draws = sum("ended in a Draw!" in line for line in result_lines)
    return {
        "games_completed": len(result_lines),
        "st_wins": st_wins,
        "benchmark_wins": benchmark_wins,
        "draws": draws,
    }


def pct(x: float) -> str:
    return f"{100 * x:.2f}%"


def pp(x: float) -> str:
    return f"{100 * x:+.2f} pp"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", required=True)
    parser.add_argument("--games", type=int, required=True)
    parser.add_argument("--seed", type=int, required=True)
    args = parser.parse_args()

    root = Path(args.results)
    data: dict[str, dict[str, float | int | str]] = {}

    for key, deck_name in ARMS.items():
        row = parse_log(root / f"{key}.log", deck_name)
        n = int(row["games_completed"])
        wins = int(row["st_wins"])
        rate = wins / n if n else 0.0
        lo, hi = wilson_interval(wins, n)
        row.update({
            "deck_name": deck_name,
            "win_rate": rate,
            "wilson_95_low": lo,
            "wilson_95_high": hi,
        })
        data[key] = row

    baseline = data["baseline"]
    base_rate = float(baseline["win_rate"])
    base_n = int(baseline["games_completed"])

    comparisons: dict[str, dict[str, float]] = {}
    for key in ("no_discount", "no_ping", "blank"):
        row = data[key]
        variant_rate = float(row["win_rate"])
        n = int(row["games_completed"])
        # Positive means the original Drop Pod produced a higher S.T win rate.
        effect = base_rate - variant_rate
        lo, hi = diff_interval(base_rate, base_n, variant_rate, n)
        comparisons[key] = {
            "baseline_minus_variant": effect,
            "approx_95_low": lo,
            "approx_95_high": hi,
        }

    status = {
        "experiment": "drop_pod_ablation_v1",
        "forge_version": "2.0.15",
        "games_requested_per_arm": args.games,
        "seed": args.seed,
        "arms": data,
        "comparisons": comparisons,
        "method_note": "All arms use the same Forge RNG seed for reproducibility, but this is not a strict per-game paired experiment because divergent decisions can consume RNG differently.",
    }
    (root / "status.json").write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")

    lines = [
        "# Forge Balance Lab — Drop Pod S.T",
        "",
        f"Forge: 2.0.15  ",
        f"Games per arm: {args.games}  ",
        f"RNG seed: {args.seed}",
        "",
        "This experiment keeps the full 60-card S.T deck unchanged except for the four Drop Pod slots.",
        "",
        "| Arm | S.T wins | Benchmark wins | Draws | S.T win rate | 95% Wilson CI |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for key in ("baseline", "no_discount", "no_ping", "blank"):
        row = data[key]
        lines.append(
            f"| {key} | {row['st_wins']} | {row['benchmark_wins']} | {row['draws']} | "
            f"{pct(float(row['win_rate']))} | {pct(float(row['wilson_95_low']))}–{pct(float(row['wilson_95_high']))} |"
        )

    lines += [
        "",
        "## Estimated Drop Pod ability contribution",
        "",
        "Positive numbers mean the original Drop Pod won more often than that ablated version.",
        "",
        "| Comparison | Baseline − variant | Approx. 95% CI | Interpretation |",
        "|---|---:|---:|---|",
    ]

    descriptions = {
        "no_discount": "Removes only the red-creature cost reduction; keeps Defender and the block ping.",
        "no_ping": "Removes only the 1-damage block trigger; keeps Defender and the cost reduction.",
        "blank": "Keeps only the 2-mana 0/3 Artifact Creature — Vehicle body with Defender; removes both benefits.",
    }
    for key in ("no_discount", "no_ping", "blank"):
        c = comparisons[key]
        lines.append(
            f"| baseline vs {key} | {pp(c['baseline_minus_variant'])} | "
            f"{pp(c['approx_95_low'])} to {pp(c['approx_95_high'])} | {descriptions[key]} |"
        )

    lines += [
        "",
        "## How to read this",
        "",
        "This is an ability-ablation experiment, not a final global balance verdict. It tells us how much the Drop Pod text changes S.T performance against this particular Benchmark Red deck under Forge's current AI.",
        "",
        "The arms reuse the same Forge RNG seed so the experiment is reproducible. Forge exposes a simulation seed (`-s`), but because the game trees can diverge after the card change, this version is not a strict one-to-one paired trial. A later Forge bridge can give us exact per-game paired seeds and stronger search AI.",
        "",
        "Do not rebalance a card from one opponent archetype alone. The next expansion after this test is to repeat the same ablation protocol against multiple archetypes.",
    ]

    (root / "latest_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    for key, row in data.items():
        if int(row["games_completed"]) != args.games:
            raise SystemExit(f"{key} completed {row['games_completed']}/{args.games} games")
        total = int(row["st_wins"]) + int(row["benchmark_wins"]) + int(row["draws"])
        if total != args.games:
            raise SystemExit(f"{key} result accounting mismatch: {total}/{args.games}")

    print((root / "latest_report.md").read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()
