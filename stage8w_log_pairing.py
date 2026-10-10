"""Validate Stage 8V AI-filtered telemetry against Stage 7, without gameplay."""
import hashlib
import json
from stage8w_provenance_manifest import session_id

P7 = "EXPERT_STAGE7_DATA: "
P8 = "EXPERT_STAGE8V_TOP_LEVEL: "


def audit(log_bytes, manifest):
    sid = session_id(manifest)
    if hashlib.sha256(log_bytes).hexdigest() != manifest["log_sha256"]:
        raise ValueError("Log hash mismatch")
    lines = log_bytes.decode("utf-8").splitlines()
    def rows(prefix):
        return [json.loads(line[len(prefix):]) for line in lines if line.startswith(prefix)]
    a, b = rows(P7), rows(P8)
    if not a or len(a) != len(b):
        raise ValueError("Missing paired telemetry")
    keys = set()
    for row in a:
        if row.get("run_seed") != manifest["run_seed"] or row.get("matchup_id") != manifest["matchup_id"]:
            raise ValueError("Stage 7 provenance mismatch")
        key = (row.get("run_seed"), row.get("decision_index"))
        if key in keys:
            raise ValueError("Duplicate Stage 7 decision")
        keys.add(key)
    seen = set()
    for row in b:
        if row.get("schema_version") != "stage8v-ai-filtered-top-level-v1":
            raise ValueError("Unknown Stage 8V schema")
        if (row.get("complete_legal_enumeration") is not False
                or row.get("target_combinations_enumerated") is not False
                or row.get("priority_pass_enumerated") is not False
                or row.get("selected_status") != "proposed_before_execution"):
            raise ValueError("Unsupported legality or execution claim")
        seed, idx = row.get("run_seed"), row.get("decision_index")
        count, proposed = row.get("top_level_candidate_count"), row.get("proposed_candidate_index")
        if (type(seed) is not int or seed != manifest["run_seed"] or type(idx) is not int
                or idx < 0 or type(count) is not int or count < 1
                or type(proposed) is not int or not 0 <= proposed < count):
            raise ValueError("Invalid Stage 8V decision")
        key = (seed, idx)
        if key not in keys or key in seen:
            raise ValueError("Duplicate or unpaired decision")
        seen.add(key)
    if keys != seen:
        raise ValueError("Unpaired Stage 7 decision")
    return {"session_id": sid, "decisions": len(seen),
            "complete_legal_enumeration": False, "gameplay_strength_evidence": False}
