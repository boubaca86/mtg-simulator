# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 61,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.39% | 87.72% | +5.67 pp | 93.65% | 64.91% |
| Eradicator S.T | 93.16% | 88.41% | +4.74 pp | 95.06% | 58.12% |
| Drop Pod S.T | 92.97% | 88.67% | +4.30 pp | 93.29% | 57.55% |
| Pyroclast Squad S.T | 92.82% | 88.90% | +3.93 pp | 93.61% | 64.20% |
| Terminator S.T | 92.69% | 89.14% | +3.55 pp | 93.19% | 65.48% |
| Exterminatus S.T | 89.14% | 92.14% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.26% | -2.05 pp | 92.96% | 4.18% |
| Infernus S.T | 91.83% | 90.85% | +0.98 pp | 92.98% | 61.35% |
| Whirlwind S.T | 91.15% | 92.08% | -0.93 pp | 93.37% | 47.86% |
| Servo-Skull | 91.21% | 91.57% | -0.36 pp | 91.29% | 21.93% |
| Demolitionist S.T | 91.59% | 91.30% | +0.29 pp | 93.25% | 47.34% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
