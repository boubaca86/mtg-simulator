# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 99,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.53% | +6.03 pp | 93.82% | 64.82% |
| Eradicator S.T | 93.15% | 88.57% | +4.59 pp | 95.07% | 57.98% |
| Drop Pod S.T | 93.01% | 88.73% | +4.28 pp | 93.31% | 57.52% |
| Pyroclast Squad S.T | 92.85% | 88.97% | +3.88 pp | 93.66% | 64.30% |
| Terminator S.T | 92.81% | 89.05% | +3.75 pp | 93.30% | 65.53% |
| Exterminatus S.T | 89.05% | 92.22% | -3.17 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.32% | -2.09 pp | 92.56% | 4.24% |
| Whirlwind S.T | 91.12% | 92.25% | -1.13 pp | 93.36% | 47.91% |
| Infernus S.T | 91.91% | 90.81% | +1.10 pp | 93.07% | 61.48% |
| Servo-Skull | 91.17% | 91.64% | -0.47 pp | 91.23% | 21.99% |
| Demolitionist S.T | 91.59% | 91.42% | +0.17 pp | 93.23% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
