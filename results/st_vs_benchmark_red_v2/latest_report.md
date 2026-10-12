# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 97,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.56% | +6.00 pp | 93.82% | 64.85% |
| Eradicator S.T | 93.15% | 88.59% | +4.56 pp | 95.08% | 57.99% |
| Drop Pod S.T | 93.03% | 88.72% | +4.31 pp | 93.32% | 57.51% |
| Pyroclast Squad S.T | 92.85% | 89.00% | +3.85 pp | 93.66% | 64.30% |
| Terminator S.T | 92.79% | 89.10% | +3.69 pp | 93.29% | 65.51% |
| Exterminatus S.T | 89.04% | 92.24% | -3.19 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.33% | -2.08 pp | 92.61% | 4.24% |
| Whirlwind S.T | 91.13% | 92.25% | -1.12 pp | 93.40% | 47.89% |
| Infernus S.T | 91.92% | 90.83% | +1.10 pp | 93.08% | 61.41% |
| Servo-Skull | 91.20% | 91.64% | -0.44 pp | 91.27% | 21.97% |
| Demolitionist S.T | 91.61% | 91.42% | +0.19 pp | 93.26% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
