# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 107,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.59% | +5.95 pp | 93.81% | 64.88% |
| Eradicator S.T | 93.16% | 88.60% | +4.56 pp | 95.07% | 57.90% |
| Drop Pod S.T | 93.01% | 88.76% | +4.26 pp | 93.31% | 57.46% |
| Pyroclast Squad S.T | 92.84% | 89.01% | +3.83 pp | 93.65% | 64.37% |
| Terminator S.T | 92.82% | 89.05% | +3.77 pp | 93.31% | 65.54% |
| Exterminatus S.T | 89.06% | 92.23% | -3.17 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.31% | -2.01 pp | 92.65% | 4.24% |
| Whirlwind S.T | 91.11% | 92.29% | -1.18 pp | 93.36% | 47.90% |
| Infernus S.T | 91.94% | 90.79% | +1.15 pp | 93.10% | 61.48% |
| Servo-Skull | 91.20% | 91.64% | -0.44 pp | 91.26% | 22.00% |
| Demolitionist S.T | 91.57% | 91.48% | +0.09 pp | 93.23% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
