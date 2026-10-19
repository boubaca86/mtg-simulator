# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 175,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.67% | +5.85 pp | 93.81% | 64.96% |
| Eradicator S.T | 93.13% | 88.68% | +4.45 pp | 95.04% | 57.87% |
| Drop Pod S.T | 92.99% | 88.84% | +4.15 pp | 93.24% | 57.49% |
| Pyroclast Squad S.T | 92.81% | 89.11% | +3.70 pp | 93.60% | 64.38% |
| Terminator S.T | 92.81% | 89.12% | +3.69 pp | 93.30% | 65.46% |
| Exterminatus S.T | 88.95% | 92.28% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.36% | -2.11 pp | 92.65% | 4.21% |
| Infernus S.T | 92.00% | 90.73% | +1.27 pp | 93.14% | 61.48% |
| Whirlwind S.T | 91.17% | 92.23% | -1.06 pp | 93.49% | 47.80% |
| Servo-Skull | 91.18% | 91.67% | -0.49 pp | 91.22% | 22.03% |
| Demolitionist S.T | 91.63% | 91.42% | +0.21 pp | 93.26% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
