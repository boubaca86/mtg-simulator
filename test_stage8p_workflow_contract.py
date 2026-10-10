"""Workflow safety and real shell/CLI wiring, using a fake gh executable only."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

import yaml

from stage8p_one_shot_guard import GAME_STEP, JOB_NAME, WORKFLOW
from test_stage8p_one_shot_guard import job, pages, run

ROOT = Path(__file__).resolve().parent
GUARD_STEP = "Verify reserved seeds have never entered gameplay"


def workflow(name):
    # BaseLoader preserves GitHub's `on` key instead of YAML 1.1 boolean coercion.
    return yaml.load((ROOT / ".github/workflows" / name).read_text(), Loader=yaml.BaseLoader)


class Stage8PWorkflowContractTests(unittest.TestCase):
    def setUp(self):
        self.pilot = workflow(WORKFLOW)
        self.steps = self.pilot["jobs"][JOB_NAME]["steps"]
        self.guard = next(s for s in self.steps if s.get("name") == GUARD_STEP)

    def test_pilot_is_manual_main_only_with_one_fixed_non_cancelling_lock(self):
        self.assertEqual(set(self.pilot["on"]), {"workflow_dispatch"})
        self.assertEqual(self.pilot["concurrency"], {
            "group": "stage8p-reserved-seeds-20261032-20261033", "cancel-in-progress": "false",
        })
        self.assertEqual(set(self.pilot["jobs"]), {JOB_NAME})
        pilot_job = self.pilot["jobs"][JOB_NAME]
        self.assertEqual(pilot_job["if"], "github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main'")
        self.assertNotIn("continue-on-error", pilot_job)
        self.assertNotIn("strategy", pilot_job)
        self.assertEqual(self.pilot["permissions"], {"contents": "read", "actions": "read"})

    def test_guard_is_required_separate_step_immediately_before_gameplay(self):
        names = [s.get("name") for s in self.steps]
        self.assertEqual(names.count(GUARD_STEP), 1)
        self.assertEqual(names.count(GAME_STEP), 1)
        self.assertEqual(names.index(GUARD_STEP) + 1, names.index(GAME_STEP))
        for step in (self.guard, self.steps[names.index(GAME_STEP)]):
            self.assertNotIn("if", step)
            self.assertNotIn("continue-on-error", step)
        self.assertEqual(self.guard["env"]["GH_TOKEN"], "${{ github.token }}")
        self.assertEqual(self.guard["shell"], "bash")

    def test_guard_evidence_survives_a_failed_guard_or_game(self):
        upload = next(s for s in self.steps if s.get("uses", "").startswith("actions/upload-artifact@"))
        self.assertEqual(upload["if"], "always()")
        self.assertIn("stage8p-one-shot-guard.log", upload["with"]["path"].splitlines())

    def test_contract_runs_guard_and_workflow_tests_on_branch_and_pr(self):
        contract = workflow("forge-expert-ai-stage8p-contract.yml")
        self.assertIn("pull_request", contract["on"])
        self.assertIn("expert-stage8p-one-shot-seed-guard", contract["on"]["push"]["branches"])
        commands = "\n".join(s.get("run", "") for s in contract["jobs"]["test"]["steps"])
        self.assertIn("test_stage8p_one_shot_guard", commands)
        self.assertIn("test_stage8p_workflow_contract", commands)
        self.assertIn("PyYAML==6.0.3", commands)
        pilot_commands = "\n".join(s.get("run", "") for s in self.steps)
        self.assertIn("-m unittest -v test_stage8p_one_shot_guard", pilot_commands)

    def test_actual_guard_step_shell_and_cli_fail_closed(self):
        for case in ("unused", "empty-jobs", "spent-on-earlier-attempt", "api-error"):
            with self.subTest(case=case), tempfile.TemporaryDirectory() as directory:
                work = Path(directory)
                shutil.copyfile(ROOT / "stage8p_one_shot_guard.py", work / "stage8p_one_shot_guard.py")
                attempt = 2 if case == "spent-on-earlier-attempt" else 1
                runs = pages("workflow_runs", [run(attempt=attempt, status="in_progress")])
                jobs = [dict(job(attempt=attempt, job_id=attempt), status="in_progress", steps=[])]
                if case == "spent-on-earlier-attempt":
                    jobs.insert(0, job(conclusion="failure"))
                elif case == "empty-jobs":
                    jobs = []
                responses = {f"repos/owner/repo/actions/workflows/{WORKFLOW}/runs?per_page=100": runs,
                             "repos/owner/repo/actions/runs/11/jobs?filter=all&per_page=100": pages("jobs", jobs)}
                (work / "responses.json").write_text(json.dumps(responses))
                fake_gh = work / "gh"
                fake_gh.write_text(f"#!{sys.executable}\n" + '''import json, sys
from pathlib import Path
assert sys.argv[1:4] == ['api', '--paginate', '--slurp']
if Path('api-error').exists():
    sys.exit(1)
print(json.dumps(json.loads(Path('responses.json').read_text())[sys.argv[4]]))
''')
                fake_gh.chmod(0o755)
                if case == "api-error":
                    (work / "api-error").touch()
                env = dict(os.environ, PATH=f"{work}{os.pathsep}{os.environ['PATH']}",
                           GITHUB_REPOSITORY="owner/repo", GITHUB_EVENT_NAME="workflow_dispatch",
                           GITHUB_REF="refs/heads/main", GITHUB_JOB=JOB_NAME,
                           GITHUB_WORKFLOW_REF=f"owner/repo/.github/workflows/{WORKFLOW}@refs/heads/main",
                           GITHUB_RUN_ID="11", GITHUB_RUN_ATTEMPT=str(attempt), GH_TOKEN="synthetic")
                result = subprocess.run(["bash", "--noprofile", "--norc", "-e", "-o", "pipefail", "-c", self.guard["run"]],
                                        cwd=work, env=env, capture_output=True, text=True, timeout=10)
                evidence = (work / "stage8p-one-shot-guard.log").read_text()
                if case == "unused":
                    self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                    self.assertIn("guard passed", evidence)
                else:
                    self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
                    self.assertNotIn("guard passed", evidence)
                    self.assertIn("Error", evidence)


if __name__ == "__main__":
    unittest.main()
