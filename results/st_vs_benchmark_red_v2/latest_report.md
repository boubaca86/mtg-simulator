# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 99,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.55% | +6.01 pp | 93.82% | 64.83% |
| Eradicator S.T | 93.15% | 88.58% | +4.57 pp | 95.07% | 57.98% |
| Drop Pod S.T | 93.01% | 88.73% | +4.28 pp | 93.32% | 57.52% |
| Pyroclast Squad S.T | 92.86% | 88.96% | +3.90 pp | 93.67% | 64.30% |
| Terminator S.T | 92.81% | 89.06% | +3.75 pp | 93.31% | 65.53% |
| Exterminatus S.T | 89.07% | 92.22% | -3.16 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.34% | -2.11 pp | 92.55% | 4.24% |
| Whirlwind S.T | 91.12% | 92.26% | -1.14 pp | 93.37% | 47.90% |
| Infernus S.T | 91.92% | 90.82% | +1.10 pp | 93.08% | 61.46% |
| Servo-Skull | 91.16% | 91.65% | -0.48 pp | 91.22% | 21.99% |
| Demolitionist S.T | 91.60% | 91.42% | +0.18 pp | 93.23% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
