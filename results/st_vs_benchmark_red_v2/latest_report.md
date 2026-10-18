# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 162,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.75% | +5.74 pp | 93.78% | 65.00% |
| Eradicator S.T | 93.14% | 88.71% | +4.43 pp | 95.04% | 57.83% |
| Drop Pod S.T | 93.01% | 88.85% | +4.16 pp | 93.27% | 57.49% |
| Terminator S.T | 92.85% | 89.08% | +3.77 pp | 93.34% | 65.51% |
| Pyroclast Squad S.T | 92.84% | 89.09% | +3.75 pp | 93.63% | 64.42% |
| Exterminatus S.T | 88.97% | 92.29% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.37% | -2.10 pp | 92.79% | 4.21% |
| Infernus S.T | 92.01% | 90.75% | +1.25 pp | 93.14% | 61.49% |
| Whirlwind S.T | 91.17% | 92.27% | -1.10 pp | 93.49% | 47.77% |
| Servo-Skull | 91.17% | 91.69% | -0.52 pp | 91.22% | 22.02% |
| Demolitionist S.T | 91.66% | 91.41% | +0.24 pp | 93.31% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
