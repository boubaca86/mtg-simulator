from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if text.count(old) != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {text.count(old)}")
    return text.replace(old, new, 1)


def patch(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "import forge.LobbyPlayer;\nimport forge.ai.AiProfileUtil;",
        "import forge.LobbyPlayer;\nimport forge.ai.AIOption;\nimport forge.ai.AiProfileUtil;",
        "AIOption import",
    )

    profile_block = """        List<String> aiProfiles = params.get(\"a\");
        if (aiProfiles != null) {
            for (String profile : aiProfiles) {
                if (!AiProfileUtil.getProfilesDisplayList().contains(profile)) {
                    System.out.println(TextUtil.concatNoSpace(\"Unknown AI profile - \", profile,
                            \". Available profiles: \", String.join(\", \", AiProfileUtil.getProfilesDisplayList())));
                    return;
                }
            }
        }

"""

    expert_block = profile_block + """        // Optional search mode per player. This exposes Forge's existing simulation AI to
        // headless CLI games so we can compare heuristic, hybrid, and full-search play.
        // Values: default, hybrid, full. Missing values default to normal heuristic AI.
        List<String> simulationModes = params.get(\"x\");
        if (simulationModes != null) {
            for (String mode : simulationModes) {
                String normalized = mode.toLowerCase(Locale.ROOT);
                if (!normalized.equals(\"default\") && !normalized.equals(\"hybrid\") && !normalized.equals(\"full\")) {
                    System.out.println(TextUtil.concatNoSpace(\"Unknown AI simulation mode - \", mode,
                            \". Available modes: default, hybrid, full\"));
                    return;
                }
            }
        }

"""
    text = replace_once(text, profile_block, expert_block, "AI profile block")

    profile_line = """                String profile = aiProfiles != null && aiProfiles.size() >= i ? aiProfiles.get(i - 1) : \"\";
                String name = TextUtil.concatNoSpace(\"Ai(\", String.valueOf(i), \")-\", d.getName());
                sb.append(name);
                if (!profile.isEmpty()) {
                    sb.append(\" [\").append(profile).append(\"]\");
                }

"""

    mode_line = """                String profile = aiProfiles != null && aiProfiles.size() >= i ? aiProfiles.get(i - 1) : \"\";
                String simulationMode = simulationModes != null && simulationModes.size() >= i
                        ? simulationModes.get(i - 1).toLowerCase(Locale.ROOT) : \"default\";
                Set<AIOption> aiOptions = null;
                if (simulationMode.equals(\"hybrid\")) {
                    aiOptions = EnumSet.of(AIOption.USE_HYBRID_SIMULATION);
                } else if (simulationMode.equals(\"full\")) {
                    aiOptions = EnumSet.of(AIOption.USE_FULL_SIMULATION);
                }

                String name = TextUtil.concatNoSpace(\"Ai(\", String.valueOf(i), \")-\", d.getName());
                sb.append(name);
                if (!profile.isEmpty()) {
                    sb.append(\" [\").append(profile).append(\"]\");
                }
                if (!simulationMode.equals(\"default\")) {
                    sb.append(\" {\").append(simulationMode).append(\"-simulation}\");
                }

"""
    text = replace_once(text, profile_line, mode_line, "per-player profile setup")

    text = replace_once(
        text,
        "                rp.setPlayer(GamePlayerUtil.createAiPlayer(name, i - 1, profile));",
        """                if (aiOptions == null) {
                    rp.setPlayer(GamePlayerUtil.createAiPlayer(name, i - 1, profile));
                } else {
                    rp.setPlayer(GamePlayerUtil.createAiPlayer(name, i - 1, 0, aiOptions, profile));
                }""",
        "AI player creation",
    )

    text = replace_once(
        text,
        "System.out.println(\"Syntax: forge.exe sim -d <deck1[.dck]> ... <deckX[.dck]> -D [D] -n [N] -m [M] -t [T] -p [P] -f [F] -s [S] -a [A] -q\");",
        "System.out.println(\"Syntax: forge.exe sim -d <deck1[.dck]> ... <deckX[.dck]> -D [D] -n [N] -m [M] -t [T] -p [P] -f [F] -s [S] -a [A] -x [X] -q\");",
        "CLI syntax",
    )

    text = replace_once(
        text,
        "        System.out.println(\"\\tA - AI profile per player, in the same order as the decks (e.g. -a Default Experimental)\");\n",
        """        System.out.println("\\tA - AI profile per player, in the same order as the decks (e.g. -a Default Experimental)");
        System.out.println("\\tX - AI search mode per player: default, hybrid, or full (e.g. -x full full)");
""",
        "CLI help",
    )

    path.write_text(text, encoding="utf-8")
    print(f"Patched Forge CLI simulation search modes into {path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=Path)
    args = parser.parse_args()
    patch(args.path)
