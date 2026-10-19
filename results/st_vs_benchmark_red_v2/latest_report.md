# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 179,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.66% | +5.83 pp | 93.78% | 64.95% |
| Eradicator S.T | 93.11% | 88.65% | +4.46 pp | 95.02% | 57.84% |
| Drop Pod S.T | 92.97% | 88.82% | +4.15 pp | 93.23% | 57.48% |
| Terminator S.T | 92.80% | 89.08% | +3.71 pp | 93.29% | 65.46% |
| Pyroclast Squad S.T | 92.78% | 89.10% | +3.68 pp | 93.57% | 64.39% |
| Exterminatus S.T | 88.92% | 92.26% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.35% | -2.15 pp | 92.65% | 4.20% |
| Infernus S.T | 91.98% | 90.70% | +1.27 pp | 93.12% | 61.47% |
| Whirlwind S.T | 91.16% | 92.19% | -1.03 pp | 93.50% | 47.80% |
| Servo-Skull | 91.12% | 91.66% | -0.54 pp | 91.16% | 22.02% |
| Demolitionist S.T | 91.61% | 91.39% | +0.22 pp | 93.23% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
