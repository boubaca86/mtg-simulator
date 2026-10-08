# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 49,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.76% | +5.67 pp | 93.70% | 65.02% |
| Eradicator S.T | 93.23% | 88.41% | +4.83 pp | 95.08% | 57.99% |
| Drop Pod S.T | 93.06% | 88.59% | +4.48 pp | 93.38% | 57.50% |
| Pyroclast Squad S.T | 92.84% | 88.97% | +3.87 pp | 93.64% | 64.27% |
| Terminator S.T | 92.71% | 89.22% | +3.49 pp | 93.22% | 65.59% |
| Exterminatus S.T | 89.14% | 92.18% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.33% | -2.14 pp | 92.72% | 4.13% |
| Infernus S.T | 91.84% | 90.93% | +0.91 pp | 93.00% | 61.48% |
| Whirlwind S.T | 91.26% | 91.99% | -0.74 pp | 93.51% | 47.78% |
| Servo-Skull | 91.11% | 91.65% | -0.53 pp | 91.19% | 21.79% |
| Demolitionist S.T | 91.69% | 91.23% | +0.45 pp | 93.41% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
