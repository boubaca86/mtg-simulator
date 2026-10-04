from __future__ import annotations

import csv
import json
import random
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple

GAMES_PER_RUN = 10_000
MAX_TURNS = 30

RESULTS_DIR = Path("results") / "st_vs_benchmark_red_v1"
SUMMARY_FILE = RESULTS_DIR / "summary.json"
HISTORY_FILE = RESULTS_DIR / "history.csv"
CARD_STATS_FILE = RESULTS_DIR / "card_stats.json"
REPORT_FILE = RESULTS_DIR / "latest_report.md"
RESULTS_DIR.mkdir(parents=True, exist_ok=True)


@dataclass(frozen=True)
class CardDef:
    name: str
    generic: int = 0
    red: int = 0
    card_type: str = "creature"
    power: int = 0
    toughness: int = 0
    haste: bool = False
    trample: bool = False
    menace: bool = False
    defender: bool = False
    ward: int = 0
    tags: Tuple[str, ...] = ()
    damage: int = 0

    @property
    def mana_value(self) -> int:
        return self.generic + self.red


@dataclass
class Permanent:
    card: CardDef
    tapped: bool = False
    summoning_sick: bool = True
    damage_marked: int = 0

    def can_attack(self) -> bool:
        return self.card.card_type == "creature" and not self.tapped and not self.card.defender and (not self.summoning_sick or self.card.haste)

    def can_block(self) -> bool:
        return self.card.card_type == "creature" and not self.tapped


@dataclass
class Land:
    servo_skulls: int = 0
    tapped: bool = False


@dataclass
class GameTelemetry:
    st_drawn: Counter = field(default_factory=Counter)
    st_cast: Counter = field(default_factory=Counter)
    st_opening: Counter = field(default_factory=Counter)


class Player:
    def __init__(self, name: str, deck: List[str], card_db: Dict[str, CardDef], is_st: bool):
        self.name = name
        self.card_db = card_db
        self.is_st = is_st
        self.life = 20
        self.library = list(deck)
        random.shuffle(self.library)
        self.hand: List[str] = []
        self.graveyard: List[str] = []
        self.battlefield: List[Permanent] = []
        self.lands: List[Land] = []
        self.land_played_this_turn = False
        self.pending_servitor_returns = 0
        self.telemetry: Optional[GameTelemetry] = None

    def draw(self, n: int = 1) -> None:
        for _ in range(n):
            if not self.library:
                return
            name = self.library.pop()
            self.hand.append(name)
            if self.is_st and self.telemetry is not None and name != "Mountain":
                self.telemetry.st_drawn[name] += 1

    def untap(self) -> None:
        for land in self.lands:
            land.tapped = False
        for perm in self.battlefield:
            perm.tapped = False
            perm.damage_marked = 0
            if perm.card.card_type == "creature":
                perm.summoning_sick = False
        self.land_played_this_turn = False

    def untapped_land_count(self) -> int:
        return sum(1 for x in self.lands if not x.tapped)

    def drop_pod_count(self) -> int:
        return sum(1 for p in self.battlefield if p.card.name == "Drop Pod S.T")

    def creatures(self) -> List[Permanent]:
        return [p for p in self.battlefield if p.card.card_type == "creature"]


