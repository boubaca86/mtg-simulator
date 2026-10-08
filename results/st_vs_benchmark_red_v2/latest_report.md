# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 42,500
**S.T win rate:** 91.46%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.37% | 87.66% | +5.72 pp | 93.67% | 65.13% |
| Eradicator S.T | 93.15% | 88.37% | +4.78 pp | 95.01% | 58.05% |
| Drop Pod S.T | 93.04% | 88.46% | +4.58 pp | 93.36% | 57.57% |
| Pyroclast Squad S.T | 92.85% | 88.77% | +4.08 pp | 93.65% | 64.23% |
| Terminator S.T | 92.54% | 89.36% | +3.19 pp | 93.06% | 65.53% |
| Exterminatus S.T | 89.11% | 92.11% | -3.01 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.08% | 92.30% | -2.22 pp | 92.66% | 4.11% |
| Whirlwind S.T | 91.16% | 91.99% | -0.82 pp | 93.47% | 47.68% |
| Infernus S.T | 91.72% | 90.99% | +0.73 pp | 92.91% | 61.41% |
| Demolitionist S.T | 91.64% | 91.15% | +0.49 pp | 93.48% | 47.13% |
| Servo-Skull | 91.13% | 91.56% | -0.43 pp | 91.20% | 21.76% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
