# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 55,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.72% | +5.70 pp | 93.68% | 64.92% |
| Eradicator S.T | 93.17% | 88.43% | +4.74 pp | 95.06% | 58.13% |
| Drop Pod S.T | 92.98% | 88.68% | +4.30 pp | 93.30% | 57.55% |
| Pyroclast Squad S.T | 92.85% | 88.87% | +3.98 pp | 93.65% | 64.27% |
| Terminator S.T | 92.69% | 89.19% | +3.50 pp | 93.19% | 65.42% |
| Exterminatus S.T | 89.18% | 92.15% | -2.97 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.28% | -2.06 pp | 92.97% | 4.19% |
| Whirlwind S.T | 91.20% | 92.01% | -0.81 pp | 93.44% | 47.83% |
| Infernus S.T | 91.75% | 91.03% | +0.72 pp | 92.92% | 61.37% |
| Servo-Skull | 91.11% | 91.61% | -0.50 pp | 91.21% | 21.87% |
| Demolitionist S.T | 91.62% | 91.28% | +0.35 pp | 93.30% | 47.34% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
