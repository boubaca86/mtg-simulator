# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 40,000
**S.T win rate:** 91.46%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.38% | 87.65% | +5.72 pp | 93.68% | 64.99% |
| Eradicator S.T | 93.13% | 88.39% | +4.74 pp | 94.98% | 58.16% |
| Drop Pod S.T | 93.08% | 88.36% | +4.72 pp | 93.41% | 57.64% |
| Pyroclast Squad S.T | 92.87% | 88.73% | +4.14 pp | 93.67% | 64.24% |
| Exterminatus S.T | 89.02% | 92.13% | -3.11 pp | 0.00% | 0.00% |
| Terminator S.T | 92.51% | 89.42% | +3.08 pp | 93.01% | 65.48% |
| Vindicator S.T | 90.16% | 92.25% | -2.09 pp | 92.64% | 4.11% |
| Infernus S.T | 91.74% | 90.94% | +0.80 pp | 92.94% | 61.34% |
| Whirlwind S.T | 91.19% | 91.94% | -0.75 pp | 93.53% | 47.73% |
| Servo-Skull | 91.04% | 91.58% | -0.55 pp | 91.11% | 21.84% |
| Demolitionist S.T | 91.62% | 91.17% | +0.45 pp | 93.47% | 47.16% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
