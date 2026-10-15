# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 127,500
**S.T win rate:** 91.60%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.69% | +5.88 pp | 93.84% | 64.99% |
| Eradicator S.T | 93.18% | 88.71% | +4.48 pp | 95.09% | 57.84% |
| Drop Pod S.T | 93.02% | 88.90% | +4.11 pp | 93.30% | 57.49% |
| Pyroclast Squad S.T | 92.85% | 89.15% | +3.70 pp | 93.65% | 64.46% |
| Terminator S.T | 92.85% | 89.15% | +3.70 pp | 93.35% | 65.52% |
| Exterminatus S.T | 89.01% | 92.32% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.37% | -2.03 pp | 92.71% | 4.23% |
| Infernus S.T | 92.04% | 90.77% | +1.27 pp | 93.17% | 61.61% |
| Whirlwind S.T | 91.21% | 92.27% | -1.06 pp | 93.46% | 47.87% |
| Servo-Skull | 91.25% | 91.70% | -0.45 pp | 91.30% | 21.94% |
| Demolitionist S.T | 91.65% | 91.49% | +0.16 pp | 93.28% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
