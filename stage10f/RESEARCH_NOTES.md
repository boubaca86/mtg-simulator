# Stage 10F research notes

Source review: Wizards of the Coast Magic Comprehensive Rules (2026-09-25), rules 400.2, 509, 510, and 702.111; pinned Forge 2.0.15 combat implementation; Cowling, Powley, and Whitehouse (2012) Information Set MCTS.

Scope: harden the JSON observation boundary without extending the actor's visible information. Duplicate JSON object keys and non-finite numeric literals must not be accepted. All game authority remains in Forge. This note is a design plan, not test evidence.
