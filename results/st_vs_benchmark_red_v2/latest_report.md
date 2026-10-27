# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 277,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.64% | +5.80 pp | 93.73% | 64.96% |
| Eradicator S.T | 93.05% | 88.66% | +4.39 pp | 94.98% | 57.81% |
| Drop Pod S.T | 92.92% | 88.80% | +4.11 pp | 93.16% | 57.40% |
| Terminator S.T | 92.81% | 88.95% | +3.86 pp | 93.30% | 65.45% |
| Pyroclast Squad S.T | 92.74% | 89.06% | +3.68 pp | 93.55% | 64.27% |
| Exterminatus S.T | 88.93% | 92.20% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.19 pp | 92.35% | 4.24% |
| Infernus S.T | 91.92% | 90.70% | +1.22 pp | 93.04% | 61.34% |
| Whirlwind S.T | 91.12% | 92.14% | -1.02 pp | 93.50% | 47.81% |
| Servo-Skull | 91.08% | 91.62% | -0.53 pp | 91.10% | 22.03% |
| Demolitionist S.T | 91.58% | 91.33% | +0.25 pp | 93.24% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
