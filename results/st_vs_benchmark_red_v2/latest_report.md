# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 123,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.70% | +5.85 pp | 93.83% | 64.97% |
| Eradicator S.T | 93.18% | 88.69% | +4.48 pp | 95.07% | 57.86% |
| Drop Pod S.T | 93.03% | 88.86% | +4.17 pp | 93.32% | 57.48% |
| Pyroclast Squad S.T | 92.86% | 89.10% | +3.76 pp | 93.66% | 64.49% |
| Terminator S.T | 92.85% | 89.13% | +3.72 pp | 93.35% | 65.53% |
| Exterminatus S.T | 89.01% | 92.30% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.36% | -2.03 pp | 92.67% | 4.24% |
| Infernus S.T | 92.02% | 90.78% | +1.24 pp | 93.15% | 61.54% |
| Whirlwind S.T | 91.20% | 92.27% | -1.07 pp | 93.46% | 47.82% |
| Servo-Skull | 91.24% | 91.69% | -0.45 pp | 91.29% | 21.95% |
| Demolitionist S.T | 91.64% | 91.49% | +0.15 pp | 93.28% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
