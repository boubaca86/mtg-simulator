"""Synthetic-only regression tests for Stage 8P reserved gameplay history guard."""
import unittest

from stage8p_one_shot_guard import GAME_STEP, check_history, started_gameplay


def job(status="completed", conclusion="skipped", job_id=1):
    return {
        "id": job_id, "name": "reserved-pilot", "status": "completed",
        "steps": [{"name": GAME_STEP, "status": status, "conclusion": conclusion}],
    }


def run(run_id):
    return {"id": run_id}


class Stage8POneShotGuardTests(unittest.TestCase):
    def test_skipped_preflight_attempts_do_not_consume_seeds(self):
        runs = [{"workflow_runs": [run(11), run(12)]}]
        jobs = {11: [{"jobs": [job()]}], 12: [{"jobs": [job()]}]}
        check_history(runs, lambda rid: jobs[rid])

    def test_started_gameplay_locks_seeds_even_if_job_failed(self):
        runs = [{"workflow_runs": [run(11), run(12)]}]
        jobs = {11: [{"jobs": [job()]}],
                12: [{"jobs": [job(status="completed", conclusion="failure")]}]}
        with self.assertRaisesRegex(RuntimeError, "RESERVED SEEDS LOCKED"):
            check_history(runs, lambda rid: jobs[rid])

    def test_in_progress_gameplay_locks_seeds(self):
        self.assertTrue(started_gameplay(job(status="in_progress", conclusion=None)))

    def test_unstarted_queued_gameplay_is_not_consumed(self):
        self.assertFalse(started_gameplay(job(status="queued", conclusion=None)))

    def test_missing_gameplay_step_fails_closed(self):
        with self.assertRaises(ValueError):
            started_gameplay({"status": "completed", "steps": []})

    def test_missing_steps_fails_closed(self):
        with self.assertRaises(ValueError):
            started_gameplay({"status": "completed"})

    def test_duplicate_gameplay_step_fails_closed(self):
        j = job()
        j["steps"].append(j["steps"][0])
        with self.assertRaises(ValueError):
            started_gameplay(j)

    def test_unknown_gameplay_state_fails_closed(self):
        with self.assertRaises(ValueError):
            started_gameplay(job(status="unknown", conclusion=None))

    def test_missing_run_history_fails_closed(self):
        with self.assertRaises(ValueError):
            check_history([{"workflow_runs": []}], lambda rid: [])

    def test_missing_job_pages_fails_closed(self):
        with self.assertRaises(ValueError):
            check_history([{"workflow_runs": [run(11)]}], lambda rid: [])

    def test_incomplete_job_listing_fails_closed(self):
        with self.assertRaises(ValueError):
            check_history([{"workflow_runs": [run(11)]}], lambda rid: [{}])


if __name__ == "__main__":
    unittest.main()
