# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 174,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.65% | +5.86 pp | 93.80% | 64.96% |
| Eradicator S.T | 93.12% | 88.67% | +4.45 pp | 95.03% | 57.87% |
| Drop Pod S.T | 92.98% | 88.83% | +4.16 pp | 93.23% | 57.49% |
| Pyroclast Squad S.T | 92.80% | 89.10% | +3.71 pp | 93.59% | 64.39% |
| Terminator S.T | 92.80% | 89.12% | +3.68 pp | 93.29% | 65.47% |
| Exterminatus S.T | 88.95% | 92.27% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.35% | -2.12 pp | 92.63% | 4.21% |
| Infernus S.T | 91.99% | 90.71% | +1.28 pp | 93.13% | 61.48% |
| Whirlwind S.T | 91.16% | 92.22% | -1.06 pp | 93.48% | 47.80% |
| Servo-Skull | 91.17% | 91.66% | -0.49 pp | 91.22% | 22.02% |
| Demolitionist S.T | 91.62% | 91.42% | +0.20 pp | 93.26% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
