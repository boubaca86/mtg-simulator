# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 322,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.68% | +5.74 pp | 93.71% | 64.94% |
| Eradicator S.T | 93.03% | 88.69% | +4.34 pp | 94.99% | 57.75% |
| Drop Pod S.T | 92.92% | 88.78% | +4.14 pp | 93.18% | 57.41% |
| Terminator S.T | 92.77% | 89.00% | +3.77 pp | 93.27% | 65.49% |
| Pyroclast Squad S.T | 92.76% | 89.02% | +3.74 pp | 93.57% | 64.29% |
| Exterminatus S.T | 88.90% | 92.21% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.33% | -2.20 pp | 92.47% | 4.24% |
| Infernus S.T | 91.90% | 90.73% | +1.16 pp | 93.03% | 61.36% |
| Whirlwind S.T | 91.13% | 92.12% | -1.00 pp | 93.52% | 47.76% |
| Servo-Skull | 91.04% | 91.62% | -0.59 pp | 91.06% | 22.03% |
| Demolitionist S.T | 91.57% | 91.35% | +0.23 pp | 93.26% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
