# Stage 11X integration and native outcome checks

This branch combines the Elf damage validation from PR #31 with development
commit `1eb0997d4b7e471e5901846e743ac08ea4aa370f`. The latter preserves the
emergency Bear block when another visible opposing Llanowar Elf is present and
explicitly untaps the arranged blocker. The fixture still passes priority to
preserve its declared board; Forge initializes combat and applies actual damage.
Natural games do not receive either fixture intervention.

PR #32's first push passed all 148 methods, while its first PR-preview run failed
the inherited `nativeUncuratedTwentyLifeForgeVictoryAttackerRegisteredFirst`
assertion. That test incorrectly required lethal combat from every shuffled
game. The legacy opening policy can bottom Bears during London mulligans, and
library exhaustion can end the game before twenty combat damage. The old failure
did not record a loss reason, so that individual game's reason is not claimed.

The two tests are now named `nativeUncuratedTwentyLifeForgeOutcome...`. They
retain their original decks, workers, real shuffle, London mulligans, twenty
starting life, draw/cast/combat loop and inventory checks. They validate the
actual native winner and accept only:

- `LifeReachedZero`: the defender lost to at least twenty observed damage,
  across at least three combat turns, from a paid and resolved original Bear.
- `Milled`: the actual loser has an empty library and positive life, damage is
  below twenty, and Forge records a native empty-draw loss.

Every observed damage event still must be an even two through eight damage in
a distinct real turn. Both outcomes must agree with tracked life, physical card
inventory and the registered winner. Unfinished games, draws, concessions,
invented damage and other loss causes do not pass. Actual reasons are logged;
there are no retries, selected seeds or prepared natural hands.

Two separate arranged regressions move original library cards to their owner's
graveyard and then run Forge normally. An empty library alone is not a loss;
the actual native draw must cause `Milled`. Either registered role is tested,
with twenty life unchanged and all registered cards conserved. These cases also
prove the outcome validator rejects unfinished games and fabricated damage.
The existing arranged ten-hit, twenty-to-zero regression still requires lethal
combat and exercises the same outcome validator's combat branch.

The exact native suite now requires 142 named methods, with zero failures,
errors or skips. Thirteen Python evidence tests and the existing three-case
disabled-validator mutation gate remain required. Exact run results belong in
the PR description, after CI completes. Successful tests and independent review
remain necessary before merging. This integration does not run protected seeds,
train actors, claim competitive strength or implement Ultimatus scoring.
