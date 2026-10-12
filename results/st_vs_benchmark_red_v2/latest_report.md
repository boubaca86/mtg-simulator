# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 98,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.56% | +6.00 pp | 93.82% | 64.83% |
| Eradicator S.T | 93.16% | 88.58% | +4.58 pp | 95.08% | 57.99% |
| Drop Pod S.T | 93.02% | 88.74% | +4.28 pp | 93.32% | 57.50% |
| Pyroclast Squad S.T | 92.85% | 88.99% | +3.86 pp | 93.66% | 64.29% |
| Terminator S.T | 92.80% | 89.08% | +3.72 pp | 93.30% | 65.52% |
| Exterminatus S.T | 89.05% | 92.23% | -3.18 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.35% | -2.12 pp | 92.52% | 4.24% |
| Whirlwind S.T | 91.13% | 92.26% | -1.13 pp | 93.38% | 47.92% |
| Infernus S.T | 91.92% | 90.82% | +1.10 pp | 93.08% | 61.42% |
| Servo-Skull | 91.19% | 91.64% | -0.45 pp | 91.25% | 21.97% |
| Demolitionist S.T | 91.61% | 91.41% | +0.20 pp | 93.24% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
