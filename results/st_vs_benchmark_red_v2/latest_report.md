# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 171,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.67% | +5.84 pp | 93.80% | 65.01% |
| Eradicator S.T | 93.13% | 88.69% | +4.44 pp | 95.03% | 57.83% |
| Drop Pod S.T | 92.98% | 88.85% | +4.14 pp | 93.24% | 57.49% |
| Terminator S.T | 92.82% | 89.10% | +3.73 pp | 93.31% | 65.48% |
| Pyroclast Squad S.T | 92.82% | 89.09% | +3.72 pp | 93.61% | 64.40% |
| Exterminatus S.T | 88.97% | 92.27% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.34% | -2.06 pp | 92.68% | 4.21% |
| Infernus S.T | 92.00% | 90.72% | +1.28 pp | 93.14% | 61.48% |
| Whirlwind S.T | 91.17% | 92.23% | -1.06 pp | 93.49% | 47.78% |
| Servo-Skull | 91.16% | 91.67% | -0.51 pp | 91.21% | 22.03% |
| Demolitionist S.T | 91.63% | 91.42% | +0.21 pp | 93.27% | 47.20% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
