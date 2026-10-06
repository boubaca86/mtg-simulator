# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 24,500
**S.T win rate:** 91.62%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.71% | 87.45% | +6.26 pp | 93.97% | 65.26% |
| Eradicator S.T | 93.25% | 88.64% | +4.60 pp | 95.03% | 58.26% |
| Drop Pod S.T | 93.06% | 88.87% | +4.19 pp | 93.39% | 57.54% |
| Pyroclast Squad S.T | 92.96% | 89.07% | +3.90 pp | 93.68% | 63.79% |
| Terminator S.T | 92.57% | 89.78% | +2.80 pp | 93.01% | 65.48% |
| Exterminatus S.T | 89.49% | 92.20% | -2.70 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.53% | -2.40 pp | 92.06% | 4.16% |
| Infernus S.T | 91.91% | 91.08% | +0.82 pp | 93.04% | 61.61% |
| Demolitionist S.T | 91.88% | 91.17% | +0.71 pp | 93.57% | 47.35% |
| Whirlwind S.T | 91.38% | 92.04% | -0.66 pp | 93.61% | 47.99% |
| Servo-Skull | 91.17% | 91.75% | -0.59 pp | 91.21% | 22.11% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
