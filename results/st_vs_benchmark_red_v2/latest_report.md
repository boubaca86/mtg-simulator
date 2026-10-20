# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 195,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.65% | +5.82 pp | 93.75% | 64.90% |
| Eradicator S.T | 93.08% | 88.64% | +4.44 pp | 95.01% | 57.84% |
| Drop Pod S.T | 92.94% | 88.81% | +4.13 pp | 93.20% | 57.42% |
| Pyroclast Squad S.T | 92.77% | 89.05% | +3.72 pp | 93.58% | 64.36% |
| Terminator S.T | 92.77% | 89.07% | +3.71 pp | 93.27% | 65.45% |
| Exterminatus S.T | 88.84% | 92.26% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.33% | -2.16 pp | 92.56% | 4.21% |
| Infernus S.T | 91.94% | 90.71% | +1.23 pp | 93.08% | 61.42% |
| Whirlwind S.T | 91.16% | 92.13% | -0.97 pp | 93.52% | 47.84% |
| Servo-Skull | 91.07% | 91.65% | -0.58 pp | 91.09% | 22.01% |
| Demolitionist S.T | 91.61% | 91.33% | +0.28 pp | 93.23% | 47.15% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