CARD_DB: Dict[str, CardDef] = {
    "Mountain": CardDef("Mountain", card_type="land"),
    "Servo-Skull": CardDef("Servo-Skull", red=1, card_type="artifact", tags=("servo", "artifact")),
    "Drop Pod S.T": CardDef("Drop Pod S.T", generic=2, card_type="creature", power=0, toughness=3, defender=True, tags=("artifact", "drop_pod")),
    "Servitor": CardDef("Servitor", generic=1, red=1, power=2, toughness=1, haste=True, tags=("artifact", "servitor")),
    "Terminator S.T": CardDef("Terminator S.T", generic=1, red=2, power=3, toughness=4, trample=True),
    "Pyroclast Squad S.T": CardDef("Pyroclast Squad S.T", generic=1, red=2, power=3, toughness=3, trample=True, tags=("pyroclast",)),
    "Infernus S.T": CardDef("Infernus S.T", generic=1, red=2, power=3, toughness=2, trample=True, tags=("infernus",)),
    "Demolitionist S.T": CardDef("Demolitionist S.T", generic=3, red=1, power=3, toughness=2, tags=("demolitionist",)),
    "Whirlwind S.T": CardDef("Whirlwind S.T", generic=3, red=2, card_type="artifact", tags=("whirlwind", "artifact")),
    "Eradicator S.T": CardDef("Eradicator S.T", generic=3, red=2, power=6, toughness=5, menace=True, tags=("eradicator",)),
    "Exterminatus S.T": CardDef("Exterminatus S.T", generic=3, red=2, card_type="sorcery", tags=("exterminatus",)),
    "Vindicator S.T": CardDef("Vindicator S.T", red=8, power=8, toughness=12, trample=True, ward=3, tags=("artifact", "vindicator")),
    "Raging Goblin": CardDef("Raging Goblin", red=1, power=1, toughness=1, haste=True),
    "Goblin Piker": CardDef("Goblin Piker", generic=1, red=1, power=2, toughness=1),
    "Goblin Chariot": CardDef("Goblin Chariot", generic=2, red=1, power=2, toughness=2, haste=True),
    "Hill Giant": CardDef("Hill Giant", generic=3, red=1, power=3, toughness=3),
    "Lightning Elemental": CardDef("Lightning Elemental", generic=3, red=1, power=4, toughness=1, haste=True),
    "Shock": CardDef("Shock", red=1, card_type="instant", damage=2, tags=("burn",)),
    "Lightning Strike": CardDef("Lightning Strike", generic=1, red=1, card_type="instant", damage=3, tags=("burn",)),
    "Volcanic Hammer": CardDef("Volcanic Hammer", generic=1, red=1, card_type="sorcery", damage=3, tags=("burn",)),
    "Lava Axe": CardDef("Lava Axe", generic=4, red=1, card_type="sorcery", damage=5, tags=("face_only",)),
}

ST_DECK = (
    ["Mountain"] * 24 + ["Servo-Skull"] + ["Exterminatus S.T"] + ["Vindicator S.T"] * 2
    + ["Drop Pod S.T"] * 4 + ["Servitor"] * 4 + ["Terminator S.T"] * 4
    + ["Pyroclast Squad S.T"] * 4 + ["Infernus S.T"] * 4 + ["Demolitionist S.T"] * 4
    + ["Whirlwind S.T"] * 4 + ["Eradicator S.T"] * 4
)

BENCHMARK_DECK = (
    ["Mountain"] * 24 + ["Raging Goblin"] * 4 + ["Goblin Piker"] * 4 + ["Goblin Chariot"] * 4
    + ["Hill Giant"] * 4 + ["Lightning Elemental"] * 4 + ["Shock"] * 4 + ["Lightning Strike"] * 4
    + ["Volcanic Hammer"] * 4 + ["Lava Axe"] * 4
)

assert len(ST_DECK) == 60
assert len(BENCHMARK_DECK) == 60
ST_NONLAND_NAMES = sorted({x for x in ST_DECK if x != "Mountain"})


def effective_cost(player: Player, card: CardDef, ward_tax: int = 0) -> Tuple[int, int]:
    generic = card.generic + ward_tax
    red = card.red
    if player.is_st and card.card_type == "creature" and card.red > 0:
        generic = max(0, generic - player.drop_pod_count())
    return generic, red


def can_pay(player: Player, card: CardDef, ward_tax: int = 0) -> bool:
    generic, red = effective_cost(player, card, ward_tax)
    return player.untapped_land_count() >= generic + red


def pay_mana(player: Player, amount: int) -> bool:
    untapped = [land for land in player.lands if not land.tapped]
    if len(untapped) < amount:
        return False
    untapped.sort(key=lambda land: land.servo_skulls)
    for land in untapped[:amount]:
        land.tapped = True
        if land.servo_skulls:
            player.life -= land.servo_skulls
            if player.life <= 0:
                return False
    return True


def pay_card_cost(player: Player, card: CardDef, ward_tax: int = 0) -> bool:
    generic, red = effective_cost(player, card, ward_tax)
    return pay_mana(player, generic + red)


def hand_keep_score(hand: List[str]) -> bool:
    lands = hand.count("Mountain")
    if lands < 2 or lands > 5:
        return False
    spells = [CARD_DB[n] for n in hand if n != "Mountain"]
    return any(c.mana_value <= 3 for c in spells) or lands >= 3


