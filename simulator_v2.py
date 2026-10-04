from __future__ import annotations

import copy, json, random
from dataclasses import dataclass
from datetime import datetime, timezone
from itertools import combinations
from pathlib import Path
from typing import Optional
import simulator as core

ENGINE = "st_vs_benchmark_red_v2_fair_search"
GAMES = 500
MAX_TURNS = 30
AUDIT_GAMES = 8
OUT = Path("results/st_vs_benchmark_red_v2")
OUT.mkdir(parents=True, exist_ok=True)
core.RESULTS_DIR = OUT
core.STATE_FILE = OUT / "summary.json"
core.HISTORY_FILE = OUT / "history.csv"
core.CARD_STATS_FILE = OUT / "card_stats.json"
core.REPORT_FILE = OUT / "latest_report.md"
AUDIT_FILE = OUT / "decision_audit.jsonl"
INTEGRITY_FILE = OUT / "integrity.json"

class IllegalAction(RuntimeError): pass

@dataclass(frozen=True)
class Action:
    kind: str
    card: Optional[str] = None
    target: Optional[str] = None
    idx: Optional[int] = None
    def label(self):
        if self.kind == "pass": return "Pass"
        if self.kind == "whirl": return f"Whirlwind -> creature #{self.idx}"
        if self.target: return f"Cast {self.card} -> {self.target} #{self.idx if self.idx is not None else ''}".strip()
        return f"Cast {self.card}"

def value(card):
    if card.card_type == "creature":
        v = 1.15*card.power + .8*card.toughness + .6*card.haste + .7*card.trample + .9*card.menace + .9*(card.ward>0) - 1.4*card.defender
    else: v = .5*card.mana_value
    tags=set(card.tags)
    v += 2.8*("drop_pod" in tags)+1.5*("servitor" in tags)+5*("whirlwind" in tags)+4.2*("vindicator" in tags)
    v += 1.15*card.damage*("burn" in tags)+.75*card.damage*("face_only" in tags)
    return v

def pvalue(p): return value(p.card)-.45*p.damage_marked-.15*p.tapped

def evaluate(players, actor):
    """Own hand + public state only. Opponent hidden card identities/library order are never read."""
    me,opp=players[actor],players[1-actor]
    if opp.life<=0:return 100000
    if me.life<=0:return -100000
    s=4.2*(me.life-opp.life)
    s+=1.55*(sum(map(pvalue,me.battlefield))-sum(map(pvalue,opp.battlefield)))
    s+=.45*(len(me.lands)-len(opp.lands))+.18*(me.untapped_land_count()-opp.untapped_land_count())
    s+=.52*sum(value(core.CARD_DB[n]) for n in me.hand)
    s-=1.25*len(opp.hand)
    s+=.35*(sum(l.servo_skulls for l in opp.lands)-sum(l.servo_skulls for l in me.lands))
    return s

def legal_actions(players,actor):
    me,opp=players[actor],players[1-actor]; out=[Action("pass")]; seen=set()
    for n in me.hand:
        if n=="Mountain" or n=="Exterminatus S.T": continue
        c=core.CARD_DB[n]
        if c.card_type in ("creature","artifact"):
            if n not in seen and core.can_pay(me,c): seen.add(n); out.append(Action("cast",n))
        elif "face_only" in c.tags:
            if core.can_pay(me,c): out.append(Action("cast",n,"player"))
        elif "burn" in c.tags:
            if core.can_pay(me,c): out.append(Action("cast",n,"player"))
            for i,p in enumerate(opp.creatures()):
                if core.can_pay(me,c,p.card.ward): out.append(Action("cast",n,"creature",i))
    if any(p.card.name=="Whirlwind S.T" and not p.tapped for p in me.battlefield):
        out += [Action("whirl",idx=i) for i,_ in enumerate(opp.creatures())]
    return out

def apply(players,actor,a):
    me,opp=players[actor],players[1-actor]
    if a.kind=="pass": return
    if a.kind=="whirl":
        w=next((p for p in me.battlefield if p.card.name=="Whirlwind S.T" and not p.tapped),None); cs=opp.creatures()
        if w is None or a.idx is None or a.idx>=len(cs): raise IllegalAction("bad Whirlwind")
        w.tapped=True; cs[a.idx].damage_marked+=3; core.kill_if_lethal(opp,me); return
    if a.card not in me.hand: raise IllegalAction("card not in hand")
    c=core.CARD_DB[a.card]; tax=0; target=None
    if a.target=="creature":
        cs=opp.creatures()
        if a.idx is None or a.idx>=len(cs): raise IllegalAction("bad target")
        target=cs[a.idx]; tax=target.card.ward
    if not core.can_pay(me,c,tax): raise IllegalAction("unpayable spell")
    if c.card_type in ("creature","artifact"):
        if not core.cast_permanent(me,opp,c): raise IllegalAction("cast failed")
        return
    core.pay_card_cost(me,c,tax); me.hand.remove(c.name); me.graveyard.append(c.name)
    if a.target=="player": opp.life-=c.damage
    else: target.damage_marked+=c.damage; core.kill_if_lethal(opp,me)

