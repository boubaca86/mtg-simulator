# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 138,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.72% | +5.82 pp | 93.81% | 64.99% |
| Eradicator S.T | 93.13% | 88.75% | +4.38 pp | 95.05% | 57.89% |
| Drop Pod S.T | 93.03% | 88.85% | +4.18 pp | 93.28% | 57.43% |
| Terminator S.T | 92.86% | 89.10% | +3.75 pp | 93.36% | 65.52% |
| Pyroclast Squad S.T | 92.83% | 89.14% | +3.69 pp | 93.64% | 64.42% |
| Exterminatus S.T | 88.93% | 92.32% | -3.39 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.38% | -2.09 pp | 92.76% | 4.23% |
| Infernus S.T | 92.01% | 90.78% | +1.22 pp | 93.15% | 61.54% |
| Whirlwind S.T | 91.20% | 92.25% | -1.05 pp | 93.50% | 47.85% |
| Servo-Skull | 91.21% | 91.69% | -0.48 pp | 91.26% | 21.93% |
| Demolitionist S.T | 91.66% | 91.45% | +0.20 pp | 93.30% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
