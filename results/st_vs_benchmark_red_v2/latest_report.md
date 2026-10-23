# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 221,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.65% | +5.80 pp | 93.74% | 64.92% |
| Eradicator S.T | 93.07% | 88.65% | +4.41 pp | 94.99% | 57.81% |
| Drop Pod S.T | 92.95% | 88.76% | +4.20 pp | 93.20% | 57.47% |
| Terminator S.T | 92.79% | 89.01% | +3.78 pp | 93.29% | 65.42% |
| Pyroclast Squad S.T | 92.77% | 89.04% | +3.74 pp | 93.58% | 64.32% |
| Exterminatus S.T | 88.82% | 92.25% | -3.43 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.33% | -2.16 pp | 92.56% | 4.23% |
| Infernus S.T | 91.94% | 90.69% | +1.25 pp | 93.07% | 61.42% |
| Whirlwind S.T | 91.14% | 92.15% | -1.01 pp | 93.50% | 47.86% |
| Servo-Skull | 91.11% | 91.62% | -0.51 pp | 91.14% | 22.03% |
| Demolitionist S.T | 91.60% | 91.32% | +0.28 pp | 93.25% | 47.15% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
