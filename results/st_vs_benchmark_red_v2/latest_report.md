# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 69,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.69% | +5.75 pp | 93.69% | 64.95% |
| Eradicator S.T | 93.18% | 88.46% | +4.72 pp | 95.08% | 58.15% |
| Drop Pod S.T | 93.01% | 88.67% | +4.35 pp | 93.32% | 57.58% |
| Pyroclast Squad S.T | 92.84% | 88.94% | +3.90 pp | 93.66% | 64.16% |
| Terminator S.T | 92.73% | 89.15% | +3.58 pp | 93.25% | 65.50% |
| Exterminatus S.T | 89.19% | 92.16% | -2.97 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.30% | -2.06 pp | 93.00% | 4.21% |
| Infernus S.T | 91.86% | 90.87% | +0.99 pp | 93.03% | 61.45% |
| Whirlwind S.T | 91.17% | 92.12% | -0.95 pp | 93.43% | 47.83% |
| Servo-Skull | 91.27% | 91.59% | -0.32 pp | 91.36% | 21.97% |
| Demolitionist S.T | 91.61% | 91.34% | +0.27 pp | 93.31% | 47.19% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
