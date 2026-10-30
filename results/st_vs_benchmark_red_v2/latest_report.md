# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 306,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.65% | +5.77 pp | 93.71% | 64.96% |
| Eradicator S.T | 93.03% | 88.68% | +4.35 pp | 94.99% | 57.75% |
| Drop Pod S.T | 92.92% | 88.78% | +4.14 pp | 93.18% | 57.41% |
| Terminator S.T | 92.78% | 88.97% | +3.81 pp | 93.28% | 65.47% |
| Pyroclast Squad S.T | 92.76% | 89.00% | +3.76 pp | 93.56% | 64.30% |
| Exterminatus S.T | 88.87% | 92.21% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.31% | -2.16 pp | 92.43% | 4.23% |
| Infernus S.T | 91.90% | 90.72% | +1.18 pp | 93.03% | 61.34% |
| Whirlwind S.T | 91.13% | 92.11% | -0.99 pp | 93.52% | 47.74% |
| Servo-Skull | 91.06% | 91.61% | -0.56 pp | 91.08% | 22.06% |
| Demolitionist S.T | 91.56% | 91.35% | +0.21 pp | 93.25% | 47.03% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
