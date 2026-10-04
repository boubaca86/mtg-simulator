from __future__ import annotations

import argparse
import json
from pathlib import Path

ARMS = {
    "full_raw": False,
    "full_infoset": True,
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
    for key, infoset in ARMS.items():
        row = parse_log(root / f"{key}.log")
        row["information_set_determinization"] = infoset
        n = row["games_completed"]
        row["st_win_rate"] = row["st_wins"] / n if n else 0.0
        rows[key] = row

    status = {
        "experiment": "forge_information_set_stage4",
        "forge_tag": "forge-2.0.15",
        "games_requested_per_arm": args.games,
        "seed": args.seed,
        "arms": rows,
        "notes": [
            "Both players use Forge USE_FULL_SIMULATION.",
            "full_raw searches the copied game with Forge's ordinary hidden state.",
            "full_infoset resamples unknown opponent hand/library cards before search and preserves cards visible to the acting player.",
            "This is one deterministic information-set sample per search state; multi-sample aggregation is Stage 5.",
        ],
    }
    (root / "status.json").write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")

    lines = [
        "# Forge Expert AI — Stage 4 Information Boundary",
        "",
        f"Forge source tag: forge-2.0.15  ",
        f"Games per arm: {args.games}  ",
        f"Seed: {args.seed}",
        "",
        "Both players use Forge `USE_FULL_SIMULATION`. The only difference is whether search is allowed to inherit the real hidden hand/library assignment from the copied game.",
        "",
        "| Arm | Hidden-state treatment | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate |",
        "|---|---|---:|---:|---:|---:|---:|",
    ]
    for key in ARMS:
        row = rows[key]
        treatment = "information-set determinization" if row["information_set_determinization"] else "raw copied hidden state"
        lines.append(
            f"| {key} | {treatment} | {row['st_wins']} | {row['benchmark_wins']} | "
            f"{row['draws']} | {row['timeouts']} | {100 * row['st_win_rate']:.1f}% |"
        )

    lines += [
        "",
        "## What Stage 4 changes",
        "",
        "With information-set determinization enabled, the search copy preserves public information, hand/library counts, and cards the acting player is allowed to look at. Unknown opponent hand cards and unknown future library cards are pooled and resampled using a local deterministic RNG derived from public/count information only. The real game's RNG stream is not consumed by this resampling.",
        "",
        "This prevents a search line from relying on the opponent's actual unknown hand identity or the actual future library order. It is still not the final expert planner because one sampled hidden world can be misleading. Stage 5 will evaluate candidate actions across multiple plausible hidden worlds and aggregate their values.",
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
