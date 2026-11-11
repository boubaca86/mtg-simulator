# Stage 9T — repair forged-command test evidence

Source review on October 11, 2026 found a false-pass pattern in three current
Stage 9T methods: the general-command, creature-command and attack-command
rejection tests called `Assert.fail` inside the same `try` whose `catch`
accepted any `AssertionError`. If `validateReply` returned normally, the test
caught its own failure and continued. Nineteen invalid command fixtures were
therefore not reliable evidence of schema rejection.

The repair puts the failure assertion **after** the catch. Only an assertion
from `validateReply` satisfies rejection. Malformed JSON is parsed before that
catch and remains a test error. Valid actor commands and the referee's gameplay
implementation are unchanged. The existing 23 named Forge tests are preserved.

## Executed-evidence requirement

The Stage 9T workflow now requires all three of these outcomes in the pinned
Forge `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` checkout:

1. Corrected tests with the real validator: all **23** methods pass.
2. Historical tests from `d3b831d6e6ddfc4e4d17df6fb8c5c44b091b7aa7`, with
   only `validateReply` disabled in a disposable copy: all **23** methods pass.
   This is the negative control that reproduces the original false pass.
3. Corrected tests with only that validator disabled: exactly the **three**
   forged-command methods fail with `FORGED_PAYLOAD_ACCEPTED`, and the other
   **20** methods pass. A crash, build failure, different assertion, missing
   method, duplicate, error, skip or surviving mutation cannot satisfy this gate.

`forge_tests/stage9t_rejection_integrity.py` runs and verifies the two mutation
controls after the normal baseline. Every variant has its own source hash,
source copy, Maven log, process exit and Surefire XML. Reports and compiled
copies of the canary are cleared before each run. The disposable Java source
is restored in `finally`; no mutation is made to the checked-in validator or
actor worker. The complete evidence is archived even if a gate fails.

Fourteen local Python tests exercise the evidence checker and mutation scope
with synthetic XML. These are separate from the real Forge executions; they
do not count as gameplay evidence.

```bash
python3 -m unittest -v forge_tests.test_stage9t_rejection_integrity
```

## Scope and next work

This is a focused repair on the active Stage 9T test harness. Older stage
snapshots remain historical evidence and are not retroactively certified.
It adds no card rules, production gameplay, actor permissions or strategic
strength claim. It does not launch matches on evaluation seed families;
Stage 8P seeds **20261032 / 20261033 remain permanently consumed**.

Stage 9 remains a bounded two-process actor/referee prototype with genuine
Forge priority, land, mana-paid creature and unblocked-combat fixtures. Full
information isolation, blocking, general spells/triggers, complete matches,
Ultimatus end conditions and strategic AI are still open work. The planned
Stage 9U optional/mandatory-trigger investigation follows this evidence repair.

The PR targets the existing experimental branch
`expert-stage9a-forge-decision-pairs`, not production `main`. Passing CI and a
reviewed PR are required before integration; this change does not bypass that
gate or approve the whole Stage 9 branch for production.

Research: [TestNG assertion/failure semantics](https://testng.org/documentation.html)
and [Maven Surefire test execution](https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html).
The pending trigger work was also checked against the official
[Comprehensive Rules, September 25, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt),
especially 603.3–603.5 and 605.4a, and pinned Forge `PlayerControllerAi` /
`WrappedAbility`: optional choices must not be conflated with mandatory effect
resolution. No trigger behavior is changed by this repair.
