# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 294,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.64% | +5.79 pp | 93.72% | 64.95% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.98% | 57.76% |
| Drop Pod S.T | 92.92% | 88.78% | +4.14 pp | 93.16% | 57.40% |
| Terminator S.T | 92.78% | 88.96% | +3.82 pp | 93.28% | 65.48% |
| Pyroclast Squad S.T | 92.76% | 89.01% | +3.75 pp | 93.55% | 64.29% |
| Exterminatus S.T | 88.88% | 92.21% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.31% | -2.16 pp | 92.42% | 4.24% |
| Infernus S.T | 91.91% | 90.70% | +1.21 pp | 93.04% | 61.35% |
| Whirlwind S.T | 91.13% | 92.11% | -0.98 pp | 93.50% | 47.78% |
| Servo-Skull | 91.10% | 91.60% | -0.50 pp | 91.11% | 22.07% |
| Demolitionist S.T | 91.57% | 91.33% | +0.25 pp | 93.25% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
