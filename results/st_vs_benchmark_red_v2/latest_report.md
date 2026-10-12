# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 87,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.59% | +5.95 pp | 93.80% | 64.82% |
| Eradicator S.T | 93.13% | 88.61% | +4.52 pp | 95.09% | 58.05% |
| Drop Pod S.T | 93.02% | 88.71% | +4.30 pp | 93.33% | 57.59% |
| Pyroclast Squad S.T | 92.83% | 89.01% | +3.82 pp | 93.65% | 64.28% |
| Terminator S.T | 92.75% | 89.16% | +3.60 pp | 93.27% | 65.56% |
| Exterminatus S.T | 89.10% | 92.21% | -3.11 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.34% | -2.13 pp | 92.75% | 4.24% |
| Whirlwind S.T | 91.14% | 92.21% | -1.07 pp | 93.44% | 47.90% |
| Infernus S.T | 91.90% | 90.86% | +1.04 pp | 93.07% | 61.34% |
| Servo-Skull | 91.26% | 91.62% | -0.35 pp | 91.34% | 21.96% |
| Demolitionist S.T | 91.61% | 91.40% | +0.21 pp | 93.31% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
