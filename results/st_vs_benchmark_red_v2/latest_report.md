# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 180,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.67% | +5.82 pp | 93.77% | 64.96% |
| Eradicator S.T | 93.12% | 88.65% | +4.47 pp | 95.03% | 57.84% |
| Drop Pod S.T | 92.97% | 88.82% | +4.15 pp | 93.23% | 57.48% |
| Terminator S.T | 92.81% | 89.08% | +3.73 pp | 93.30% | 65.45% |
| Pyroclast Squad S.T | 92.78% | 89.11% | +3.67 pp | 93.57% | 64.38% |
| Exterminatus S.T | 88.92% | 92.26% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.36% | -2.16 pp | 92.66% | 4.21% |
| Infernus S.T | 91.98% | 90.70% | +1.28 pp | 93.13% | 61.46% |
| Whirlwind S.T | 91.17% | 92.18% | -1.01 pp | 93.50% | 47.81% |
| Servo-Skull | 91.12% | 91.66% | -0.55 pp | 91.15% | 22.03% |
| Demolitionist S.T | 91.62% | 91.39% | +0.23 pp | 93.23% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
