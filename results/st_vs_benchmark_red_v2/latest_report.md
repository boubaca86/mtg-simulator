# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 118,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.68% | +5.88 pp | 93.84% | 64.96% |
| Eradicator S.T | 93.19% | 88.68% | +4.51 pp | 95.08% | 57.83% |
| Drop Pod S.T | 93.04% | 88.85% | +4.19 pp | 93.33% | 57.51% |
| Pyroclast Squad S.T | 92.85% | 89.11% | +3.74 pp | 93.65% | 64.48% |
| Terminator S.T | 92.85% | 89.13% | +3.72 pp | 93.34% | 65.58% |
| Exterminatus S.T | 89.04% | 92.30% | -3.26 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.35% | -2.01 pp | 92.68% | 4.25% |
| Infernus S.T | 92.01% | 90.80% | +1.22 pp | 93.15% | 61.52% |
| Whirlwind S.T | 91.20% | 92.27% | -1.07 pp | 93.44% | 47.81% |
| Servo-Skull | 91.22% | 91.70% | -0.48 pp | 91.28% | 21.99% |
| Demolitionist S.T | 91.64% | 91.49% | +0.16 pp | 93.29% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
