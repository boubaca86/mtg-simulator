# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 60,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.67% | +5.74 pp | 93.66% | 64.90% |
| Eradicator S.T | 93.15% | 88.39% | +4.76 pp | 95.05% | 58.08% |
| Drop Pod S.T | 92.96% | 88.64% | +4.32 pp | 93.28% | 57.57% |
| Pyroclast Squad S.T | 92.81% | 88.90% | +3.91 pp | 93.59% | 64.19% |
| Terminator S.T | 92.67% | 89.14% | +3.53 pp | 93.16% | 65.52% |
| Exterminatus S.T | 89.14% | 92.13% | -2.98 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.25% | -2.03 pp | 92.97% | 4.18% |
| Infernus S.T | 91.81% | 90.85% | +0.96 pp | 92.96% | 61.34% |
| Whirlwind S.T | 91.14% | 92.07% | -0.93 pp | 93.35% | 47.86% |
| Servo-Skull | 91.21% | 91.55% | -0.34 pp | 91.30% | 21.94% |
| Demolitionist S.T | 91.59% | 91.27% | +0.32 pp | 93.25% | 47.35% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
