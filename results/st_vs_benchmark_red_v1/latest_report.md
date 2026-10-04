# S.T vs Benchmark Red — Balance Report

**Engine:** st_vs_benchmark_red_v1
**Cumulative games:** 10,000
**S.T wins:** 8,605 (86.05%)
**Benchmark Red wins:** 1,395 (13.95%)
**Draws:** 0
**Deck-level read:** strongly overtuned in this matchup.

## Play / draw split

- S.T on the play: 87.40% over 5,000 games
- S.T on the draw: 84.70% over 5,000 games

## Individual-card observational telemetry

These deltas are signals, not causal proof. Paired replacement testing is the next phase.

| Card | Drawn WR | Not drawn WR | Δ | Cast WR | Cast rate | Games drawn |
|---|---:|---:|---:|---:|---:|---:|
| Drop Pod S.T | 89.01% | 80.57% | +8.44 pp | 89.06% | 64.92% | 6,496 |
| Terminator S.T | 87.95% | 82.33% | +5.62 pp | 91.95% | 60.12% | 6,616 |
| Eradicator S.T | 87.82% | 82.89% | +4.93 pp | 95.58% | 51.36% | 6,418 |
| Pyroclast Squad S.T | 87.72% | 82.93% | +4.79 pp | 91.35% | 58.74% | 6,515 |
| Servo-Skull | 82.63% | 87.10% | -4.47 pp | 82.63% | 23.49% | 2,349 |
| Exterminatus S.T | 82.58% | 87.03% | -4.45 pp | 0.00% | 0.00% | 2,198 |
| Vindicator S.T | 83.97% | 87.36% | -3.40 pp | 93.99% | 4.16% | 3,867 |
| Whirlwind S.T | 86.27% | 85.67% | +0.61 pp | 91.38% | 46.61% | 6,295 |
| Servitor | 85.99% | 86.18% | -0.20 pp | 86.13% | 67.13% | 6,736 |
| Demolitionist S.T | 86.00% | 86.15% | -0.15 pp | 92.09% | 40.07% | 6,521 |
| Infernus S.T | 86.10% | 85.96% | +0.14 pp | 89.21% | 58.67% | 6,503 |

## Matchup limitations

- Benchmark Red uses only Mountains, so effects that destroy lands which can't produce {R} have no legal targets here.
- Benchmark Red has no artifacts, so Pyroclast's artifact-destruction trigger is inactive.
- V1 models the mechanics needed for this matchup, not every timing/priority/stack corner case in Magic.
- Do not judge a card's final fairness from one matchup alone.

## Next step

Add paired A/B replacement tests using the same random seeds to estimate each card's causal win-rate contribution.

Last cloud batch: 10,000 games at 2026-10-04T00:30:46.637278+00:00
