# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 190,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.69% | +5.79 pp | 93.77% | 64.92% |
| Eradicator S.T | 93.11% | 88.67% | +4.44 pp | 95.01% | 57.86% |
| Drop Pod S.T | 92.97% | 88.83% | +4.14 pp | 93.23% | 57.45% |
| Terminator S.T | 92.80% | 89.08% | +3.72 pp | 93.30% | 65.46% |
| Pyroclast Squad S.T | 92.80% | 89.08% | +3.71 pp | 93.59% | 64.36% |
| Exterminatus S.T | 88.87% | 92.28% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.37% | -2.18 pp | 92.58% | 4.21% |
| Infernus S.T | 91.96% | 90.75% | +1.22 pp | 93.10% | 61.44% |
| Whirlwind S.T | 91.18% | 92.17% | -0.99 pp | 93.52% | 47.86% |
| Servo-Skull | 91.10% | 91.67% | -0.57 pp | 91.12% | 22.02% |
| Demolitionist S.T | 91.63% | 91.36% | +0.27 pp | 93.24% | 47.18% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
