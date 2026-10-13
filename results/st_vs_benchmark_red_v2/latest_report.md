# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 102,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.55% | +6.03 pp | 93.83% | 64.83% |
| Eradicator S.T | 93.16% | 88.59% | +4.57 pp | 95.07% | 57.93% |
| Drop Pod S.T | 93.02% | 88.74% | +4.28 pp | 93.33% | 57.50% |
| Pyroclast Squad S.T | 92.87% | 88.96% | +3.91 pp | 93.67% | 64.32% |
| Terminator S.T | 92.82% | 89.06% | +3.75 pp | 93.30% | 65.51% |
| Exterminatus S.T | 89.11% | 92.22% | -3.11 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.33% | -2.07 pp | 92.63% | 4.23% |
| Whirlwind S.T | 91.12% | 92.27% | -1.15 pp | 93.35% | 47.90% |
| Infernus S.T | 91.93% | 90.81% | +1.12 pp | 93.08% | 61.50% |
| Servo-Skull | 91.17% | 91.65% | -0.47 pp | 91.24% | 21.99% |
| Demolitionist S.T | 91.59% | 91.44% | +0.15 pp | 93.22% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
