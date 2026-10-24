# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 236,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.61% | +5.83 pp | 93.73% | 64.95% |
| Eradicator S.T | 93.04% | 88.64% | +4.40 pp | 94.98% | 57.81% |
| Drop Pod S.T | 92.92% | 88.76% | +4.16 pp | 93.17% | 57.44% |
| Terminator S.T | 92.78% | 88.96% | +3.83 pp | 93.27% | 65.44% |
| Pyroclast Squad S.T | 92.74% | 89.04% | +3.70 pp | 93.54% | 64.29% |
| Exterminatus S.T | 88.79% | 92.23% | -3.43 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.30% | -2.16 pp | 92.48% | 4.24% |
| Infernus S.T | 91.92% | 90.67% | +1.25 pp | 93.04% | 61.41% |
| Whirlwind S.T | 91.11% | 92.13% | -1.01 pp | 93.48% | 47.84% |
| Servo-Skull | 91.04% | 91.61% | -0.57 pp | 91.07% | 22.05% |
| Demolitionist S.T | 91.58% | 91.31% | +0.26 pp | 93.23% | 47.09% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
