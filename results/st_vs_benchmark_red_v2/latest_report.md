# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 56,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.71% | +5.71 pp | 93.69% | 64.89% |
| Eradicator S.T | 93.18% | 88.41% | +4.78 pp | 95.07% | 58.15% |
| Drop Pod S.T | 92.99% | 88.66% | +4.33 pp | 93.32% | 57.52% |
| Pyroclast Squad S.T | 92.85% | 88.89% | +3.97 pp | 93.63% | 64.26% |
| Terminator S.T | 92.68% | 89.21% | +3.47 pp | 93.18% | 65.44% |
| Exterminatus S.T | 89.13% | 92.16% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.29% | -2.08 pp | 93.00% | 4.17% |
| Whirlwind S.T | 91.20% | 92.02% | -0.82 pp | 93.46% | 47.82% |
| Infernus S.T | 91.76% | 91.01% | +0.75 pp | 92.92% | 61.38% |
| Servo-Skull | 91.13% | 91.61% | -0.48 pp | 91.22% | 21.88% |
| Demolitionist S.T | 91.61% | 91.30% | +0.31 pp | 93.30% | 47.37% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
