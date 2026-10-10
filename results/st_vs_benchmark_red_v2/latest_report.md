# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 67,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.69% | +5.75 pp | 93.69% | 64.89% |
| Eradicator S.T | 93.16% | 88.47% | +4.69 pp | 95.07% | 58.14% |
| Drop Pod S.T | 93.00% | 88.65% | +4.35 pp | 93.32% | 57.61% |
| Pyroclast Squad S.T | 92.81% | 88.98% | +3.84 pp | 93.62% | 64.17% |
| Terminator S.T | 92.70% | 89.18% | +3.52 pp | 93.22% | 65.47% |
| Exterminatus S.T | 89.13% | 92.17% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.30% | -2.08 pp | 92.94% | 4.21% |
| Infernus S.T | 91.85% | 90.86% | +0.99 pp | 93.02% | 61.41% |
| Whirlwind S.T | 91.17% | 92.10% | -0.93 pp | 93.43% | 47.84% |
| Servo-Skull | 91.30% | 91.57% | -0.27 pp | 91.38% | 21.97% |
| Demolitionist S.T | 91.58% | 91.37% | +0.22 pp | 93.30% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
