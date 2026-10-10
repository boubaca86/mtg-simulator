"""Synthetic-only regressions; these tests never launch Forge or query GitHub."""
import copy
import io
import json
import os
import subprocess
import unittest
from contextlib import redirect_stdout
from unittest.mock import patch

from stage8p_one_shot_guard import (
    GAME_STEP, JOB_NAME, WORKFLOW, check_history, main, started_gameplay,
)


def job(status="completed", conclusion="skipped", job_id=1, run_id=11, attempt=1):
    return {
        "id": job_id, "run_id": run_id, "run_attempt": attempt,
        "name": JOB_NAME, "status": "completed",
        "steps": [{"name": GAME_STEP, "status": status, "conclusion": conclusion}],
    }


def run(run_id=11, number=1, attempt=1, status="completed"):
    return {"id": run_id, "run_number": number, "run_attempt": attempt, "status": status}


def pages(key, records, page_size=100):
    return [{"total_count": len(records), key: records[i:i + page_size]}
            for i in range(0, max(1, len(records)), page_size)]


def history(runs, jobs, **kwargs):
    check_history(pages("workflow_runs", runs), lambda rid: pages("jobs", jobs[rid]), **kwargs)


class Stage8POneShotGuardTests(unittest.TestCase):
    def test_skipped_preflight_attempts_do_not_consume_seeds(self):
        history([run(), run(12, 2)], {11: [job()], 12: [job(run_id=12)]})

    def test_started_gameplay_locks_seeds_for_every_outcome(self):
        for conclusion in ("success", "failure", "cancelled", "timed_out", "neutral", None):
            with self.subTest(conclusion=conclusion), self.assertRaisesRegex(RuntimeError, "RESERVED SEEDS LOCKED"):
                history([run()], {11: [job(conclusion=conclusion)]})

    def test_in_progress_gameplay_locks_seeds(self):
        self.assertTrue(started_gameplay(job(status="in_progress", conclusion=None)))

    def test_queued_gameplay_is_only_allowed_for_current_attempt(self):
        j = job(status="queued", conclusion=None)
        j["status"] = "in_progress"
        self.assertFalse(started_gameplay(j, current=True))
        with self.assertRaises(ValueError):
            started_gameplay(j)

    def test_missing_gameplay_step_fails_closed(self):
        for status in ("completed", "queued", "in_progress"):
            with self.subTest(status=status), self.assertRaises(ValueError):
                started_gameplay({"status": status, "steps": []})

    def test_missing_steps_fails_closed(self):
        for steps in (None, {}, [None], ["step"]):
            with self.subTest(steps=steps), self.assertRaises(ValueError):
                started_gameplay({"status": "in_progress", "steps": steps}, current=True)

    def test_duplicate_gameplay_step_fails_closed(self):
        j = job()
        j["steps"].append(copy.deepcopy(j["steps"][0]))
        with self.assertRaises(ValueError):
            started_gameplay(j)

    def test_unknown_or_contradictory_gameplay_state_fails_closed(self):
        for status, conclusion in (("unknown", None), ("queued", "skipped"), ("in_progress", "skipped")):
            with self.subTest(status=status), self.assertRaises(ValueError):
                started_gameplay(job(status=status, conclusion=conclusion))

    def test_missing_run_history_fails_closed(self):
        for listing in ([], [{}], [{"workflow_runs": []}], pages("workflow_runs", [])):
            with self.subTest(listing=listing), self.assertRaises(ValueError):
                check_history(listing, lambda rid: pages("jobs", [job()]))

    def test_missing_job_pages_fails_closed(self):
        with self.assertRaises(ValueError):
            check_history(pages("workflow_runs", [run()]), lambda rid: [])

    def test_incomplete_job_listing_fails_closed(self):
        for listing in ([{}], [{"jobs": []}], pages("jobs", [])):
            with self.subTest(listing=listing), self.assertRaises(ValueError):
                check_history(pages("workflow_runs", [run()]), lambda rid: listing)

    def test_unrelated_job_cannot_stand_in_for_pilot(self):
        j = job()
        j["name"] = "other-job"
        with self.assertRaisesRegex(ValueError, "unexpected job"):
            history([run()], {11: [j]})

    def test_job_must_belong_to_listed_run(self):
        with self.assertRaisesRegex(ValueError, "unexpected job"):
            history([run()], {11: [job(run_id=12)]})

    def test_all_run_pages_are_checked(self):
        check_history(pages("workflow_runs", [run(), run(12, 2)], page_size=1),
                      lambda rid: pages("jobs", [job(run_id=rid)]))
        with self.assertRaises(RuntimeError):
            check_history(pages("workflow_runs", [run(), run(12, 2)], page_size=1),
                          lambda rid: pages("jobs", [job(run_id=rid, conclusion="failure" if rid == 12 else "skipped")]))

    def test_all_job_pages_and_old_attempts_are_checked(self):
        jobs = [job(attempt=2, job_id=2), job(conclusion="failure")]
        with self.assertRaisesRegex(RuntimeError, "attempt 1"):
            check_history(pages("workflow_runs", [run(attempt=2)]),
                          lambda rid: pages("jobs", jobs, page_size=1))

    def test_all_skipped_attempts_allow_preflight_retry(self):
        jobs = [job(), job(attempt=2, job_id=2)]
        check_history(pages("workflow_runs", [run(attempt=2)]), lambda rid: pages("jobs", jobs, page_size=1))

    def test_missing_or_duplicate_attempts_fail_closed(self):
        for jobs in ([job(attempt=2)], [job(), job(job_id=2)], [job(), job(attempt=3, job_id=2)]):
            with self.subTest(jobs=jobs), self.assertRaisesRegex(ValueError, "attempts"):
                history([run(attempt=2)], {11: jobs})

    def test_pagination_truncation_duplicates_and_changing_counts_fail_closed(self):
        for key, records in (("workflow_runs", [run(), run(12, 2)]), ("jobs", [job(), job(job_id=2, attempt=2)])):
            complete = pages(key, records, page_size=1)
            changed = copy.deepcopy(complete)
            changed[1]["total_count"] = 3
            for listing in (complete[:1], [complete[0], complete[0]], changed):
                with self.subTest(key=key, listing=listing), self.assertRaises(ValueError):
                    check_history(listing if key == "workflow_runs" else pages("workflow_runs", [run(attempt=2)]),
                                  lambda rid: listing if key == "jobs" else pages("jobs", [job(run_id=rid)]))

    def test_malformed_records_and_counts_fail_closed(self):
        for key, record in (("workflow_runs", run()), ("jobs", job())):
            bad_record = dict(record, id=True)
            for listing in ([{"total_count": True, key: [record]}],
                            [{"total_count": 1, key: [bad_record]}],
                            [{"total_count": 1, key: [None]}]):
                with self.subTest(key=key, listing=listing), self.assertRaises(ValueError):
                    check_history(listing if key == "workflow_runs" else pages("workflow_runs", [run()]),
                                  lambda rid: listing if key == "jobs" else pages("jobs", [job()]))

    def test_deleted_run_gap_or_duplicate_number_fails_closed(self):
        for runs in ([run(number=2)], [run(), run(12, 3)], [run(), run(12, 1)]):
            with self.subTest(runs=runs), self.assertRaisesRegex(ValueError, "run numbers"):
                history(runs, {r["id"]: [job(run_id=r["id"])] for r in runs})

    def test_current_run_must_be_present_with_matching_attempt(self):
        for kwargs in ({"current_run_id": 12, "current_attempt": 1},
                       {"current_run_id": 11, "current_attempt": 2},
                       {"current_run_id": 11}, {"current_attempt": 1}):
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                history([run()], {11: [job()]}, **kwargs)

    def test_only_current_running_attempt_may_omit_future_gameplay(self):
        current = job()
        current.update(status="in_progress", steps=[])
        history([run(status="in_progress")], {11: [current]}, current_run_id=11, current_attempt=1)
        current["steps"] = job(status="queued", conclusion=None)["steps"]
        history([run(status="in_progress")], {11: [current]}, current_run_id=11, current_attempt=1)

    def test_current_attempt_does_not_hide_prior_gameplay_on_rerun(self):
        current = job(attempt=2, job_id=2)
        current.update(status="in_progress", steps=[])
        for prior in (job(conclusion="failure"), dict(job(), steps=[])):
            with self.subTest(prior=prior), self.assertRaises((ValueError, RuntimeError)):
                history([run(attempt=2, status="in_progress")], {11: [prior, current]},
                        current_run_id=11, current_attempt=2)
        history([run(attempt=2, status="in_progress")], {11: [job(), current]},
                current_run_id=11, current_attempt=2)

    def test_current_gameplay_cannot_already_have_started_or_be_skipped(self):
        for conclusion in ("success", "skipped"):
            j = job(conclusion=conclusion)
            j["status"] = "in_progress"
            with self.subTest(conclusion=conclusion), self.assertRaises((ValueError, RuntimeError)):
                history([run(status="in_progress")], {11: [j]}, current_run_id=11, current_attempt=1)

    def test_other_active_run_is_ambiguous_even_before_gameplay(self):
        current = dict(job(run_id=12), status="in_progress", steps=[])
        for status in ("queued", "pending", "in_progress", "unknown"):
            with self.subTest(status=status), self.assertRaises(ValueError):
                history([run(status=status), run(12, 2, status="in_progress")],
                        {11: [dict(job(), status=status, steps=[])], 12: [current]},
                        current_run_id=12, current_attempt=1)

    def test_incomplete_historical_job_is_not_allowed(self):
        with self.assertRaises(ValueError):
            history([run()], {11: [dict(job(), status="in_progress")]})


