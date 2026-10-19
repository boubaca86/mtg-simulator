# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 176,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.65% | +5.85 pp | 93.79% | 64.94% |
| Eradicator S.T | 93.11% | 88.65% | +4.46 pp | 95.03% | 57.86% |
| Drop Pod S.T | 92.97% | 88.81% | +4.16 pp | 93.23% | 57.49% |
| Terminator S.T | 92.79% | 89.09% | +3.70 pp | 93.29% | 65.46% |
| Pyroclast Squad S.T | 92.79% | 89.10% | +3.69 pp | 93.57% | 64.38% |
| Exterminatus S.T | 88.94% | 92.26% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.35% | -2.14 pp | 92.61% | 4.20% |
| Infernus S.T | 91.97% | 90.72% | +1.26 pp | 93.11% | 61.46% |
| Whirlwind S.T | 91.16% | 92.20% | -1.04 pp | 93.48% | 47.80% |
| Servo-Skull | 91.13% | 91.66% | -0.52 pp | 91.17% | 22.02% |
| Demolitionist S.T | 91.62% | 91.39% | +0.23 pp | 93.25% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
