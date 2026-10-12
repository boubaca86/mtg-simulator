# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 91,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.61% | +5.94 pp | 93.81% | 64.80% |
| Eradicator S.T | 93.15% | 88.62% | +4.53 pp | 95.10% | 58.07% |
| Drop Pod S.T | 93.04% | 88.71% | +4.33 pp | 93.34% | 57.59% |
| Pyroclast Squad S.T | 92.87% | 88.98% | +3.89 pp | 93.69% | 64.27% |
| Terminator S.T | 92.77% | 89.17% | +3.60 pp | 93.27% | 65.56% |
| Exterminatus S.T | 89.08% | 92.24% | -3.16 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.35% | -2.12 pp | 92.81% | 4.23% |
| Whirlwind S.T | 91.14% | 92.26% | -1.12 pp | 93.41% | 47.91% |
| Infernus S.T | 91.92% | 90.86% | +1.06 pp | 93.08% | 61.37% |
| Servo-Skull | 91.25% | 91.64% | -0.39 pp | 91.32% | 21.95% |
| Demolitionist S.T | 91.63% | 91.41% | +0.22 pp | 93.32% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
