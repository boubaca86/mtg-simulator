# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 168,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.72% | +5.78 pp | 93.78% | 65.01% |
| Eradicator S.T | 93.13% | 88.70% | +4.43 pp | 95.03% | 57.85% |
| Drop Pod S.T | 92.99% | 88.85% | +4.14 pp | 93.25% | 57.48% |
| Terminator S.T | 92.84% | 89.08% | +3.77 pp | 93.33% | 65.49% |
| Pyroclast Squad S.T | 92.81% | 89.12% | +3.69 pp | 93.60% | 64.41% |
| Exterminatus S.T | 88.99% | 92.28% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.34% | -2.05 pp | 92.71% | 4.21% |
| Infernus S.T | 92.00% | 90.75% | +1.24 pp | 93.13% | 61.46% |
| Whirlwind S.T | 91.18% | 92.24% | -1.06 pp | 93.49% | 47.78% |
| Servo-Skull | 91.19% | 91.68% | -0.49 pp | 91.24% | 22.03% |
| Demolitionist S.T | 91.65% | 91.41% | +0.23 pp | 93.29% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
