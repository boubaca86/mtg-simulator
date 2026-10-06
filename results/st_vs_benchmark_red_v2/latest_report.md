# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 18,500
**S.T win rate:** 91.69%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.79% | 87.49% | +6.30 pp | 94.03% | 65.18% |
| Eradicator S.T | 93.41% | 88.53% | +4.88 pp | 95.07% | 58.30% |
| Drop Pod S.T | 93.04% | 89.10% | +3.93 pp | 93.32% | 57.73% |
| Pyroclast Squad S.T | 92.91% | 89.34% | +3.58 pp | 93.68% | 63.83% |
| Terminator S.T | 92.70% | 89.73% | +2.98 pp | 93.11% | 65.32% |
| Exterminatus S.T | 89.53% | 92.27% | -2.74 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.42% | 92.45% | -2.02 pp | 92.79% | 4.12% |
| Infernus S.T | 92.03% | 91.03% | +1.00 pp | 93.23% | 61.44% |
| Servo-Skull | 90.95% | 91.91% | -0.95 pp | 91.06% | 22.18% |
| Whirlwind S.T | 91.37% | 92.23% | -0.85 pp | 93.49% | 48.10% |
| Demolitionist S.T | 91.93% | 91.25% | +0.68 pp | 93.62% | 47.65% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
