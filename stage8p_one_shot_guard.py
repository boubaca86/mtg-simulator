"""Fail-closed history guard for Stage 8P's one-shot reserved gameplay.

Run as a separate step immediately before gameplay, inside the workflow's
fixed concurrency group. Only an explicitly skipped historical gameplay step
proves that an attempt did not consume the reserved seeds.
"""
import json
import os
import subprocess

WORKFLOW = "forge-expert-ai-stage8p-reserved-pilot.yml"
GAME_STEP = "Run four fresh paired reserved games with one-shot public-only control"
JOB_NAME = "reserved-pilot"


def _positive_int(value, label):
    if type(value) is not int or value < 1:
        raise ValueError(f"Cannot verify reserved seed history: invalid {label}")
    return value


def _complete_listing(pages, key):
    """Verify pagination counts and identities; missing history is not empty history."""
    if not isinstance(pages, list) or not pages:
        raise ValueError(f"Cannot verify reserved seed history: no {key} pages")
    records = []
    total = None
    for page in pages:
        if not isinstance(page, dict) or not isinstance(page.get(key), list):
            raise ValueError(f"Cannot verify reserved seed history: {key} listing missing")
        count = page.get("total_count")
        if type(count) is not int or count < 1:
            raise ValueError(f"Cannot verify reserved seed history: no {key} or missing count")
        if total is not None and count != total:
            raise ValueError(f"Cannot verify reserved seed history: {key} count changed")
        total = count
        records.extend(page[key])
    ids = []
    for record in records:
        if not isinstance(record, dict):
            raise ValueError(f"Cannot verify reserved seed history: malformed {key} entry")
        ids.append(_positive_int(record.get("id"), f"{key} ID"))
    if len(records) != total or len(set(ids)) != total:
        raise ValueError(f"Cannot verify reserved seed history: incomplete or duplicate {key}")
    return records


def started_gameplay(job, *, current=False):
    """Only the exact current attempt may have an omitted or queued future step."""
    steps = job.get("steps")
    if not isinstance(steps, list) or any(not isinstance(s, dict) for s in steps):
        raise ValueError("Cannot verify reserved seed history: job steps unavailable")
    matches = [step for step in steps if step.get("name") == GAME_STEP]
    if not matches:
        if current and job.get("status") == "in_progress":
            # GitHub may omit future steps from the currently executing job.
            return False
        raise ValueError("Cannot verify reserved seed history: gameplay step missing")
    if len(matches) != 1:
        raise ValueError("Cannot verify reserved seed history: duplicate gameplay step")
    step = matches[0]
    status, conclusion = step.get("status"), step.get("conclusion")
    if status == "completed" and conclusion == "skipped":
        if current:
            raise ValueError("Cannot verify reserved seed history: current gameplay already skipped")
        return False
    if status == "queued" and conclusion is None and current:
        return False
    if status in ("in_progress", "completed") and conclusion != "skipped":
        # A failed, cancelled or timed-out gameplay step still spends the seeds.
        return True
    raise ValueError("Cannot verify reserved seed history: unknown gameplay step state")


def check_history(run_pages, job_pages_for_run, *, current_run_id=None, current_attempt=None):
    """Reject started gameplay, incomplete history and concurrent active attempts.

    Jobs must include *all* attempts (GitHub's filter=all), not just the latest.
    Workflow run numbers must be contiguous from 1, so deleting a prior run
    cannot make its reserved-seed use disappear from the history check.
    """
    runs = _complete_listing(run_pages, "workflow_runs")
    numbers = sorted(_positive_int(r.get("run_number"), "run number") for r in runs)
    if numbers != list(range(1, len(runs) + 1)):
        raise ValueError("Cannot verify reserved seed history: missing or duplicate run numbers")
    if current_run_id is not None:
        _positive_int(current_run_id, "current run ID")
        _positive_int(current_attempt, "current attempt")
        if current_run_id not in {r["id"] for r in runs}:
            raise ValueError("Cannot verify reserved seed history: current run missing")
    elif current_attempt is not None:
        raise ValueError("Cannot verify reserved seed history: current run ID missing")

    for run in runs:
        run_id = run["id"]
        last_attempt = _positive_int(run.get("run_attempt"), "run attempt")
        is_current_run = run_id == current_run_id
        if is_current_run and last_attempt != current_attempt:
            raise ValueError("Cannot verify reserved seed history: current attempt mismatch")
        expected_status = "in_progress" if is_current_run else "completed"
        if run.get("status") != expected_status:
            raise ValueError(f"Cannot verify reserved seed history: run {run_id} is not {expected_status}")
        jobs = _complete_listing(job_pages_for_run(run_id), "jobs")
        attempts = []
        for job in jobs:
            if job.get("name") != JOB_NAME or job.get("run_id") != run_id:
                raise ValueError(f"Cannot verify reserved seed history: unexpected job in run {run_id}")
            attempt = _positive_int(job.get("run_attempt"), "job attempt")
            attempts.append(attempt)
            current = is_current_run and attempt == current_attempt
            expected_status = "in_progress" if current else "completed"
            if job.get("status") != expected_status:
                raise ValueError(f"Cannot verify reserved seed history: job {job['id']} is not {expected_status}")
            if started_gameplay(job, current=current):
                raise RuntimeError(
                    f"RESERVED SEEDS LOCKED: gameplay started in run {run_id}, "
                    f"attempt {attempt}, job {job['id']}. Never replay these seeds."
                )
        if sorted(attempts) != list(range(1, last_attempt + 1)):
            raise ValueError(f"Cannot verify reserved seed history: missing or duplicate attempts for run {run_id}")


def gh_pages(path):
    result = subprocess.run(
        ["gh", "api", "--paginate", "--slurp", path],
        check=True, capture_output=True, text=True, timeout=60,
    )
    pages = json.loads(result.stdout)
    if not isinstance(pages, list):
        raise ValueError("GitHub API pagination did not return a page list")
    return pages


def main():
    repo = os.environ["GITHUB_REPOSITORY"]
    expected_workflow = f"{repo}/.github/workflows/{WORKFLOW}@refs/heads/main"
    if (os.environ.get("GITHUB_EVENT_NAME") != "workflow_dispatch"
            or os.environ.get("GITHUB_REF") != "refs/heads/main"
            or os.environ.get("GITHUB_WORKFLOW_REF") != expected_workflow
            or os.environ.get("GITHUB_JOB") != JOB_NAME):
        raise ValueError("Stage 8P reserved gameplay requires the manual main-branch pilot job")
    run_id = int(os.environ["GITHUB_RUN_ID"])
    attempt = int(os.environ["GITHUB_RUN_ATTEMPT"])
    runs = gh_pages(f"repos/{repo}/actions/workflows/{WORKFLOW}/runs?per_page=100")
    check_history(runs, lambda rid: gh_pages(
        f"repos/{repo}/actions/runs/{rid}/jobs?filter=all&per_page=100"
    ), current_run_id=run_id, current_attempt=attempt)
    print("Stage 8P one-shot guard passed: no prior attempt started reserved gameplay.")


if __name__ == "__main__":
    main()
