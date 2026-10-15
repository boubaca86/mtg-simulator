# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 124,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.70% | +5.85 pp | 93.83% | 64.97% |
| Eradicator S.T | 93.18% | 88.70% | +4.48 pp | 95.07% | 57.86% |
| Drop Pod S.T | 93.03% | 88.86% | +4.17 pp | 93.32% | 57.50% |
| Pyroclast Squad S.T | 92.86% | 89.10% | +3.76 pp | 93.66% | 64.49% |
| Terminator S.T | 92.85% | 89.14% | +3.72 pp | 93.35% | 65.54% |
| Exterminatus S.T | 89.02% | 92.31% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.36% | -2.02 pp | 92.69% | 4.24% |
| Infernus S.T | 92.03% | 90.78% | +1.24 pp | 93.16% | 61.54% |
| Whirlwind S.T | 91.20% | 92.28% | -1.08 pp | 93.46% | 47.83% |
| Servo-Skull | 91.24% | 91.70% | -0.46 pp | 91.29% | 21.95% |
| Demolitionist S.T | 91.65% | 91.49% | +0.16 pp | 93.28% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
