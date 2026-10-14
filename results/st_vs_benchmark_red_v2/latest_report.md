# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 113,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.63% | +5.91 pp | 93.82% | 64.91% |
| Eradicator S.T | 93.16% | 88.63% | +4.53 pp | 95.06% | 57.87% |
| Drop Pod S.T | 93.02% | 88.77% | +4.24 pp | 93.32% | 57.53% |
| Pyroclast Squad S.T | 92.85% | 89.02% | +3.83 pp | 93.66% | 64.43% |
| Terminator S.T | 92.85% | 89.04% | +3.81 pp | 93.34% | 65.54% |
| Exterminatus S.T | 89.01% | 92.26% | -3.25 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.32% | -2.03 pp | 92.71% | 4.25% |
| Infernus S.T | 91.97% | 90.78% | +1.19 pp | 93.11% | 61.52% |
| Whirlwind S.T | 91.14% | 92.27% | -1.13 pp | 93.39% | 47.81% |
| Servo-Skull | 91.19% | 91.66% | -0.47 pp | 91.26% | 22.00% |
| Demolitionist S.T | 91.59% | 91.48% | +0.11 pp | 93.25% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
