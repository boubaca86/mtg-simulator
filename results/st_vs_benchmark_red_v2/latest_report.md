# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 66,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.68% | +5.75 pp | 93.68% | 64.88% |
| Eradicator S.T | 93.15% | 88.46% | +4.69 pp | 95.07% | 58.15% |
| Drop Pod S.T | 93.00% | 88.64% | +4.36 pp | 93.31% | 57.61% |
| Pyroclast Squad S.T | 92.80% | 88.98% | +3.82 pp | 93.61% | 64.17% |
| Terminator S.T | 92.68% | 89.20% | +3.49 pp | 93.20% | 65.47% |
| Exterminatus S.T | 89.12% | 92.16% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.30% | -2.09 pp | 92.98% | 4.20% |
| Infernus S.T | 91.84% | 90.87% | +0.97 pp | 93.01% | 61.38% |
| Whirlwind S.T | 91.16% | 92.10% | -0.94 pp | 93.42% | 47.87% |
| Servo-Skull | 91.30% | 91.56% | -0.26 pp | 91.37% | 21.97% |
| Demolitionist S.T | 91.58% | 91.35% | +0.24 pp | 93.30% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
