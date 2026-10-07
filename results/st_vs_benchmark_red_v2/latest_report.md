# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 37,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.71% | +5.73 pp | 93.73% | 64.99% |
| Eradicator S.T | 93.20% | 88.43% | +4.77 pp | 95.05% | 58.25% |
| Drop Pod S.T | 93.14% | 88.42% | +4.72 pp | 93.45% | 57.64% |
| Pyroclast Squad S.T | 92.88% | 88.87% | +4.01 pp | 93.68% | 64.18% |
| Terminator S.T | 92.56% | 89.49% | +3.08 pp | 93.06% | 65.54% |
| Exterminatus S.T | 89.12% | 92.18% | -3.05 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.28% | -2.03 pp | 92.78% | 4.16% |
| Infernus S.T | 91.86% | 90.88% | +0.98 pp | 93.07% | 61.37% |
| Whirlwind S.T | 91.27% | 91.95% | -0.68 pp | 93.64% | 47.66% |
| Servo-Skull | 91.11% | 91.64% | -0.53 pp | 91.18% | 21.94% |
| Demolitionist S.T | 91.69% | 91.22% | +0.47 pp | 93.51% | 47.16% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