def choose(players,actor,audit,phase):
    scored=[]
    for a in legal_actions(players,actor):
        if a.kind=="pass": s=evaluate(players,actor)+.04*players[actor].untapped_land_count()
        else:
            sim=copy.deepcopy(players); apply(sim,actor,a); s=evaluate(sim,actor)
        scored.append((s,a))
    scored.sort(key=lambda x:x[0],reverse=True); best=scored[0][1]
    if audit is not None and len(audit)<60:
        audit.append({"phase":phase,"actor":players[actor].name,"chosen":best.label(),"candidate_count":len(scored),"top":[{"a":a.label(),"s":round(s,2)} for s,a in scored[:3]],"hidden_hand_seen":False})
    return best

def main(players,actor,audit,phase):
    for _ in range(20):
        a=choose(players,actor,audit,phase)
        if a.kind=="pass": return
        apply(players,actor,a)
        if players[0].life<=0 or players[1].life<=0:return

def subsets(n):
    if n<=6:return [x for r in range(n+1) for x in combinations(range(n),r)]
    return [(),tuple(range(n)),tuple(range(max(0,n-1))),tuple(range(max(0,n-2)))]

def attack_score(players,actor,eligible,sub):
    if not sub:return evaluate(players,actor)
    sim=copy.deepcopy(players); original=players[actor].battlefield
    chosen={original.index(eligible[i]) for i in sub}; held=[]
    for i,p in enumerate(sim[actor].battlefield):
        if i not in chosen and p.card.card_type=="creature" and p.can_attack():p.tapped=True;held.append(p)
    core.combat(sim[actor],sim[1-actor])
    for p in held:
        if p in sim[actor].battlefield:p.tapped=False
    return evaluate(sim,actor)

def combat(players,actor,audit):
    eligible=[p for p in players[actor].creatures() if p.can_attack()]
    if not eligible:return
    scored=[(attack_score(players,actor,eligible,s),s) for s in subsets(len(eligible))];scored.sort(key=lambda x:x[0],reverse=True);best=scored[0][1]
    if audit is not None and len(audit)<60:audit.append({"phase":"combat","actor":players[actor].name,"chosen":[eligible[i].card.name for i in best] or ["no attack"],"candidate_count":len(scored),"hidden_hand_seen":False})
    if not best:return
    original=players[actor].battlefield;chosen={original.index(eligible[i]) for i in best};held=[]
    for i,p in enumerate(players[actor].battlefield):
        if i not in chosen and p.card.card_type=="creature" and p.can_attack():p.tapped=True;held.append(p)
    core.combat(players[actor],players[1-actor])
    for p in held:
        if p in players[actor].battlefield:p.tapped=False

def game(seed,on_play,keep_audit):
    random.seed(seed);st=core.Player("S.T",core.ST_DECK,core.CARD_DB,True);br=core.Player("Benchmark Red",core.BENCHMARK_DECK,core.CARD_DB,False);players=[st,br]
    t=core.GameTelemetry();st.telemetry=t;core.london_mulligan(st);core.london_mulligan(br)
    for n in st.hand:
        if n!="Mountain":t.st_drawn[n]+=1;t.st_opening[n]+=1
    order=[0,1] if on_play else [1,0];turn=0;audit=[] if keep_audit else None
    try:
        for _ in range(MAX_TURNS):
            for actor in order:
                turn+=1;opp=1-actor;players[actor].untap()
                if turn!=1:
                    if not players[actor].library:return players[opp].name,turn,t,audit or []
                    players[actor].draw(1)
                core.play_land(players[actor]);main(players,actor,audit,"precombat")
                if min(st.life,br.life)<=0:return ("S.T" if br.life<=0 else "Benchmark Red"),turn,t,audit or []
                combat(players,actor,audit)
                if min(st.life,br.life)<=0:return ("S.T" if br.life<=0 else "Benchmark Red"),turn,t,audit or []
                main(players,actor,audit,"postcombat")
                if min(st.life,br.life)<=0:return ("S.T" if br.life<=0 else "Benchmark Red"),turn,t,audit or []
                core.end_step(st);core.end_step(br)
    except (IllegalAction,ValueError,IndexError) as e:
        raise RuntimeError(f"Legality audit failed in seed {seed}: {e}")
    if st.life!=br.life:return ("S.T" if st.life>br.life else "Benchmark Red"),turn,t,audit or []
    a=sum(map(pvalue,st.battlefield));b=sum(map(pvalue,br.battlefield))
    return ("S.T" if a>b else "Benchmark Red" if b>a else "Draw"),turn,t,audit or []

