# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 36,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.68% | +5.76 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.21% | 88.37% | +4.84 pp | 95.06% | 58.22% |
| Drop Pod S.T | 93.11% | 88.45% | +4.66 pp | 93.42% | 57.67% |
| Pyroclast Squad S.T | 92.87% | 88.87% | +4.00 pp | 93.66% | 64.19% |
| Terminator S.T | 92.55% | 89.49% | +3.06 pp | 93.04% | 65.56% |
| Exterminatus S.T | 89.13% | 92.16% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.28% | -2.03 pp | 92.73% | 4.15% |
| Infernus S.T | 91.87% | 90.83% | +1.04 pp | 93.08% | 61.36% |
| Whirlwind S.T | 91.27% | 91.93% | -0.66 pp | 93.62% | 47.66% |
| Demolitionist S.T | 91.70% | 91.18% | +0.52 pp | 93.52% | 47.17% |
| Servo-Skull | 91.11% | 91.63% | -0.51 pp | 91.19% | 21.98% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
