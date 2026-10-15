# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 135,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.69% | +5.85 pp | 93.82% | 65.02% |
| Eradicator S.T | 93.14% | 88.74% | +4.40 pp | 95.07% | 57.86% |
| Drop Pod S.T | 93.02% | 88.86% | +4.16 pp | 93.28% | 57.43% |
| Terminator S.T | 92.85% | 89.10% | +3.75 pp | 93.35% | 65.55% |
| Pyroclast Squad S.T | 92.83% | 89.15% | +3.68 pp | 93.63% | 64.44% |
| Exterminatus S.T | 88.96% | 92.31% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.37% | -2.08 pp | 92.77% | 4.24% |
| Infernus S.T | 92.01% | 90.78% | +1.23 pp | 93.14% | 61.56% |
| Whirlwind S.T | 91.19% | 92.26% | -1.07 pp | 93.49% | 47.85% |
| Servo-Skull | 91.23% | 91.69% | -0.45 pp | 91.28% | 21.94% |
| Demolitionist S.T | 91.65% | 91.47% | +0.18 pp | 93.29% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
