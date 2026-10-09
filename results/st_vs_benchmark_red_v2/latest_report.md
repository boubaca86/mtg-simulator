# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 53,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.72% | +5.70 pp | 93.68% | 64.92% |
| Eradicator S.T | 93.20% | 88.39% | +4.81 pp | 95.08% | 58.15% |
| Drop Pod S.T | 93.00% | 88.64% | +4.36 pp | 93.32% | 57.53% |
| Pyroclast Squad S.T | 92.86% | 88.89% | +3.97 pp | 93.65% | 64.26% |
| Terminator S.T | 92.69% | 89.20% | +3.49 pp | 93.20% | 65.44% |
| Exterminatus S.T | 89.20% | 92.14% | -2.94 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.31% | -2.12 pp | 92.91% | 4.15% |
| Infernus S.T | 91.78% | 90.99% | +0.79 pp | 92.95% | 61.41% |
| Whirlwind S.T | 91.25% | 91.94% | -0.69 pp | 93.48% | 47.85% |
| Servo-Skull | 91.13% | 91.62% | -0.49 pp | 91.22% | 21.81% |
| Demolitionist S.T | 91.66% | 91.22% | +0.44 pp | 93.37% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
