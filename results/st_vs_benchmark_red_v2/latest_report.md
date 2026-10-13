# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 104,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.56% | +6.00 pp | 93.82% | 64.85% |
| Eradicator S.T | 93.16% | 88.59% | +4.57 pp | 95.07% | 57.93% |
| Drop Pod S.T | 93.01% | 88.76% | +4.25 pp | 93.31% | 57.52% |
| Pyroclast Squad S.T | 92.85% | 88.98% | +3.87 pp | 93.66% | 64.33% |
| Terminator S.T | 92.82% | 89.05% | +3.77 pp | 93.31% | 65.54% |
| Exterminatus S.T | 89.08% | 92.22% | -3.14 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.32% | -2.04 pp | 92.65% | 4.23% |
| Whirlwind S.T | 91.11% | 92.28% | -1.17 pp | 93.36% | 47.89% |
| Infernus S.T | 91.94% | 90.79% | +1.15 pp | 93.09% | 61.52% |
| Servo-Skull | 91.20% | 91.64% | -0.44 pp | 91.27% | 21.98% |
| Demolitionist S.T | 91.60% | 91.44% | +0.16 pp | 93.22% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
