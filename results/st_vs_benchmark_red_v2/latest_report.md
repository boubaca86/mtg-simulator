# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 200,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.64% | +5.83 pp | 93.76% | 64.92% |
| Eradicator S.T | 93.08% | 88.66% | +4.42 pp | 95.00% | 57.80% |
| Drop Pod S.T | 92.93% | 88.83% | +4.11 pp | 93.19% | 57.40% |
| Terminator S.T | 92.80% | 89.03% | +3.77 pp | 93.29% | 65.45% |
| Pyroclast Squad S.T | 92.77% | 89.06% | +3.71 pp | 93.58% | 64.34% |
| Exterminatus S.T | 88.84% | 92.25% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.34% | -2.18 pp | 92.57% | 4.22% |
| Infernus S.T | 91.94% | 90.72% | +1.22 pp | 93.07% | 61.42% |
| Whirlwind S.T | 91.16% | 92.14% | -0.98 pp | 93.52% | 47.85% |
| Servo-Skull | 91.10% | 91.64% | -0.54 pp | 91.13% | 22.00% |
| Demolitionist S.T | 91.61% | 91.33% | +0.28 pp | 93.22% | 47.13% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
