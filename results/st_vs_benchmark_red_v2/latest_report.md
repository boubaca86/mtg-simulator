# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 105,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.57% | +5.99 pp | 93.82% | 64.85% |
| Eradicator S.T | 93.16% | 88.59% | +4.57 pp | 95.07% | 57.93% |
| Drop Pod S.T | 93.00% | 88.77% | +4.23 pp | 93.31% | 57.51% |
| Pyroclast Squad S.T | 92.85% | 89.00% | +3.84 pp | 93.66% | 64.34% |
| Terminator S.T | 92.81% | 89.08% | +3.73 pp | 93.30% | 65.51% |
| Exterminatus S.T | 89.08% | 92.23% | -3.15 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.31% | -2.03 pp | 92.64% | 4.23% |
| Whirlwind S.T | 91.11% | 92.29% | -1.17 pp | 93.37% | 47.90% |
| Infernus S.T | 91.94% | 90.80% | +1.14 pp | 93.09% | 61.51% |
| Servo-Skull | 91.19% | 91.65% | -0.46 pp | 91.26% | 21.98% |
| Demolitionist S.T | 91.60% | 91.43% | +0.17 pp | 93.24% | 47.33% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
