# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 80,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.69% | +5.86 pp | 93.79% | 64.89% |
| Eradicator S.T | 93.17% | 88.65% | +4.52 pp | 95.10% | 58.07% |
| Drop Pod S.T | 93.06% | 88.73% | +4.33 pp | 93.37% | 57.60% |
| Pyroclast Squad S.T | 92.84% | 89.11% | +3.73 pp | 93.66% | 64.20% |
| Terminator S.T | 92.78% | 89.20% | +3.58 pp | 93.30% | 65.63% |
| Exterminatus S.T | 89.23% | 92.23% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.40% | -2.18 pp | 92.76% | 4.22% |
| Whirlwind S.T | 91.17% | 92.27% | -1.10 pp | 93.46% | 47.94% |
| Infernus S.T | 91.93% | 90.91% | +1.02 pp | 93.08% | 61.36% |
| Servo-Skull | 91.28% | 91.66% | -0.38 pp | 91.36% | 21.88% |
| Demolitionist S.T | 91.66% | 91.41% | +0.25 pp | 93.36% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
