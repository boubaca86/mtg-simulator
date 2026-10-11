# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 80,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.68% | +5.86 pp | 93.79% | 64.91% |
| Eradicator S.T | 93.16% | 88.67% | +4.48 pp | 95.09% | 58.05% |
| Drop Pod S.T | 93.07% | 88.73% | +4.34 pp | 93.37% | 57.60% |
| Pyroclast Squad S.T | 92.85% | 89.10% | +3.75 pp | 93.67% | 64.19% |
| Terminator S.T | 92.79% | 89.19% | +3.60 pp | 93.31% | 65.64% |
| Exterminatus S.T | 89.24% | 92.23% | -2.99 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.40% | -2.17 pp | 92.86% | 4.22% |
| Whirlwind S.T | 91.18% | 92.27% | -1.09 pp | 93.46% | 47.92% |
| Infernus S.T | 91.93% | 90.91% | +1.01 pp | 93.08% | 61.36% |
| Servo-Skull | 91.26% | 91.67% | -0.41 pp | 91.34% | 21.90% |
| Demolitionist S.T | 91.66% | 91.41% | +0.25 pp | 93.36% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
