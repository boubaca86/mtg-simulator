# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 18,000
**S.T win rate:** 91.74%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.82% | 87.57% | +6.24 pp | 94.06% | 65.31% |
| Eradicator S.T | 93.44% | 88.62% | +4.82 pp | 95.12% | 58.31% |
| Drop Pod S.T | 93.04% | 89.25% | +3.79 pp | 93.36% | 57.66% |
| Pyroclast Squad S.T | 92.91% | 89.49% | +3.42 pp | 93.70% | 63.76% |
| Terminator S.T | 92.74% | 89.81% | +2.93 pp | 93.14% | 65.35% |
| Exterminatus S.T | 89.68% | 92.30% | -2.63 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.44% | 92.52% | -2.07 pp | 92.92% | 4.16% |
| Servo-Skull | 90.94% | 91.98% | -1.04 pp | 91.04% | 22.19% |
| Infernus S.T | 92.09% | 91.08% | +1.00 pp | 93.30% | 61.43% |
| Whirlwind S.T | 91.39% | 92.34% | -0.95 pp | 93.54% | 48.16% |
| Demolitionist S.T | 91.97% | 91.32% | +0.65 pp | 93.69% | 47.61% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