def london_mulligan(player: Player) -> None:
    mulligans = 0
    while True:
        player.hand.clear()
        player.library = list(ST_DECK if player.is_st else BENCHMARK_DECK)
        random.shuffle(player.library)
        for _ in range(7):
            player.hand.append(player.library.pop())
        if hand_keep_score(player.hand) or mulligans >= 2:
            break
        mulligans += 1
    for _ in range(mulligans):
        nonlands = [n for n in player.hand if n != "Mountain"]
        if nonlands:
            choice = max(nonlands, key=lambda n: CARD_DB[n].mana_value)
        else:
            choice = "Mountain"
        player.hand.remove(choice)
        player.library.insert(0, choice)


def permanent_value(p: Permanent) -> float:
    c = p.card
    value = c.power + 0.55 * c.toughness + c.mana_value * 0.4
    if c.haste:
        value += 0.4
    if c.trample:
        value += 0.6
    if c.menace:
        value += 0.7
    if c.ward:
        value += 0.8
    if "vindicator" in c.tags:
        value += 4
    if "drop_pod" in c.tags:
        value += 2
    return value


def move_dead_creatures(owner: Player, opponent: Player) -> None:
    survivors = []
    for p in owner.battlefield:
        if p.card.card_type == "creature" and p.damage_marked >= p.card.toughness:
            owner.graveyard.append(p.card.name)
            if p.card.name == "Servitor":
                owner.pending_servitor_returns += 1
        else:
            survivors.append(p)
    owner.battlefield = survivors


def kill_if_lethal(owner: Player, opponent: Player) -> None:
    move_dead_creatures(owner, opponent)
    move_dead_creatures(opponent, owner)


def play_land(player: Player) -> None:
    if not player.land_played_this_turn and "Mountain" in player.hand:
        player.hand.remove("Mountain")
        player.lands.append(Land())
        player.land_played_this_turn = True


def attach_servo_skull(caster: Player, opponent: Player) -> None:
    if opponent.lands:
        target = min(opponent.lands, key=lambda land: land.servo_skulls)
        target.servo_skulls += 1


def cast_permanent(player: Player, opponent: Player, card: CardDef) -> bool:
    if not can_pay(player, card) or not pay_card_cost(player, card):
        return False
    player.hand.remove(card.name)
    if player.is_st and player.telemetry is not None:
        player.telemetry.st_cast[card.name] += 1
    player.battlefield.append(Permanent(card=card, tapped=False, summoning_sick=(card.card_type == "creature" and not card.haste)))
    if card.name == "Servo-Skull":
        attach_servo_skull(player, opponent)
    return True


def benchmark_burn_target(player: Player, opponent: Player, card: CardDef) -> Tuple[str, Optional[Permanent], int]:
    if "face_only" in card.tags:
        return "face", None, 0
    candidates = []
    for p in opponent.creatures():
        ward_tax = p.card.ward
        if not can_pay(player, card, ward_tax=ward_tax):
            continue
        if card.damage >= (p.card.toughness - p.damage_marked):
            candidates.append((permanent_value(p) - 0.15 * ward_tax, p, ward_tax))
    if candidates:
        candidates.sort(key=lambda x: x[0], reverse=True)
        best = candidates[0]
        if best[0] >= 2.3 or opponent.life > card.damage + 3:
            return "creature", best[1], best[2]
    return "face", None, 0


def cast_benchmark_burn(player: Player, opponent: Player, card: CardDef) -> bool:
    target_type, target, ward_tax = benchmark_burn_target(player, opponent, card)
    if not can_pay(player, card, ward_tax):
        return False
    if not pay_card_cost(player, card, ward_tax):
        return True
    player.hand.remove(card.name)
    player.graveyard.append(card.name)
    if target_type == "face":
        opponent.life -= card.damage
    elif target is not None and target in opponent.battlefield:
        target.damage_marked += card.damage
        kill_if_lethal(opponent, player)
    return True


def st_spell_priority(player: Player, opponent: Player, name: str) -> float:
    c = CARD_DB[name]
    if name == "Exterminatus S.T":
        return -99
    score = 10 - c.mana_value
    if name == "Servo-Skull": score += 5 if opponent.lands else -10
    if name == "Drop Pod S.T": score += 4
    if name == "Whirlwind S.T": score += 5 if opponent.creatures() else 2
    if name == "Servitor": score += 2
    if name == "Vindicator S.T": score += 7
    if name == "Eradicator S.T": score += 3
    return score


