# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 216,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.67% | +5.78 pp | 93.74% | 64.91% |
| Eradicator S.T | 93.07% | 88.67% | +4.40 pp | 94.99% | 57.83% |
| Drop Pod S.T | 92.96% | 88.76% | +4.20 pp | 93.21% | 57.47% |
| Terminator S.T | 92.80% | 89.01% | +3.80 pp | 93.30% | 65.43% |
| Pyroclast Squad S.T | 92.76% | 89.07% | +3.69 pp | 93.58% | 64.30% |
| Exterminatus S.T | 88.81% | 92.26% | -3.45 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.33% | -2.16 pp | 92.59% | 4.24% |
| Infernus S.T | 91.95% | 90.69% | +1.25 pp | 93.08% | 61.42% |
| Whirlwind S.T | 91.16% | 92.13% | -0.98 pp | 93.52% | 47.86% |
| Servo-Skull | 91.08% | 91.64% | -0.56 pp | 91.11% | 22.02% |
| Demolitionist S.T | 91.61% | 91.33% | +0.29 pp | 93.26% | 47.17% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