def report(state,batch):
    rows=core.observational_card_rows(state);valid=state["total_games"]
    lines=["# S.T vs Benchmark Red — V2 Fair-Search AI","",f"**Games:** {valid:,}",f"**S.T win rate:** {core.pct(state['st_wins'],valid):.2f}%","","## Fair-play checks","","- Both decks use the same legal-action generator, evaluator, and one-ply action search.","- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.","- Every run aborts instead of recording results if the legality checker encounters an impossible action.","- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.","","## Card signals","","| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |","|---|---:|---:|---:|---:|---:|"]
    for r in rows:lines.append(f"| {r['card']} | {r['win_rate_when_drawn']:.2f}% | {r['win_rate_when_not_drawn']:.2f}% | {r['drawn_delta_pp']:+.2f} pp | {r['win_rate_when_cast']:.2f}% | {r['cast_rate']:.2f}% |")
    lines += ["","## Limits","","- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.","- Instant-speed response windows and some corner cases remain simplified.","- Do not rebalance an individual card until paired same-seed A/B replacement tests are added."]
    core.REPORT_FILE.write_text("\n".join(lines)+"\n",encoding="utf-8")

def run():
    state=core.load_state();state["engine_version"]=ENGINE;batch={"games":GAMES,"st_wins":0,"benchmark_wins":0,"draws":0,"turns":0,"time_utc":datetime.now(timezone.utc).isoformat()};audits=[];base=4_000_000+state["runs"]*GAMES
    for i in range(GAMES):
        w,tr,t,log=game(base+i,i%2==0,i<AUDIT_GAMES);batch["turns"]+=tr;sw=w=="S.T"
        if w=="S.T":batch["st_wins"]+=1
        elif w=="Benchmark Red":batch["benchmark_wins"]+=1
        else:batch["draws"]+=1
        state["st_on_play_games" if i%2==0 else "st_on_draw_games"]+=1
        if sw:state["st_on_play_wins" if i%2==0 else "st_on_draw_wins"]+=1
        for n in core.ST_NONLAND_NAMES:
            s=state["card_stats"][n];d=t.st_drawn[n];c=t.st_cast[n];o=t.st_opening[n]
            if d:s["games_drawn"]+=1;s["wins_when_drawn"]+=sw
            if c:s["games_cast"]+=1;s["wins_when_cast"]+=sw
            if o:s["opening_hand_games"]+=1
            s["copies_drawn"]+=d;s["copies_cast"]+=c
        if i<AUDIT_GAMES:audits.append({"seed":base+i,"st_on_play":i%2==0,"winner":w,"decisions":log})
    state["runs"]+=1;state["total_games"]+=GAMES;state["st_wins"]+=batch["st_wins"];state["benchmark_wins"]+=batch["benchmark_wins"];state["draws"]+=batch["draws"];state["total_turns"]+=batch["turns"];state["last_run_utc"]=batch["time_utc"]
    core.write_report=report;core.save_state(state,batch)
    AUDIT_FILE.write_text("".join(json.dumps(x)+"\n" for x in audits),encoding="utf-8")
    INTEGRITY_FILE.write_text(json.dumps({"engine":ENGINE,"same_ai_architecture":True,"hidden_information":"own hand + public zones + opponent hand count only","legality_failures":0,"last_run_utc":batch["time_utc"]},indent=2),encoding="utf-8")
    print("="*74);print("MTG CLOUD SIMULATOR — V2 FAIR-SEARCH AI");print("="*74);print(f"Games this run: {GAMES:,}");print("Legality failures: 0");print(f"S.T wins: {batch['st_wins']:,} ({core.pct(batch['st_wins'],GAMES):.2f}%)");print(f"Benchmark wins: {batch['benchmark_wins']:,} ({core.pct(batch['benchmark_wins'],GAMES):.2f}%)");print("Hidden opponent hand identities/library order used by evaluator: NO");print(f"Report: {core.REPORT_FILE}");print(f"Audit: {AUDIT_FILE}");print(f"Integrity: {INTEGRITY_FILE}");print("="*74)

if __name__=="__main__":run()
