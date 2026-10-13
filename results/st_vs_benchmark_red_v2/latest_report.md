# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 102,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.53% | +6.04 pp | 93.83% | 64.83% |
| Eradicator S.T | 93.16% | 88.58% | +4.57 pp | 95.07% | 57.92% |
| Drop Pod S.T | 93.01% | 88.74% | +4.26 pp | 93.31% | 57.49% |
| Pyroclast Squad S.T | 92.86% | 88.95% | +3.91 pp | 93.67% | 64.32% |
| Terminator S.T | 92.81% | 89.05% | +3.77 pp | 93.30% | 65.51% |
| Exterminatus S.T | 89.08% | 92.22% | -3.15 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.32% | -2.07 pp | 92.63% | 4.24% |
| Whirlwind S.T | 91.12% | 92.26% | -1.14 pp | 93.36% | 47.89% |
| Infernus S.T | 91.93% | 90.80% | +1.13 pp | 93.08% | 61.52% |
| Servo-Skull | 91.17% | 91.65% | -0.48 pp | 91.23% | 21.99% |
| Demolitionist S.T | 91.59% | 91.44% | +0.15 pp | 93.21% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
