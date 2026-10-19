# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 171,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.66% | +5.85 pp | 93.80% | 64.99% |
| Eradicator S.T | 93.12% | 88.69% | +4.43 pp | 95.03% | 57.84% |
| Drop Pod S.T | 92.98% | 88.84% | +4.14 pp | 93.24% | 57.49% |
| Pyroclast Squad S.T | 92.82% | 89.08% | +3.74 pp | 93.61% | 64.40% |
| Terminator S.T | 92.82% | 89.10% | +3.72 pp | 93.31% | 65.48% |
| Exterminatus S.T | 88.97% | 92.27% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.34% | -2.06 pp | 92.66% | 4.21% |
| Infernus S.T | 92.00% | 90.72% | +1.28 pp | 93.13% | 61.48% |
| Whirlwind S.T | 91.16% | 92.23% | -1.07 pp | 93.48% | 47.78% |
| Servo-Skull | 91.16% | 91.67% | -0.50 pp | 91.21% | 22.02% |
| Demolitionist S.T | 91.63% | 91.42% | +0.21 pp | 93.27% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
