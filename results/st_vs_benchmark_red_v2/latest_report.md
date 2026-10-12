# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 95,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.58% | 87.62% | +5.96 pp | 93.84% | 64.81% |
| Eradicator S.T | 93.17% | 88.64% | +4.53 pp | 95.12% | 58.03% |
| Drop Pod S.T | 93.06% | 88.76% | +4.29 pp | 93.36% | 57.54% |
| Pyroclast Squad S.T | 92.88% | 89.03% | +3.85 pp | 93.69% | 64.32% |
| Terminator S.T | 92.81% | 89.17% | +3.64 pp | 93.31% | 65.53% |
| Exterminatus S.T | 89.07% | 92.27% | -3.20 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.36% | -2.08 pp | 92.77% | 4.24% |
| Whirlwind S.T | 91.17% | 92.27% | -1.10 pp | 93.43% | 47.91% |
| Infernus S.T | 91.95% | 90.87% | +1.07 pp | 93.10% | 61.41% |
| Servo-Skull | 91.25% | 91.67% | -0.42 pp | 91.32% | 21.97% |
| Demolitionist S.T | 91.65% | 91.44% | +0.21 pp | 93.30% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
