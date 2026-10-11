# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 77,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.67% | +5.84 pp | 93.76% | 64.95% |
| Eradicator S.T | 93.15% | 88.63% | +4.52 pp | 95.08% | 58.03% |
| Drop Pod S.T | 93.06% | 88.67% | +4.39 pp | 93.36% | 57.64% |
| Pyroclast Squad S.T | 92.85% | 89.05% | +3.80 pp | 93.67% | 64.17% |
| Terminator S.T | 92.78% | 89.14% | +3.64 pp | 93.30% | 65.65% |
| Exterminatus S.T | 89.25% | 92.19% | -2.95 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.38% | -2.17 pp | 92.83% | 4.21% |
| Whirlwind S.T | 91.17% | 92.22% | -1.06 pp | 93.44% | 47.86% |
| Infernus S.T | 91.90% | 90.90% | +1.00 pp | 93.05% | 61.35% |
| Servo-Skull | 91.26% | 91.64% | -0.38 pp | 91.33% | 21.97% |
| Demolitionist S.T | 91.66% | 91.36% | +0.30 pp | 93.33% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
