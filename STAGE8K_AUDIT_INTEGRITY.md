# Stage 8K audit integrity

Review found a reproducible false pass in the controlled-game audit. A synthetic
move with `dispatch_success=false` and a contradictory `outcome=resolved` passed
the previous pair comparison and aggregate safety gate. The audit matched action
strings but did not apply the existing Stage 8F/H semantic checks.

The repaired audit rejects this case and retains those established acceptance
and lifecycle validators. Failed dispatches also have an explicit count in each
pair and in the aggregate safety gate; a zero anomaly count cannot conceal them.

Additional checks enforce the already declared protocol:

- Recompute the first eligible recommendation from the unchanged frozen model
  and baseline public information. Verify the model ID, exact planned decision,
  target classification and recommendation metadata.
- Verify both the request file's SHA-256 and its exact canonical contents.
  Editing the recorded digest cannot authorize a different request.
- Compare chronological public capture, pass, return, acceptance and terminal
  records up to the intervention boundary. This includes later-index phase
  probes that occur before an earlier-index current-phase action is returned.
- Require no-intervention games to preserve every recorded gameplay event and
  the terminal winner, excluding elapsed-time text.
- Validate controlled captures through the same public-input and three-world
  candidate contract; match them to legal Stage 7 observations.
- Require causal arm → capture → substitution → return ordering (arming may
  precede capture emission), reject events after game completion, and retain
  boundary/schema validation.

Eleven additional regressions exercise complete synthetic baseline/controlled
logs through the real Python parsers, public feature scorer and comparison gate.
The original false-pass example is rejected. The focused Stage 8C–8K contract
suite passes **71 tests** locally.

The strengthened checker also reaudits the complete successful Stage 8K artifact
from run `37557979992`: all **16 recorded games**, **8/8 learned interventions**,
and **2 targeted interventions** pass. There are zero failed dispatches,
lifecycle anomalies, invalid requests or pre-intervention changes. Every field
of the original result is reproduced exactly, with the addition of explicit
zero failed-dispatch counts. This is verification of already observed data.

The dedicated audit-integrity workflow downloads both the original evidence and
frozen model by pinned artifact digest, repeats the check, and verifies that
source files remain unchanged. Its CI result is recorded after completion.

This change is confined to auditing and regression coverage. It changes no
Forge code, request-generation rule, model weight, seed family, deck, gameplay
rule or predeclared success threshold. It supplies evidence for the existing
zero-failure requirements; it does not promote learned control.
