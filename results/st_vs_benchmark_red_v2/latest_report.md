# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 196,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.64% | +5.83 pp | 93.76% | 64.91% |
| Eradicator S.T | 93.09% | 88.65% | +4.44 pp | 95.01% | 57.82% |
| Drop Pod S.T | 92.94% | 88.83% | +4.11 pp | 93.19% | 57.40% |
| Pyroclast Squad S.T | 92.78% | 89.06% | +3.72 pp | 93.59% | 64.35% |
| Terminator S.T | 92.78% | 89.06% | +3.72 pp | 93.28% | 65.45% |
| Exterminatus S.T | 88.84% | 92.26% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.34% | -2.17 pp | 92.57% | 4.21% |
| Infernus S.T | 91.94% | 90.72% | +1.23 pp | 93.09% | 61.41% |
| Whirlwind S.T | 91.16% | 92.14% | -0.98 pp | 93.52% | 47.85% |
| Servo-Skull | 91.08% | 91.65% | -0.57 pp | 91.11% | 22.01% |
| Demolitionist S.T | 91.62% | 91.33% | +0.29 pp | 93.23% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
