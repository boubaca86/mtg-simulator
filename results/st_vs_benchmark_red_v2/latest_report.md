# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 165,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.75% | +5.75 pp | 93.79% | 65.01% |
| Eradicator S.T | 93.15% | 88.71% | +4.44 pp | 95.05% | 57.84% |
| Drop Pod S.T | 93.02% | 88.85% | +4.17 pp | 93.27% | 57.49% |
| Terminator S.T | 92.85% | 89.10% | +3.76 pp | 93.34% | 65.50% |
| Pyroclast Squad S.T | 92.84% | 89.11% | +3.73 pp | 93.63% | 64.43% |
| Exterminatus S.T | 89.01% | 92.29% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.37% | -2.09 pp | 92.76% | 4.21% |
| Infernus S.T | 92.01% | 90.77% | +1.25 pp | 93.15% | 61.48% |
| Whirlwind S.T | 91.18% | 92.26% | -1.08 pp | 93.50% | 47.78% |
| Servo-Skull | 91.18% | 91.70% | -0.51 pp | 91.23% | 22.04% |
| Demolitionist S.T | 91.65% | 91.44% | +0.21 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
