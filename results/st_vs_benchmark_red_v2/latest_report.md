# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 20,000
**S.T win rate:** 91.64%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.75% | 87.44% | +6.31 pp | 93.99% | 65.11% |
| Eradicator S.T | 93.37% | 88.45% | +4.92 pp | 95.03% | 58.44% |
| Drop Pod S.T | 93.04% | 88.95% | +4.08 pp | 93.28% | 57.70% |
| Pyroclast Squad S.T | 92.92% | 89.19% | +3.74 pp | 93.68% | 63.69% |
| Terminator S.T | 92.68% | 89.61% | +3.07 pp | 93.08% | 65.39% |
| Exterminatus S.T | 89.45% | 92.24% | -2.79 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.48% | -2.26 pp | 92.25% | 4.13% |
| Infernus S.T | 92.00% | 90.94% | +1.06 pp | 93.18% | 61.48% |
| Servo-Skull | 90.91% | 91.85% | -0.94 pp | 90.98% | 22.18% |
| Whirlwind S.T | 91.36% | 92.11% | -0.75 pp | 93.47% | 48.30% |
| Demolitionist S.T | 91.89% | 91.17% | +0.72 pp | 93.54% | 47.59% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
