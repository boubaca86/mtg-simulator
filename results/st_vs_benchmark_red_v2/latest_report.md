# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 172,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.67% | +5.84 pp | 93.80% | 64.98% |
| Eradicator S.T | 93.12% | 88.69% | +4.43 pp | 95.03% | 57.85% |
| Drop Pod S.T | 92.99% | 88.84% | +4.15 pp | 93.24% | 57.49% |
| Pyroclast Squad S.T | 92.82% | 89.08% | +3.74 pp | 93.61% | 64.40% |
| Terminator S.T | 92.82% | 89.10% | +3.71 pp | 93.30% | 65.49% |
| Exterminatus S.T | 88.96% | 92.28% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.35% | -2.09 pp | 92.68% | 4.21% |
| Infernus S.T | 92.00% | 90.72% | +1.28 pp | 93.13% | 61.49% |
| Whirlwind S.T | 91.16% | 92.23% | -1.07 pp | 93.49% | 47.79% |
| Servo-Skull | 91.17% | 91.67% | -0.50 pp | 91.22% | 22.02% |
| Demolitionist S.T | 91.63% | 91.42% | +0.20 pp | 93.27% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
