# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 78,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.69% | +5.83 pp | 93.76% | 64.96% |
| Eradicator S.T | 93.16% | 88.63% | +4.53 pp | 95.08% | 58.05% |
| Drop Pod S.T | 93.06% | 88.69% | +4.37 pp | 93.37% | 57.62% |
| Pyroclast Squad S.T | 92.86% | 89.03% | +3.83 pp | 93.68% | 64.18% |
| Terminator S.T | 92.78% | 89.16% | +3.62 pp | 93.29% | 65.64% |
| Exterminatus S.T | 89.25% | 92.20% | -2.95 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.39% | -2.18 pp | 92.81% | 4.21% |
| Whirlwind S.T | 91.17% | 92.24% | -1.07 pp | 93.44% | 47.87% |
| Infernus S.T | 91.91% | 90.91% | +1.00 pp | 93.05% | 61.34% |
| Servo-Skull | 91.27% | 91.64% | -0.37 pp | 91.35% | 21.94% |
| Demolitionist S.T | 91.67% | 91.35% | +0.32 pp | 93.35% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
