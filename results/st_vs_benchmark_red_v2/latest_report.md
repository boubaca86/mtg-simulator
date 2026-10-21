# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 201,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.65% | +5.83 pp | 93.77% | 64.92% |
| Eradicator S.T | 93.09% | 88.68% | +4.41 pp | 95.01% | 57.80% |
| Drop Pod S.T | 92.95% | 88.83% | +4.12 pp | 93.20% | 57.41% |
| Terminator S.T | 92.81% | 89.04% | +3.77 pp | 93.31% | 65.45% |
| Pyroclast Squad S.T | 92.78% | 89.08% | +3.70 pp | 93.59% | 64.34% |
| Exterminatus S.T | 88.85% | 92.27% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.35% | -2.18 pp | 92.61% | 4.22% |
| Infernus S.T | 91.95% | 90.73% | +1.22 pp | 93.08% | 61.43% |
| Whirlwind S.T | 91.17% | 92.16% | -0.99 pp | 93.53% | 47.86% |
| Servo-Skull | 91.11% | 91.65% | -0.53 pp | 91.15% | 22.01% |
| Demolitionist S.T | 91.62% | 91.35% | +0.27 pp | 93.23% | 47.13% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
