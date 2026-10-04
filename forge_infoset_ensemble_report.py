from __future__ import annotations

import argparse
import json
from pathlib import Path

ARMS = {
    "single_full_default": (1, "full", "default"),
    "ensemble_full_default": (3, "full", "default"),
    "single_default_full": (1, "default", "full"),
    "ensemble_default_full": (3, "default", "full"),
}


def parse_log(path: Path) -> dict[str, int]:
    text = path.read_text(encoding="utf-8", errors="replace")
    result_lines = [line for line in text.splitlines() if line.startswith("Game Result: Game ")]
    st_wins = sum("Ai(1)-ST Forge Full has won!" in line for line in result_lines)
    benchmark_wins = sum("Ai(2)-Benchmark Red Forge has won!" in line for line in result_lines)
    draws = sum("ended in a Draw!" in line for line in result_lines)
    timeouts = text.count("Stopping slow match as draw")
    return {
        "games_completed": len(result_lines),
        "st_wins": st_wins,
        "benchmark_wins": benchmark_wins,
        "draws": draws,
        "timeouts": timeouts,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", required=True)
    parser.add_argument("--games", type=int, required=True)
    parser.add_argument("--seed", type=int, required=True)
    args = parser.parse_args()

    root = Path(args.results)
    rows = {}
    for key, (samples, st_mode, benchmark_mode) in ARMS.items():
        row = parse_log(root / f"{key}.log")
        row["information_set_samples"] = samples
        row["st_mode"] = st_mode
        row["benchmark_mode"] = benchmark_mode
        n = row["games_completed"]
        row["st_win_rate"] = row["st_wins"] / n if n else 0.0
        rows[key] = row

    status = {
        "experiment": "forge_information_set_ensemble_stage5",
        "forge_tag": "forge-2.0.15",
        "games_requested_per_arm": args.games,
        "seed": args.seed,
        "arms": rows,
        "notes": [
            "All full-simulation players use the Stage 4 hidden-information boundary.",
            "single arms use one determinization per root decision.",
            "ensemble arms score each candidate root action across three determinizations and use the mean score.",
            "Only the chosen root action is retained; the next real priority window re-searches instead of following a multi-step plan tailored to one sampled hidden world.",
            "This is root-sampled information-set search, not yet full IS-MCTS or a trained expert agent.",
        ],
    }
    (root / "status.json").write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")

    lines = [
        "# Forge Expert AI — Stage 5 Multi-Sample Information-Set Search",
        "",
        f"Forge source tag: forge-2.0.15  ",
        f"Games per arm: {args.games}  ",
        f"Seed: {args.seed}",
        "",
        "Stage 5 evaluates a root action across several plausible opponent hidden states instead of trusting a single determinization.",
        "",
        "| Arm | Samples | S.T AI | Benchmark AI | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate |",
        "|---|---:|---|---|---:|---:|---:|---:|---:|",
    ]
    for key in ARMS:
        row = rows[key]
        lines.append(
            f"| {key} | {row['information_set_samples']} | {row['st_mode']} | {row['benchmark_mode']} | "
            f"{row['st_wins']} | {row['benchmark_wins']} | {row['draws']} | {row['timeouts']} | "
            f"{100 * row['st_win_rate']:.1f}% |"
        )

    lines += [
        "",
        "## Search behavior",
        "",
        "For the three-sample arms, every legal root spell/ability candidate is evaluated in three hidden-world determinizations consistent with the information available to the acting player. Scores are averaged, and the candidate with the best mean value is selected.",
        "",
        "Recursive lookahead within each sampled world still uses Forge Full Simulation. To avoid strategy fusion, the executable plan retains only the selected root action; subsequent real priority windows perform a fresh information-set search.",
        "",
        "This stage is an engineering and strength smoke test. The game count is intentionally small. A later stage must increase samples, add downside/risk handling, improve opponent-response search, and replace the hand-written state evaluator with a learned win-probability model.",
    ]
    (root / "latest_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    for key, row in rows.items():
        if row["games_completed"] != args.games:
            raise SystemExit(f"{key}: completed {row['games_completed']}/{args.games} games")
        accounted = row["st_wins"] + row["benchmark_wins"] + row["draws"]
        if accounted != args.games:
            raise SystemExit(f"{key}: result accounting mismatch {accounted}/{args.games}")

    print((root / "latest_report.md").read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()