def benchmark_spell_priority(name: str) -> float:
    c = CARD_DB[name]
    if "burn" in c.tags or "face_only" in c.tags:
        return 20 + c.damage - c.mana_value
    return 10 - c.mana_value + c.power * 0.7 + (2 if c.haste else 0)


def main_phase(player: Player, opponent: Player) -> None:
    safety = 0
    while safety < 30 and player.life > 0 and opponent.life > 0:
        safety += 1
        options = [n for n in player.hand if n != "Mountain"]
        if not options:
            break
        if player.is_st:
            options.sort(key=lambda n: st_spell_priority(player, opponent, n), reverse=True)
            cast_any = False
            for name in options:
                card = CARD_DB[name]
                if card.card_type == "sorcery":
                    continue
                if can_pay(player, card):
                    cast_any = cast_permanent(player, opponent, card)
                    if cast_any:
                        break
            if not cast_any:
                break
        else:
            options.sort(key=benchmark_spell_priority, reverse=True)
            cast_any = False
            for name in options:
                card = CARD_DB[name]
                if card.card_type in ("instant", "sorcery") and ("burn" in card.tags or "face_only" in card.tags):
                    _, _, ward_tax = benchmark_burn_target(player, opponent, card)
                    if can_pay(player, card, ward_tax):
                        cast_any = cast_benchmark_burn(player, opponent, card)
                        break
                elif can_pay(player, card):
                    cast_any = cast_permanent(player, opponent, card)
                    break
            if not cast_any:
                break


def activate_whirlwinds(player: Player, opponent: Player) -> None:
    for w in [p for p in player.battlefield if p.card.name == "Whirlwind S.T" and not p.tapped]:
        if not opponent.creatures():
            break
        target = max(opponent.creatures(), key=permanent_value)
        w.tapped = True
        target.damage_marked += 3
        if target.damage_marked < target.card.toughness:
            target.tapped = True
        kill_if_lethal(opponent, player)


def vindicator_trigger(attacker_owner: Player, defender: Player) -> None:
    if defender.creatures():
        target = max(defender.creatures(), key=permanent_value)
        target.damage_marked += 4
        kill_if_lethal(defender, attacker_owner)


def choose_blockers(defender: Player, attackers: List[Permanent]) -> Dict[int, List[Permanent]]:
    available = [p for p in defender.creatures() if p.can_block()]
    blocks: Dict[int, List[Permanent]] = defaultdict(list)
    ordered = sorted(list(enumerate(attackers)), key=lambda pair: permanent_value(pair[1]) + pair[1].card.power * 0.8, reverse=True)
    for idx, atk in ordered:
        need = 2 if atk.card.menace else 1
        if len(available) < need:
            continue
        combos = []
        if need == 1:
            for b in available:
                lethal = b.card.power >= atk.card.toughness
                loss_risk = atk.card.power >= b.card.toughness
                score = (5 if lethal else 0) - (1.5 if loss_risk else 0) - 0.15 * permanent_value(b) + min(b.card.toughness, atk.card.power) * 0.25
                combos.append((score, [b]))
        else:
            for i in range(len(available)):
                for j in range(i + 1, len(available)):
                    pair = [available[i], available[j]]
                    lethal = sum(x.card.power for x in pair) >= atk.card.toughness
                    score = (5 if lethal else 0) - 0.12 * sum(permanent_value(x) for x in pair)
                    combos.append((score, pair))
        if not combos:
            continue
        combos.sort(key=lambda x: x[0], reverse=True)
        best_score, chosen = combos[0]
        if best_score < 0 and defender.life > 8 and atk.card.power <= 3:
            continue
        for b in chosen:
            available.remove(b)
            blocks[idx].append(b)
    return blocks


