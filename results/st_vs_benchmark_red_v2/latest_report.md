# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 7,000
**S.T win rate:** 92.36%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.31% | 88.80% | +5.51 pp | 95.85% | 58.24% |
| Servitor | 94.14% | 88.79% | +5.35 pp | 94.33% | 65.29% |
| Exterminatus S.T | 89.29% | 93.20% | -3.92 pp | 0.00% | 0.00% |
| Pyroclast Squad S.T | 93.65% | 89.78% | +3.87 pp | 94.40% | 64.57% |
| Drop Pod S.T | 93.42% | 90.34% | +3.08 pp | 93.72% | 57.09% |
| Terminator S.T | 93.18% | 90.79% | +2.39 pp | 93.52% | 65.24% |
| Servo-Skull | 90.66% | 92.87% | -2.21 pp | 90.66% | 22.34% |
| Vindicator S.T | 91.51% | 92.86% | -1.35 pp | 93.29% | 4.26% |
| Infernus S.T | 92.58% | 91.94% | +0.64 pp | 93.55% | 61.39% |
| Whirlwind S.T | 92.17% | 92.68% | -0.52 pp | 93.79% | 48.09% |
| Demolitionist S.T | 92.48% | 92.15% | +0.33 pp | 94.08% | 47.33% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
