# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 107,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.58% | +5.96 pp | 93.80% | 64.88% |
| Eradicator S.T | 93.15% | 88.58% | +4.57 pp | 95.07% | 57.91% |
| Drop Pod S.T | 93.00% | 88.76% | +4.24 pp | 93.30% | 57.48% |
| Pyroclast Squad S.T | 92.83% | 89.00% | +3.83 pp | 93.65% | 64.37% |
| Terminator S.T | 92.81% | 89.04% | +3.77 pp | 93.31% | 65.54% |
| Exterminatus S.T | 89.06% | 92.22% | -3.17 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.30% | -2.01 pp | 92.65% | 4.24% |
| Whirlwind S.T | 91.10% | 92.29% | -1.19 pp | 93.35% | 47.89% |
| Infernus S.T | 91.93% | 90.79% | +1.14 pp | 93.08% | 61.49% |
| Servo-Skull | 91.18% | 91.64% | -0.46 pp | 91.24% | 21.99% |
| Demolitionist S.T | 91.57% | 91.47% | +0.10 pp | 93.22% | 47.30% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