def combat(attacker_owner: Player, defender: Player) -> None:
    attackers = [p for p in attacker_owner.creatures() if p.can_attack()]
    if not attackers:
        return
    for atk in attackers:
        atk.tapped = True
        if atk.card.name == "Vindicator S.T":
            vindicator_trigger(attacker_owner, defender)
            if defender.life <= 0:
                return
    attackers = [a for a in attackers if a in attacker_owner.battlefield]
    blocks = choose_blockers(defender, attackers)
    for idx, bs in blocks.items():
        for b in list(bs):
            if b.card.name == "Vindicator S.T" and b in defender.battlefield:
                attackers[idx].damage_marked += 4
    kill_if_lethal(attacker_owner, defender)
    living_attacker_indices = {i for i, a in enumerate(attackers) if a in attacker_owner.battlefield}
    player_damage = 0
    for idx, atk in enumerate(attackers):
        if idx not in living_attacker_indices:
            continue
        bs = [b for b in blocks.get(idx, []) if b in defender.battlefield]
        if not bs:
            player_damage += atk.card.power
            continue
        atk.damage_marked += sum(b.card.power for b in bs)
        remaining = atk.card.power
        for b in sorted(bs, key=lambda x: x.card.toughness):
            lethal_needed = max(0, b.card.toughness - b.damage_marked)
            assign = min(remaining, lethal_needed)
            b.damage_marked += assign
            remaining -= assign
            if remaining <= 0:
                break
        if atk.card.trample and remaining > 0:
            player_damage += remaining
        for b in bs:
            if b.card.name == "Drop Pod S.T":
                atk.damage_marked += 1
    defender.life -= player_damage
    kill_if_lethal(attacker_owner, defender)


def end_step(player: Player) -> None:
    while player.pending_servitor_returns > 0:
        try:
            player.graveyard.remove("Servitor")
            player.hand.append("Servitor")
        except ValueError:
            pass
        player.pending_servitor_returns -= 1
    while len(player.hand) > 7:
        nonlands = [n for n in player.hand if n != "Mountain"]
        choice = max(nonlands, key=lambda n: CARD_DB[n].mana_value) if nonlands else player.hand[-1]
        player.hand.remove(choice)
        player.graveyard.append(choice)


def play_game(st_starts: bool) -> Tuple[str, int, GameTelemetry]:
    st = Player("S.T", ST_DECK, CARD_DB, is_st=True)
    br = Player("Benchmark Red", BENCHMARK_DECK, CARD_DB, is_st=False)
    telemetry = GameTelemetry()
    st.telemetry = telemetry
    london_mulligan(st)
    london_mulligan(br)
    for n in st.hand:
        if n != "Mountain":
            telemetry.st_drawn[n] += 1
            telemetry.st_opening[n] += 1
    players = [st, br] if st_starts else [br, st]
    turn_number = 0
    for _ in range(1, MAX_TURNS + 1):
        for active in players:
            nonactive = br if active is st else st
            turn_number += 1
            active.untap()
            if turn_number != 1:
                if not active.library:
                    return nonactive.name, turn_number, telemetry
                active.draw(1)
            play_land(active)
            main_phase(active, nonactive)
            if active.life <= 0: return nonactive.name, turn_number, telemetry
            if nonactive.life <= 0: return active.name, turn_number, telemetry
            if active.is_st:
                activate_whirlwinds(active, nonactive)
                if nonactive.life <= 0: return active.name, turn_number, telemetry
            combat(active, nonactive)
            if active.life <= 0: return nonactive.name, turn_number, telemetry
            if nonactive.life <= 0: return active.name, turn_number, telemetry
            main_phase(active, nonactive)
            if active.life <= 0: return nonactive.name, turn_number, telemetry
            if nonactive.life <= 0: return active.name, turn_number, telemetry
            end_step(active)
            end_step(nonactive)
    if st.life != br.life:
        return ("S.T" if st.life > br.life else "Benchmark Red"), turn_number, telemetry
    st_board = sum(permanent_value(p) for p in st.battlefield)
    br_board = sum(permanent_value(p) for p in br.battlefield)
    if abs(st_board - br_board) > 0.01:
        return ("S.T" if st_board > br_board else "Benchmark Red"), turn_number, telemetry
    return "Draw", turn_number, telemetry


def empty_state() -> dict:
    return {
        "engine_version": "st_vs_benchmark_red_v1", "runs": 0, "total_games": 0,
        "st_wins": 0, "benchmark_wins": 0, "draws": 0,
        "st_on_play_games": 0, "st_on_play_wins": 0, "st_on_draw_games": 0, "st_on_draw_wins": 0,
        "total_turns": 0,
        "card_stats": {name: {"games_drawn": 0, "wins_when_drawn": 0, "games_cast": 0, "wins_when_cast": 0, "opening_hand_games": 0, "copies_drawn": 0, "copies_cast": 0} for name in ST_NONLAND_NAMES},
    }


