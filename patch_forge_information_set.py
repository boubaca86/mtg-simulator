from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "import forge.game.spellability.TargetChoices;\nimport forge.util.collect.FCollectionView;",
        "import forge.game.spellability.TargetChoices;\nimport forge.game.zone.PlayerZone;\nimport forge.game.zone.ZoneType;\nimport forge.util.collect.FCollectionView;",
        "zone imports",
    )

    text = replace_once(
        text,
        """        aiPlayer = copier.find(origAiPlayer);
        eval = new GameStateEvaluator();
""",
        """        aiPlayer = copier.find(origAiPlayer);

        // Expert-AI information boundary: do not let search evaluate the opponent's
        // actual hidden hand or actual future library order.  The copied game is
        // determinized from information available to the acting player.  Known /
        // revealed hidden cards are kept in their known zones; unknown cards are
        // resampled between the opponent's hand and library while preserving zone
        // counts.  A local RNG derived only from public/count information is used so
        // search does not consume the real game's RNG stream.
        determinizeOpponentHiddenInformation();

        eval = new GameStateEvaluator();
""",
        "GameSimulator constructor hook",
    )

    marker = """    private void ensureGameCopyScoreMatches(Game origGame, Player origAiPlayer) {
"""

    method = """    private long informationSetSeed() {
        long seed = 0x6A09E667F3BCC909L;
        seed = seed * 31 + simGame.getPhaseHandler().getTurn();
        seed = seed * 31 + simGame.getPhaseHandler().getPhase().ordinal();
        seed = seed * 31 + aiPlayer.getId();
        for (Player p : simGame.getPlayers()) {
            // Use public quantities only. Never hash hidden card identities or library order.
            seed = seed * 31 + p.getId();
            seed = seed * 31 + p.getLife();
            seed = seed * 31 + p.getCardsIn(ZoneType.Hand).size();
            seed = seed * 31 + p.getCardsIn(ZoneType.Library).size();
            seed = seed * 31 + p.getCardsIn(ZoneType.Battlefield).size();
            seed = seed * 31 + p.getCardsIn(ZoneType.Graveyard).size();
        }
        return seed;
    }

    private void determinizeOpponentHiddenInformation() {
        Random rng = new Random(informationSetSeed());

        for (Player opponent : aiPlayer.getOpponents()) {
            PlayerZone hand = opponent.getZone(ZoneType.Hand);
            PlayerZone library = opponent.getZone(ZoneType.Library);

            List<Card> originalHand = new ArrayList<>(hand.getCards());
            List<Card> originalLibrary = new ArrayList<>(library.getCards());

            // Cards explicitly visible to the acting player are knowledge and must remain known.
            List<Card> knownHand = new ArrayList<>();
            List<Card> unknownPool = new ArrayList<>();
            for (Card c : originalHand) {
                if (c.mayPlayerLook(aiPlayer)) {
                    knownHand.add(c);
                } else {
                    unknownPool.add(c);
                }
            }
            for (Card c : originalLibrary) {
                if (!c.mayPlayerLook(aiPlayer)) {
                    unknownPool.add(c);
                }
            }

            int unknownHandCount = originalHand.size() - knownHand.size();
            Collections.shuffle(unknownPool, rng);

            List<Card> newHand = new ArrayList<>(knownHand);
            newHand.addAll(unknownPool.subList(0, unknownHandCount));

            Iterator<Card> sampledLibrary = unknownPool.subList(unknownHandCount, unknownPool.size()).iterator();
            List<Card> newLibrary = new ArrayList<>(originalLibrary.size());
            for (Card c : originalLibrary) {
                if (c.mayPlayerLook(aiPlayer)) {
                    // Preserve cards whose identity/location is currently known.
                    newLibrary.add(c);
                } else if (sampledLibrary.hasNext()) {
                    newLibrary.add(sampledLibrary.next());
                }
            }

            hand.setCards(newHand);
            library.setCards(newLibrary);
        }
    }

"""

    text = replace_once(text, marker, method + marker, "information-set method insertion")

    path.write_text(text, encoding="utf-8")
    print(f"Patched hidden-information determinization into {path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=Path)
    args = parser.parse_args()
    patch(args.path)
