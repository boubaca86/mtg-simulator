# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 50,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.77% | +5.65 pp | 93.70% | 65.00% |
| Eradicator S.T | 93.24% | 88.41% | +4.83 pp | 95.10% | 58.03% |
| Drop Pod S.T | 93.07% | 88.59% | +4.48 pp | 93.40% | 57.54% |
| Pyroclast Squad S.T | 92.85% | 88.97% | +3.89 pp | 93.65% | 64.29% |
| Terminator S.T | 92.72% | 89.22% | +3.50 pp | 93.22% | 65.61% |
| Exterminatus S.T | 89.15% | 92.19% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.34% | -2.14 pp | 92.78% | 4.14% |
| Infernus S.T | 91.83% | 90.97% | +0.86 pp | 93.00% | 61.41% |
| Whirlwind S.T | 91.26% | 92.00% | -0.74 pp | 93.49% | 47.81% |
| Servo-Skull | 91.13% | 91.65% | -0.52 pp | 91.23% | 21.80% |
| Demolitionist S.T | 91.69% | 91.25% | +0.44 pp | 93.41% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
