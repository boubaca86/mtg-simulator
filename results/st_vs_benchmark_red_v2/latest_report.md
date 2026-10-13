# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 106,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.59% | +5.97 pp | 93.81% | 64.86% |
| Eradicator S.T | 93.16% | 88.60% | +4.55 pp | 95.07% | 57.92% |
| Drop Pod S.T | 93.01% | 88.77% | +4.24 pp | 93.31% | 57.49% |
| Pyroclast Squad S.T | 92.85% | 89.00% | +3.85 pp | 93.66% | 64.38% |
| Terminator S.T | 92.82% | 89.06% | +3.76 pp | 93.31% | 65.55% |
| Exterminatus S.T | 89.06% | 92.23% | -3.17 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.32% | -2.04 pp | 92.61% | 4.23% |
| Whirlwind S.T | 91.11% | 92.30% | -1.19 pp | 93.36% | 47.90% |
| Infernus S.T | 91.94% | 90.80% | +1.14 pp | 93.09% | 61.48% |
| Servo-Skull | 91.20% | 91.65% | -0.45 pp | 91.26% | 22.00% |
| Demolitionist S.T | 91.59% | 91.47% | +0.12 pp | 93.22% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
