# Stage 8V provenance decision

A seed plus decision index is not a globally unique game identifier. Use a trusted single-game run manifest with GitHub run ID, attempt, job, game ordinal, orientation, matchup, seed, and SHA-256 log digest. Compare every Stage 8V event against its Stage 7 companion and reject missing, duplicated, or inconsistent records. This is an offline validation design, not authorization to run Forge games or evidence of complete legal-action enumeration.

Research: https://opentelemetry.io/docs/specs/otel/trace/api/ and https://docs.github.com/en/actions/reference/workflows-and-actions/variables
