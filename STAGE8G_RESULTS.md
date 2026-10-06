# Stage 8G — live shadow sidecar result

Status: **passed**.

Workflow run `37455264547` at commit
`bcb2bb4d015ecdf4274d2b459816b745d892f559` passed the one-way live-sidecar
integration gate.

Artifact `11408708260`:
SHA-256 `9127f23b193b2654e828d58d485de92f8f945a9bea8561c0d4b7746de2cc266f`.

Across eight observed-seed games:

- 178 Stage 8 captures were emitted by live Forge;
- 178/178 produced an immediate frozen-model shadow recommendation;
- zero live-sidecar rejections occurred;
- zero live/offline recommendation mismatches occurred;
- recommendation order exactly matched Forge capture order;
- all 120 returned expert actions still reached the controller;
- all 120/120 controller dispatches succeeded;
- the sidecar had no command/control channel back into Forge;
- Forge 2.0.15 remained the sole rules referee;
- `promotion_allowed=false`.

This proves the frozen public-only model can run alongside a real Forge match
stream without altering gameplay and without drifting from deterministic offline
inference. It is integration evidence, not fresh playing-strength or win-rate
evidence.

The next stage is terminal lifecycle tracking for accepted actions before any
bounded learned-policy control is considered.
