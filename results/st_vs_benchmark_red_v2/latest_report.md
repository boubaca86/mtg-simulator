# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 42,000
**S.T win rate:** 91.46%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.38% | 87.62% | +5.75 pp | 93.68% | 65.13% |
| Eradicator S.T | 93.14% | 88.36% | +4.78 pp | 94.99% | 58.08% |
| Drop Pod S.T | 93.04% | 88.43% | +4.61 pp | 93.36% | 57.58% |
| Pyroclast Squad S.T | 92.86% | 88.74% | +4.12 pp | 93.66% | 64.25% |
| Terminator S.T | 92.56% | 89.32% | +3.24 pp | 93.07% | 65.50% |
| Exterminatus S.T | 89.06% | 92.11% | -3.05 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.26% | -2.14 pp | 92.67% | 4.09% |
| Whirlwind S.T | 91.15% | 92.00% | -0.85 pp | 93.45% | 47.69% |
| Infernus S.T | 91.72% | 90.96% | +0.76 pp | 92.92% | 61.40% |
| Demolitionist S.T | 91.64% | 91.12% | +0.52 pp | 93.47% | 47.16% |
| Servo-Skull | 91.10% | 91.56% | -0.46 pp | 91.17% | 21.78% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
