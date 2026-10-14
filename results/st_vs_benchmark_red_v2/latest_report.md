# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 117,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.67% | +5.89 pp | 93.84% | 64.94% |
| Eradicator S.T | 93.18% | 88.69% | +4.49 pp | 95.07% | 57.83% |
| Drop Pod S.T | 93.04% | 88.83% | +4.20 pp | 93.34% | 57.52% |
| Pyroclast Squad S.T | 92.85% | 89.10% | +3.76 pp | 93.66% | 64.45% |
| Terminator S.T | 92.85% | 89.11% | +3.75 pp | 93.35% | 65.56% |
| Exterminatus S.T | 89.06% | 92.29% | -3.22 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.35% | -2.01 pp | 92.70% | 4.24% |
| Infernus S.T | 92.00% | 90.80% | +1.20 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.19% | 92.28% | -1.09 pp | 93.43% | 47.82% |
| Servo-Skull | 91.21% | 91.69% | -0.48 pp | 91.28% | 22.02% |
| Demolitionist S.T | 91.64% | 91.48% | +0.17 pp | 93.29% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