class Stage8POneShotGuardCliTests(unittest.TestCase):
    def setUp(self):
        self.env = {
            "GITHUB_REPOSITORY": "owner/repo", "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_REF": "refs/heads/main", "GITHUB_JOB": JOB_NAME,
            "GITHUB_WORKFLOW_REF": f"owner/repo/.github/workflows/{WORKFLOW}@refs/heads/main",
            "GITHUB_RUN_ID": "11", "GITHUB_RUN_ATTEMPT": "1",
        }
        self.runs = pages("workflow_runs", [run(status="in_progress")])
        self.jobs = pages("jobs", [dict(job(), status="in_progress", steps=[])])

    @patch("stage8p_one_shot_guard.subprocess.run")
    def test_cli_paginates_runs_and_all_attempts_before_success(self, api):
        api.side_effect = [subprocess.CompletedProcess([], 0, json.dumps(p)) for p in (self.runs, self.jobs)]
        output = io.StringIO()
        with patch.dict(os.environ, self.env, clear=True), redirect_stdout(output):
            main()
        self.assertIn("guard passed", output.getvalue())
        self.assertEqual([call.args[0] for call in api.call_args_list], [
            ["gh", "api", "--paginate", "--slurp", f"repos/owner/repo/actions/workflows/{WORKFLOW}/runs?per_page=100"],
            ["gh", "api", "--paginate", "--slurp", "repos/owner/repo/actions/runs/11/jobs?filter=all&per_page=100"],
        ])
        for call in api.call_args_list:
            self.assertTrue(call.kwargs["check"])
            self.assertGreater(call.kwargs["timeout"], 0)

    @patch("stage8p_one_shot_guard.gh_pages")
    def test_cli_rejects_non_manual_or_wrong_job_context_before_api(self, api):
        for key, value in (("GITHUB_EVENT_NAME", "push"), ("GITHUB_REF", "refs/heads/feature"),
                           ("GITHUB_WORKFLOW_REF", "other-workflow"), ("GITHUB_JOB", "other-job")):
            with self.subTest(key=key), patch.dict(os.environ, dict(self.env, **{key: value}), clear=True):
                with self.assertRaises(ValueError):
                    main()
        api.assert_not_called()

    @patch("stage8p_one_shot_guard.subprocess.run")
    def test_cli_api_errors_and_invalid_json_never_report_success(self, api):
        for result in (subprocess.CalledProcessError(1, "gh"), subprocess.TimeoutExpired("gh", 60),
                       subprocess.CompletedProcess([], 0, "not-json"), subprocess.CompletedProcess([], 0, "{}")):
            api.side_effect = [result]
            output = io.StringIO()
            with self.subTest(result=result), patch.dict(os.environ, self.env, clear=True), redirect_stdout(output):
                with self.assertRaises((subprocess.SubprocessError, ValueError)):
                    main()
            self.assertNotIn("guard passed", output.getvalue())

    @patch("stage8p_one_shot_guard.subprocess.run")
    def test_cli_rejects_missing_jobs_without_success_message(self, api):
        api.side_effect = [subprocess.CompletedProcess([], 0, json.dumps(p)) for p in (self.runs, pages("jobs", []))]
        output = io.StringIO()
        with patch.dict(os.environ, self.env, clear=True), redirect_stdout(output):
            with self.assertRaises(ValueError):
                main()
        self.assertNotIn("guard passed", output.getvalue())


if __name__ == "__main__":
    unittest.main()
