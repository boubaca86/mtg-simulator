"""Fail-closed one-shot guard for Stage 8P's reserved Forge gameplay seeds.

Inspect every workflow run and job attempt before reserved gameplay begins.
A skipped or not-yet-started gameplay step does not consume reserved seeds.
"""
import json
import os
import subprocess

WORKFLOW = "forge-expert-ai-stage8p-reserved-pilot.yml"
GAME_STEP = "Run four fresh paired reserved games with one-shot public-only control"


def started_gameplay(job):
    """Return whether this job ever entered the reserved-gameplay step."""
    steps = job.get("steps")
    if not isinstance(steps, list):
        raise ValueError("Cannot verify reserved seed history: job steps unavailable")
    matches = [step for step in steps if step.get("name") == GAME_STEP]
    if not matches:
        if job.get("status") in ("queued", "in_progress"):
            # GitHub may omit future steps while the current job is running.
            return False
        raise ValueError("Cannot verify reserved seed history: gameplay step missing")
    if len(matches) != 1:
        raise ValueError("Cannot verify reserved seed history: duplicate gameplay step")
    step = matches[0]
    if step.get("conclusion") == "skipped":
        return False
    if step.get("status") == "queued":
        return False
    if step.get("status") in ("in_progress", "completed"):
        return True
    raise ValueError("Cannot verify reserved seed history: unknown gameplay step state")


def check_history(run_pages, job_pages_for_run):
    """Reject any previously started reserved gameplay, including earlier attempts.

    The current job is also inspected: before this guard completes its gameplay
    step cannot have started. GitHub workflow concurrency serializes all runs.
    """
    run_ids = set()
    for page in run_pages:
        if not isinstance(page.get("workflow_runs"), list):
            raise ValueError("Cannot verify reserved seed history: run listing incomplete")
        for run in page["workflow_runs"]:
            run_ids.add(int(run["id"]))
    if not run_ids:
        raise ValueError("Cannot verify reserved seed history: no workflow runs listed")
    for run_id in sorted(run_ids):
        pages = job_pages_for_run(run_id)
        if not pages:
            raise ValueError(f"Cannot verify reserved seed history: no jobs for run {run_id}")
        for page in pages:
            if not isinstance(page.get("jobs"), list):
                raise ValueError(f"Cannot verify reserved seed history: jobs missing for run {run_id}")
            for job in page["jobs"]:
                if job.get("name") != "reserved-pilot":
                    continue
                if started_gameplay(job):
                    raise RuntimeError(
                        f"RESERVED SEEDS LOCKED: gameplay started in run {run_id}, "
                        f"job {job.get('id')}. Never replay these seeds."
                    )


def gh_pages(path):
    result = subprocess.run(
        ["gh", "api", "--paginate", "--slurp", path],
        check=True, capture_output=True, text=True,
    )
    pages = json.loads(result.stdout)
    if not isinstance(pages, list):
        raise ValueError("GitHub API pagination did not return a page list")
    return pages


def main():
    repo = os.environ["GITHUB_REPOSITORY"]
    runs = gh_pages(f"repos/{repo}/actions/workflows/{WORKFLOW}/runs?per_page=100")
    check_history(runs, lambda run_id: gh_pages(
        f"repos/{repo}/actions/runs/{run_id}/jobs?filter=all&per_page=100"
    ))
    print("Stage 8P one-shot guard passed: no prior attempt started reserved gameplay.")


if __name__ == "__main__":
    main()
