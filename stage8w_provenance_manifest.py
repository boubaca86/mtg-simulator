"""Deterministic, read-only identity for a verified single-game Stage 8V log."""
import hashlib
import json


def session_id(manifest):
    required = ("github_run_id", "github_run_attempt", "job_id", "game_ordinal",
                "game_count", "orientation", "matchup_id", "run_seed", "log_sha256")
    if set(manifest) != set(required):
        raise ValueError("Incomplete provenance")
    if type(manifest["game_count"]) is not int or manifest["game_count"] != 1:
        raise ValueError("Single-game log required")
    if type(manifest["game_ordinal"]) is not int or manifest["game_ordinal"] != 0:
        raise ValueError("Invalid game ordinal")
    if manifest["orientation"] not in ("st-first", "red-first"):
        raise ValueError("Unknown orientation")
    if not isinstance(manifest["matchup_id"], str) or not manifest["matchup_id"]:
        raise ValueError("Missing matchup")
    if any(type(manifest[key]) is not int or manifest[key] <= 0
           for key in ("github_run_id", "github_run_attempt")):
        raise ValueError("Invalid run identity")
    if type(manifest["run_seed"]) is not int:
        raise ValueError("Invalid seed")
    digest = manifest["log_sha256"]
    if not isinstance(digest, str) or len(digest) != 64:
        raise ValueError("Missing log digest")
    payload = json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(payload).hexdigest()
