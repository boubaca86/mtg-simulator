# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 203,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.66% | +5.82 pp | 93.76% | 64.91% |
| Eradicator S.T | 93.08% | 88.67% | +4.41 pp | 95.00% | 57.82% |
| Drop Pod S.T | 92.95% | 88.82% | +4.14 pp | 93.20% | 57.42% |
| Terminator S.T | 92.81% | 89.03% | +3.78 pp | 93.31% | 65.45% |
| Pyroclast Squad S.T | 92.77% | 89.08% | +3.69 pp | 93.58% | 64.32% |
| Exterminatus S.T | 88.83% | 92.27% | -3.44 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.35% | -2.18 pp | 92.57% | 4.23% |
| Infernus S.T | 91.94% | 90.73% | +1.21 pp | 93.07% | 61.43% |
| Whirlwind S.T | 91.16% | 92.15% | -0.99 pp | 93.52% | 47.86% |
| Servo-Skull | 91.10% | 91.65% | -0.55 pp | 91.13% | 22.01% |
| Demolitionist S.T | 91.63% | 91.33% | +0.29 pp | 93.24% | 47.15% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
