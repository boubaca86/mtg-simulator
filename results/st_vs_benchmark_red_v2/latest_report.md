# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 121,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.67% | +5.88 pp | 93.83% | 64.97% |
| Eradicator S.T | 93.18% | 88.68% | +4.51 pp | 95.08% | 57.81% |
| Drop Pod S.T | 93.02% | 88.85% | +4.17 pp | 93.32% | 57.48% |
| Pyroclast Squad S.T | 92.85% | 89.10% | +3.76 pp | 93.65% | 64.45% |
| Terminator S.T | 92.84% | 89.13% | +3.72 pp | 93.33% | 65.56% |
| Exterminatus S.T | 89.04% | 92.29% | -3.24 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.36% | -2.05 pp | 92.63% | 4.26% |
| Infernus S.T | 92.01% | 90.78% | +1.23 pp | 93.14% | 61.54% |
| Whirlwind S.T | 91.19% | 92.27% | -1.09 pp | 93.44% | 47.83% |
| Servo-Skull | 91.22% | 91.69% | -0.47 pp | 91.28% | 21.99% |
| Demolitionist S.T | 91.64% | 91.48% | +0.16 pp | 93.27% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
