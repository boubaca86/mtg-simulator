# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 91,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.58% | +5.98 pp | 93.82% | 64.82% |
| Eradicator S.T | 93.14% | 88.62% | +4.53 pp | 95.11% | 58.06% |
| Drop Pod S.T | 93.04% | 88.70% | +4.33 pp | 93.34% | 57.60% |
| Pyroclast Squad S.T | 92.86% | 88.99% | +3.87 pp | 93.67% | 64.26% |
| Terminator S.T | 92.76% | 89.17% | +3.60 pp | 93.27% | 65.54% |
| Exterminatus S.T | 89.05% | 92.24% | -3.19 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.34% | -2.11 pp | 92.84% | 4.23% |
| Whirlwind S.T | 91.14% | 92.25% | -1.11 pp | 93.41% | 47.92% |
| Infernus S.T | 91.91% | 90.85% | +1.06 pp | 93.07% | 61.39% |
| Servo-Skull | 91.25% | 91.63% | -0.39 pp | 91.32% | 21.96% |
| Demolitionist S.T | 91.61% | 91.42% | +0.19 pp | 93.29% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
