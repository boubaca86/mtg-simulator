# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 114,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.63% | +5.91 pp | 93.83% | 64.91% |
| Eradicator S.T | 93.17% | 88.64% | +4.53 pp | 95.07% | 57.87% |
| Drop Pod S.T | 93.03% | 88.78% | +4.25 pp | 93.33% | 57.55% |
| Terminator S.T | 92.85% | 89.05% | +3.81 pp | 93.34% | 65.55% |
| Pyroclast Squad S.T | 92.85% | 89.05% | +3.80 pp | 93.65% | 64.43% |
| Exterminatus S.T | 89.02% | 92.27% | -3.24 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.33% | -2.02 pp | 92.71% | 4.25% |
| Infernus S.T | 91.98% | 90.78% | +1.20 pp | 93.12% | 61.52% |
| Whirlwind S.T | 91.15% | 92.27% | -1.12 pp | 93.40% | 47.82% |
| Servo-Skull | 91.21% | 91.67% | -0.46 pp | 91.27% | 22.01% |
| Demolitionist S.T | 91.61% | 91.48% | +0.13 pp | 93.26% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
