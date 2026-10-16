# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 137,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.70% | +5.84 pp | 93.81% | 65.01% |
| Eradicator S.T | 93.12% | 88.76% | +4.36 pp | 95.05% | 57.87% |
| Drop Pod S.T | 93.03% | 88.85% | +4.18 pp | 93.29% | 57.42% |
| Terminator S.T | 92.85% | 89.11% | +3.74 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.83% | 89.14% | +3.69 pp | 93.64% | 64.45% |
| Exterminatus S.T | 88.93% | 92.32% | -3.39 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.37% | -2.07 pp | 92.73% | 4.24% |
| Infernus S.T | 92.00% | 90.79% | +1.21 pp | 93.15% | 61.55% |
| Whirlwind S.T | 91.19% | 92.26% | -1.07 pp | 93.50% | 47.84% |
| Servo-Skull | 91.23% | 91.69% | -0.45 pp | 91.28% | 21.92% |
| Demolitionist S.T | 91.65% | 91.46% | +0.19 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
