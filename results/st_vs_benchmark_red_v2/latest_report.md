# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 43,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.39% | 87.67% | +5.72 pp | 93.69% | 65.13% |
| Eradicator S.T | 93.16% | 88.43% | +4.73 pp | 95.00% | 58.03% |
| Drop Pod S.T | 93.07% | 88.46% | +4.60 pp | 93.38% | 57.55% |
| Pyroclast Squad S.T | 92.87% | 88.79% | +4.09 pp | 93.67% | 64.25% |
| Terminator S.T | 92.58% | 89.34% | +3.24 pp | 93.10% | 65.51% |
| Exterminatus S.T | 89.10% | 92.14% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.11% | 92.31% | -2.20 pp | 92.76% | 4.11% |
| Whirlwind S.T | 91.19% | 92.00% | -0.81 pp | 93.48% | 47.68% |
| Infernus S.T | 91.76% | 90.97% | +0.78 pp | 92.94% | 61.40% |
| Demolitionist S.T | 91.66% | 91.17% | +0.48 pp | 93.48% | 47.13% |
| Servo-Skull | 91.16% | 91.58% | -0.42 pp | 91.24% | 21.72% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
