# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 319,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.67% | +5.74 pp | 93.70% | 64.96% |
| Eradicator S.T | 93.03% | 88.67% | +4.36 pp | 94.99% | 57.75% |
| Drop Pod S.T | 92.92% | 88.77% | +4.15 pp | 93.18% | 57.42% |
| Terminator S.T | 92.77% | 88.99% | +3.78 pp | 93.26% | 65.49% |
| Pyroclast Squad S.T | 92.76% | 89.01% | +3.75 pp | 93.56% | 64.29% |
| Exterminatus S.T | 88.87% | 92.21% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.19 pp | 92.48% | 4.24% |
| Infernus S.T | 91.89% | 90.73% | +1.16 pp | 93.02% | 61.36% |
| Whirlwind S.T | 91.12% | 92.12% | -1.00 pp | 93.52% | 47.75% |
| Servo-Skull | 91.04% | 91.62% | -0.58 pp | 91.06% | 22.04% |
| Demolitionist S.T | 91.56% | 91.34% | +0.22 pp | 93.25% | 47.04% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
