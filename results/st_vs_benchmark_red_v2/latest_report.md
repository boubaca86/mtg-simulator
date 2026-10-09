# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 59,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.69% | +5.72 pp | 93.66% | 64.87% |
| Eradicator S.T | 93.16% | 88.40% | +4.76 pp | 95.05% | 58.10% |
| Drop Pod S.T | 92.94% | 88.69% | +4.24 pp | 93.27% | 57.57% |
| Pyroclast Squad S.T | 92.84% | 88.85% | +3.98 pp | 93.61% | 64.21% |
| Terminator S.T | 92.66% | 89.18% | +3.48 pp | 93.15% | 65.55% |
| Exterminatus S.T | 89.15% | 92.13% | -2.98 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.27% | -2.08 pp | 93.03% | 4.20% |
| Whirlwind S.T | 91.12% | 92.09% | -0.97 pp | 93.36% | 47.85% |
| Infernus S.T | 91.80% | 90.88% | +0.92 pp | 92.94% | 61.34% |
| Servo-Skull | 91.21% | 91.56% | -0.35 pp | 91.30% | 21.93% |
| Demolitionist S.T | 91.60% | 91.27% | +0.33 pp | 93.26% | 47.35% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