def load_state() -> dict:
    if SUMMARY_FILE.exists():
        with open(SUMMARY_FILE, "r", encoding="utf-8") as f:
            state = json.load(f)
        state.setdefault("card_stats", {})
        for name in ST_NONLAND_NAMES:
            state["card_stats"].setdefault(name, empty_state()["card_stats"][name])
        return state
    return empty_state()


def pct(n: int, d: int) -> float:
    return 100.0 * n / d if d else 0.0


def observational_card_rows(state: dict) -> List[dict]:
    rows = []
    total_games, total_wins = state["total_games"], state["st_wins"]
    for name in ST_NONLAND_NAMES:
        s = state["card_stats"][name]
        dg, cg = s["games_drawn"], s["games_cast"]
        ndg, ncg = total_games - dg, total_games - cg
        wd, wc = s["wins_when_drawn"], s["wins_when_cast"]
        wnd, wnc = total_wins - wd, total_wins - wc
        drawn_wr, not_drawn_wr = pct(wd, dg), pct(wnd, ndg)
        cast_wr, not_cast_wr = pct(wc, cg), pct(wnc, ncg)
        rows.append({
            "card": name, "games_drawn": dg, "draw_rate": pct(dg, total_games),
            "win_rate_when_drawn": drawn_wr, "win_rate_when_not_drawn": not_drawn_wr,
            "drawn_delta_pp": drawn_wr - not_drawn_wr if dg and ndg else 0.0,
            "games_cast": cg, "cast_rate": pct(cg, total_games),
            "win_rate_when_cast": cast_wr, "win_rate_when_not_cast": not_cast_wr,
            "cast_delta_pp": cast_wr - not_cast_wr if cg and ncg else 0.0,
            "opening_hand_games": s["opening_hand_games"], "copies_drawn": s["copies_drawn"], "copies_cast": s["copies_cast"],
        })
    rows.sort(key=lambda r: abs(r["drawn_delta_pp"]), reverse=True)
    return rows


def balance_label(win_rate: float) -> str:
    if 47 <= win_rate <= 53: return "very close to even"
    if 45 <= win_rate <= 55: return "within the initial balance target"
    if win_rate > 60: return "strongly overtuned in this matchup"
    if win_rate > 55: return "favored / potentially overtuned in this matchup"
    if win_rate < 40: return "strongly undertuned in this matchup"
    return "unfavored / potentially undertuned in this matchup"


