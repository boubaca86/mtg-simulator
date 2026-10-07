"""Stage 8P command line: plan or compare four reserved Forge game pairs."""
import argparse
import json
from pathlib import Path

from stage8p_reserved_planner import load_models, plan
from stage8p_reserved_audit import compare_pair
from stage8p_reserved_gate import aggregate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    for name in ("plan", "compare"):
        p = sub.add_parser(name)
        p.add_argument("outcome_checkpoint", type=Path)
        p.add_argument("validator_checkpoint", type=Path)
        if name == "plan":
            p.add_argument("baseline", type=Path)
            p.add_argument("--request", required=True, type=Path)
            p.add_argument("--metadata", required=True, type=Path)
        else:
            p.add_argument("--baseline", nargs=4, required=True, type=Path)
            p.add_argument("--controlled", nargs=4, required=True, type=Path)
            p.add_argument("--plans", nargs=4, required=True, type=Path)
            p.add_argument("--requests", nargs=4, required=True, type=Path)
            p.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    model, validator = load_models(args.outcome_checkpoint, args.validator_checkpoint)
    if args.command == "plan":
        result = plan(model, validator, args.baseline, args.request, args.metadata)
        print(json.dumps({"intervention_planned": result["intervention_planned"],
                          "model_id": result["model_id"]}, sort_keys=True))
    else:
        pairs = [compare_pair(model, validator, *items) for items in zip(
            args.baseline, args.controlled, args.plans, args.requests)]
        result = aggregate(pairs)
        args.report.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n")
        print(json.dumps({"evaluation_gate_passed": result["evaluation_gate_passed"],
                          "totals": result["totals"]}, sort_keys=True))


if __name__ == "__main__":
    main()
