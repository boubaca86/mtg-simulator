# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 126,000
**S.T win rate:** 91.60%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.70% | +5.87 pp | 93.84% | 65.00% |
| Eradicator S.T | 93.18% | 88.71% | +4.47 pp | 95.09% | 57.81% |
| Drop Pod S.T | 93.02% | 88.90% | +4.12 pp | 93.30% | 57.50% |
| Pyroclast Squad S.T | 92.85% | 89.14% | +3.72 pp | 93.66% | 64.46% |
| Terminator S.T | 92.86% | 89.14% | +3.71 pp | 93.36% | 65.54% |
| Exterminatus S.T | 88.99% | 92.32% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.37% | -2.03 pp | 92.68% | 4.24% |
| Infernus S.T | 92.03% | 90.78% | +1.25 pp | 93.16% | 61.56% |
| Whirlwind S.T | 91.21% | 92.28% | -1.07 pp | 93.47% | 47.85% |
| Servo-Skull | 91.21% | 91.71% | -0.50 pp | 91.26% | 21.94% |
| Demolitionist S.T | 91.66% | 91.49% | +0.17 pp | 93.28% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