def write_report(state: dict, batch: dict) -> None:
    wr = pct(state["st_wins"], state["total_games"])
    rows = observational_card_rows(state)
    lines = [
        "# S.T vs Benchmark Red — Balance Report", "",
        f"**Engine:** {state['engine_version']}", f"**Cumulative games:** {state['total_games']:,}",
        f"**S.T wins:** {state['st_wins']:,} ({wr:.2f}%)",
        f"**Benchmark Red wins:** {state['benchmark_wins']:,} ({pct(state['benchmark_wins'], state['total_games']):.2f}%)",
        f"**Draws:** {state['draws']:,}", f"**Deck-level read:** {balance_label(wr)}.", "",
        "## Play / draw split", "",
        f"- S.T on the play: {pct(state['st_on_play_wins'], state['st_on_play_games']):.2f}% over {state['st_on_play_games']:,} games",
        f"- S.T on the draw: {pct(state['st_on_draw_wins'], state['st_on_draw_games']):.2f}% over {state['st_on_draw_games']:,} games", "",
        "## Individual-card observational telemetry", "",
        "These deltas are signals, not causal proof. Paired replacement testing is the next phase.", "",
        "| Card | Drawn WR | Not drawn WR | Δ | Cast WR | Cast rate | Games drawn |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for r in rows:
        lines.append(f"| {r['card']} | {r['win_rate_when_drawn']:.2f}% | {r['win_rate_when_not_drawn']:.2f}% | {r['drawn_delta_pp']:+.2f} pp | {r['win_rate_when_cast']:.2f}% | {r['cast_rate']:.2f}% | {r['games_drawn']:,} |")
    lines += [
        "", "## Matchup limitations", "",
        "- Benchmark Red uses only Mountains, so effects that destroy lands which can't produce {R} have no legal targets here.",
        "- Benchmark Red has no artifacts, so Pyroclast's artifact-destruction trigger is inactive.",
        "- V1 models the mechanics needed for this matchup, not every timing/priority/stack corner case in Magic.",
        "- Do not judge a card's final fairness from one matchup alone.", "",
        "## Next step", "",
        "Add paired A/B replacement tests using the same random seeds to estimate each card's causal win-rate contribution.", "",
        f"Last cloud batch: {batch['games']:,} games at {batch['time_utc']}",
    ]
    REPORT_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")


def save_state(state: dict, batch: dict) -> None:
    SUMMARY_FILE.write_text(json.dumps(state, indent=2), encoding="utf-8")
    rows = observational_card_rows(state)
    CARD_STATS_FILE.write_text(json.dumps(rows, indent=2), encoding="utf-8")
    is_new = not HISTORY_FILE.exists()
    with open(HISTORY_FILE, "a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        if is_new:
            writer.writerow(["run", "time_utc", "games", "st_wins", "benchmark_wins", "draws", "st_win_rate", "cumulative_games", "cumulative_st_win_rate"])
        writer.writerow([state["runs"], batch["time_utc"], batch["games"], batch["st_wins"], batch["benchmark_wins"], batch["draws"], f"{pct(batch['st_wins'], batch['games']):.4f}", state["total_games"], f"{pct(state['st_wins'], state['total_games']):.4f}"])
    write_report(state, batch)


def run_batch() -> None:
    state = load_state()
    batch = {"games": GAMES_PER_RUN, "st_wins": 0, "benchmark_wins": 0, "draws": 0, "turns": 0, "time_utc": datetime.now(timezone.utc).isoformat()}
    for game_idx in range(GAMES_PER_RUN):
        st_starts = game_idx % 2 == 0
        winner, turns, telemetry = play_game(st_starts)
        batch["turns"] += turns
        st_win = winner == "S.T"
        if winner == "S.T": batch["st_wins"] += 1
        elif winner == "Benchmark Red": batch["benchmark_wins"] += 1
        else: batch["draws"] += 1
        state["st_on_play_games" if st_starts else "st_on_draw_games"] += 1
        if st_win: state["st_on_play_wins" if st_starts else "st_on_draw_wins"] += 1
        for name in ST_NONLAND_NAMES:
            s = state["card_stats"][name]
            drawn_copies, cast_copies, opening_copies = telemetry.st_drawn[name], telemetry.st_cast[name], telemetry.st_opening[name]
            if drawn_copies > 0:
                s["games_drawn"] += 1
                if st_win: s["wins_when_drawn"] += 1
            if cast_copies > 0:
                s["games_cast"] += 1
                if st_win: s["wins_when_cast"] += 1
            if opening_copies > 0: s["opening_hand_games"] += 1
            s["copies_drawn"] += drawn_copies
            s["copies_cast"] += cast_copies
    state["runs"] += 1
    state["total_games"] += batch["games"]
    state["st_wins"] += batch["st_wins"]
    state["benchmark_wins"] += batch["benchmark_wins"]
    state["draws"] += batch["draws"]
    state["total_turns"] += batch["turns"]
    state["last_run_utc"] = batch["time_utc"]
    save_state(state, batch)
    print("=" * 72)
    print("MTG CLOUD SIMULATOR — S.T vs BENCHMARK RED")
    print("=" * 72)
    print(f"Engine: {state['engine_version']}")
    print(f"Run number: {state['runs']}")
    print(f"Games this run: {batch['games']:,}")
    print(f"S.T batch wins: {batch['st_wins']:,} ({pct(batch['st_wins'], batch['games']):.2f}%)")
    print(f"Benchmark batch wins: {batch['benchmark_wins']:,} ({pct(batch['benchmark_wins'], batch['games']):.2f}%)")
    print(f"Draws: {batch['draws']:,}")
    print(f"CUMULATIVE GAMES: {state['total_games']:,}")
    print(f"S.T cumulative win rate: {pct(state['st_wins'], state['total_games']):.2f}%")
    print("Top observational card signals:")
    for row in observational_card_rows(state)[:6]:
        print(f"  {row['card']:<24} {row['drawn_delta_pp']:+6.2f} pp  drawn in {row['draw_rate']:.1f}%")
    print(f"Report saved to: {REPORT_FILE}")
    print("=" * 72)


if __name__ == "__main__":
    run_batch()
