from __future__ import annotations

import argparse
import json
from pathlib import Path

ARMS = {
    "default_default": ("default", "default"),
    "full_full": ("full", "full"),
    "full_default": ("full", "default"),
    "default_full": ("default", "full"),
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
    for key, modes in ARMS.items():
        row = parse_log(root / f"{key}.log")
        row["st_mode"] = modes[0]
        row["benchmark_mode"] = modes[1]
        n = row["games_completed"]
        row["st_win_rate"] = row["st_wins"] / n if n else 0.0
        rows[key] = row

    status = {
        "experiment": "forge_full_simulation_ai_stage3",
        "forge_tag": "forge-2.0.15",
        "games_requested_per_arm": args.games,
        "seed": args.seed,
        "arms": rows,
        "notes": [
            "default = Forge heuristic AI",
            "full = Forge USE_FULL_SIMULATION search AI",
            "This is a stronger search mode, not yet a trained or proven expert-human agent.",
        ],
    }
    (root / "status.json").write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")

    lines = [
        "# Forge Expert-AI Stage 3 Benchmark",
        "",
        f"Forge source tag: forge-2.0.15  ",
        f"Games per arm: {args.games}  ",
        f"Seed: {args.seed}",
        "",
        "This benchmark exposes Forge's built-in `USE_FULL_SIMULATION` AI to headless CLI games and compares it with the normal heuristic AI.",
        "",
        "| Arm | S.T AI | Benchmark AI | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate |",
        "|---|---|---|---:|---:|---:|---:|---:|",
    ]
    for key in ARMS:
        row = rows[key]
        lines.append(
            f"| {key} | {row['st_mode']} | {row['benchmark_mode']} | {row['st_wins']} | "
            f"{row['benchmark_wins']} | {row['draws']} | {row['timeouts']} | {100 * row['st_win_rate']:.1f}% |"
        )

    lines += [
        "",
        "## Interpretation",
        "",
        "The mixed arms are the most informative: `full_default` asks whether full-search S.T gains an edge against normal Forge AI, while `default_full` asks whether a full-search Benchmark Red gains an edge against normal S.T AI.",
        "",
        "Passing this benchmark means the stronger search mode is operational in cloud simulations. It does **not** by itself establish expert-human strength. The next architecture step is a hidden-information-safe information-set planner with deeper tree search and a learned value model.",
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
