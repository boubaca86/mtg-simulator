# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 23,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.64% | 87.41% | +6.22 pp | 93.92% | 65.20% |
| Eradicator S.T | 93.23% | 88.50% | +4.73 pp | 95.01% | 58.32% |
| Drop Pod S.T | 93.00% | 88.80% | +4.20 pp | 93.33% | 57.59% |
| Pyroclast Squad S.T | 92.94% | 88.94% | +4.00 pp | 93.69% | 63.60% |
| Terminator S.T | 92.50% | 89.73% | +2.77 pp | 92.93% | 65.47% |
| Exterminatus S.T | 89.44% | 92.13% | -2.70 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.11% | 92.42% | -2.31 pp | 92.11% | 4.13% |
| Infernus S.T | 91.91% | 90.89% | +1.02 pp | 93.10% | 61.54% |
| Whirlwind S.T | 91.30% | 92.01% | -0.71 pp | 93.51% | 48.13% |
| Demolitionist S.T | 91.81% | 91.11% | +0.70 pp | 93.48% | 47.42% |
| Servo-Skull | 91.07% | 91.70% | -0.64 pp | 91.14% | 22.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
