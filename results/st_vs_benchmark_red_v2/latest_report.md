# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 60,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.68% | +5.74 pp | 93.67% | 64.88% |
| Eradicator S.T | 93.16% | 88.41% | +4.75 pp | 95.05% | 58.06% |
| Drop Pod S.T | 92.95% | 88.69% | +4.26 pp | 93.27% | 57.56% |
| Pyroclast Squad S.T | 92.83% | 88.87% | +3.96 pp | 93.61% | 64.20% |
| Terminator S.T | 92.68% | 89.15% | +3.53 pp | 93.17% | 65.55% |
| Exterminatus S.T | 89.16% | 92.13% | -2.97 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.26% | -2.05 pp | 92.97% | 4.20% |
| Infernus S.T | 91.81% | 90.88% | +0.93 pp | 92.96% | 61.34% |
| Whirlwind S.T | 91.15% | 92.07% | -0.92 pp | 93.37% | 47.85% |
| Servo-Skull | 91.21% | 91.57% | -0.36 pp | 91.30% | 21.93% |
| Demolitionist S.T | 91.60% | 91.27% | +0.33 pp | 93.26% | 47.36% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
