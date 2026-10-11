# Stage 9H — Real Forge / two isolated actor processes

**Verified:** GitHub Actions run
https://github.com/boubaca86/mtg-simulator/actions/runs/38133487055
at feature commit `f05805a8d4321270ca875737fb8cd70a2fedf0ce`.
Pinned Card-Forge/forge `forge-2.0.15` peeled commit
`4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.

Maven **BUILD SUCCESS**; **3 TestNG methods executed, 0 failed, 0 errors,
0 skipped**; strict independent Surefire XML gate PASS; source hashes, logs and
reports archived as artifact `stage9h-forge-two-actors-38133487055-1`,
ID `11699877349`.

## What actually executed

* Authentic Forge `Game` with two `Player` objects, including their
  separate hand, library and battlefield zones.
* Two distinct persistent `python3 -I -S -u` worker processes with different
  PIDs, independently receiving strictly allowlisted serialized observations.
  Each reads objective, own hand, own decklist **multiset** (not draw order),
  only visible opponent battlefield cards, and referee-owned pair of life
  totals. Referee sends no turn, phase, priority, full public-zone snapshot,
  opponents' hidden cards, library order, internal card IDs or global catalog.
* Real `LifeLoseEffect.resolve` and `LifeGainEffect.resolve` moved Forge
  player life 20 -> 17 and 20 -> 24. Both separate workers confirmed their
  perspective on the updated values.
* Adding secret opponent cards to the actual Forge hand and library did
  not alter the observing actor's serialized observation; concealed
  face-down battlefield name and internal ID also omitted.
* The test-only referee rejects an actor attempt to add direct life-mutation
  commands to a response. Supported actor intent is **pass** only.

## NOT proven / not yet implemented

This is a real **Forge-to-separate-process bridge**, not a complete game.
Workers are deliberately nonstrategic, `pass` is not yet connected to
Forge's real priority loop, effects were explicitly resolved by the trusted
test referee, and the test does not cover full timing/stack/cost/target
enforcement. Both child processes still run on a shared OS and are not
hardened memory/filesystem/network sandboxes. No inference about invisible
opponent cards, other public zones, extra player state or rule permission
is granted by these tests.

Stage 9I independently works toward referee-validated land actions.
The feature branch remains unmerged. No training, reserved evaluation seeds,
production patches or deployments were used.
