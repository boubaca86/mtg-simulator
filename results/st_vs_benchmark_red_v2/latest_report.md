# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 38,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.66% | +5.74 pp | 93.69% | 64.98% |
| Eradicator S.T | 93.16% | 88.37% | +4.79 pp | 95.03% | 58.21% |
| Drop Pod S.T | 93.11% | 88.35% | +4.75 pp | 93.46% | 57.67% |
| Pyroclast Squad S.T | 92.84% | 88.83% | +4.01 pp | 93.66% | 64.14% |
| Terminator S.T | 92.54% | 89.41% | +3.13 pp | 93.06% | 65.46% |
| Exterminatus S.T | 89.09% | 92.13% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.25% | -2.06 pp | 92.56% | 4.14% |
| Infernus S.T | 91.80% | 90.88% | +0.92 pp | 93.01% | 61.42% |
| Whirlwind S.T | 91.22% | 91.93% | -0.71 pp | 93.60% | 47.62% |
| Servo-Skull | 91.09% | 91.59% | -0.51 pp | 91.16% | 21.92% |
| Demolitionist S.T | 91.66% | 91.16% | +0.50 pp | 93.54% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
