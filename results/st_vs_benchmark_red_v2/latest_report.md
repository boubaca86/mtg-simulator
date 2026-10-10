# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 65,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.74% | +5.67 pp | 93.66% | 64.90% |
| Eradicator S.T | 93.16% | 88.46% | +4.69 pp | 95.06% | 58.15% |
| Drop Pod S.T | 92.99% | 88.67% | +4.32 pp | 93.31% | 57.61% |
| Pyroclast Squad S.T | 92.81% | 88.98% | +3.84 pp | 93.61% | 64.18% |
| Terminator S.T | 92.69% | 89.21% | +3.48 pp | 93.19% | 65.45% |
| Exterminatus S.T | 89.13% | 92.17% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.31% | -2.12 pp | 93.11% | 4.20% |
| Infernus S.T | 91.85% | 90.86% | +0.98 pp | 93.00% | 61.38% |
| Whirlwind S.T | 91.18% | 92.06% | -0.88 pp | 93.44% | 47.85% |
| Servo-Skull | 91.29% | 91.57% | -0.28 pp | 91.35% | 22.00% |
| Demolitionist S.T | 91.60% | 91.34% | +0.26 pp | 93.30% | 47.30% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
