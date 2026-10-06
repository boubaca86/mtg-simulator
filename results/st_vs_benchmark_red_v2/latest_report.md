# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 25,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.64% | 87.46% | +6.18 pp | 93.91% | 65.26% |
| Eradicator S.T | 93.24% | 88.55% | +4.69 pp | 95.01% | 58.32% |
| Drop Pod S.T | 93.03% | 88.83% | +4.21 pp | 93.37% | 57.52% |
| Pyroclast Squad S.T | 92.87% | 89.13% | +3.74 pp | 93.59% | 63.85% |
| Terminator S.T | 92.57% | 89.67% | +2.90 pp | 93.02% | 65.43% |
| Exterminatus S.T | 89.42% | 92.17% | -2.75 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.06% | 92.51% | -2.45 pp | 92.16% | 4.20% |
| Infernus S.T | 91.88% | 91.02% | +0.86 pp | 93.04% | 61.57% |
| Demolitionist S.T | 91.85% | 91.11% | +0.74 pp | 93.62% | 47.31% |
| Servo-Skull | 91.12% | 91.72% | -0.60 pp | 91.15% | 22.07% |
| Whirlwind S.T | 91.38% | 91.95% | -0.57 pp | 93.64% | 47.89% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
