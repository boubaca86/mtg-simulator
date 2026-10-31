# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 323,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.68% | +5.73 pp | 93.70% | 64.95% |
| Eradicator S.T | 93.03% | 88.69% | +4.35 pp | 94.99% | 57.76% |
| Drop Pod S.T | 92.92% | 88.78% | +4.14 pp | 93.18% | 57.41% |
| Terminator S.T | 92.77% | 89.01% | +3.76 pp | 93.27% | 65.49% |
| Pyroclast Squad S.T | 92.76% | 89.02% | +3.74 pp | 93.57% | 64.29% |
| Exterminatus S.T | 88.90% | 92.21% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.32% | -2.18 pp | 92.49% | 4.24% |
| Infernus S.T | 91.90% | 90.73% | +1.17 pp | 93.03% | 61.35% |
| Whirlwind S.T | 91.13% | 92.12% | -0.99 pp | 93.53% | 47.77% |
| Servo-Skull | 91.04% | 91.63% | -0.59 pp | 91.06% | 22.03% |
| Demolitionist S.T | 91.57% | 91.34% | +0.23 pp | 93.26% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
