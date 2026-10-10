# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 66,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.70% | +5.74 pp | 93.68% | 64.89% |
| Eradicator S.T | 93.16% | 88.45% | +4.71 pp | 95.07% | 58.17% |
| Drop Pod S.T | 93.01% | 88.65% | +4.35 pp | 93.32% | 57.60% |
| Pyroclast Squad S.T | 92.82% | 88.97% | +3.85 pp | 93.62% | 64.17% |
| Terminator S.T | 92.69% | 89.21% | +3.48 pp | 93.20% | 65.48% |
| Exterminatus S.T | 89.13% | 92.17% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.31% | -2.11 pp | 93.01% | 4.20% |
| Infernus S.T | 91.84% | 90.88% | +0.95 pp | 92.99% | 61.38% |
| Whirlwind S.T | 91.16% | 92.10% | -0.94 pp | 93.42% | 47.88% |
| Servo-Skull | 91.28% | 91.57% | -0.30 pp | 91.34% | 21.98% |
| Demolitionist S.T | 91.59% | 91.35% | +0.24 pp | 93.30% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
