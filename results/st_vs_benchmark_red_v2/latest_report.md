# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 73,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.66% | +5.84 pp | 93.74% | 64.93% |
| Eradicator S.T | 93.17% | 88.56% | +4.61 pp | 95.07% | 58.10% |
| Drop Pod S.T | 93.03% | 88.69% | +4.34 pp | 93.32% | 57.63% |
| Pyroclast Squad S.T | 92.85% | 89.01% | +3.84 pp | 93.67% | 64.15% |
| Terminator S.T | 92.75% | 89.17% | +3.58 pp | 93.27% | 65.57% |
| Exterminatus S.T | 89.30% | 92.16% | -2.86 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.34% | -2.10 pp | 92.91% | 4.20% |
| Whirlwind S.T | 91.16% | 92.19% | -1.03 pp | 93.44% | 47.80% |
| Infernus S.T | 91.88% | 90.91% | +0.97 pp | 93.03% | 61.39% |
| Demolitionist S.T | 91.67% | 91.31% | +0.35 pp | 93.35% | 47.26% |
| Servo-Skull | 91.30% | 91.61% | -0.31 pp | 91.37% | 21.96% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
