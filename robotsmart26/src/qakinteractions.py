#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
qakinteractions.py - analisi delle interazioni di un modello qak.

Legge SOLO la parte qak del modello: commenti e codice Kotlin racchiuso
fra [# e #] (il "codice delle parti") vengono eliminati prima dell'analisi.

Uso:
    python qakinteractions.py modello.qak [altro.qak ...]

Controlli
  1. Grafo delle interazioni: request, dispatch, eventi emessi, deleghe.
  2. Eventi: chi li emette e in quali stati sono percepiti
     (eventi senza emettitore o senza percettore nel modello).
  3. Attese circolari potenziali: A attende la reply di B mentre B invia
     ad A un messaggio che A, in quello stato, non accetta.
     Il controllo e' sensibile al flusso: un invio di B (stato bs, messaggio
     m2) e' escluso se non puo' essere pendente mentre A e' nello stato s:
       a) bs non e' raggiungibile dallo stato iniziale di B, oppure
       b) conteggio invii/consumi: N_max = numero massimo di m2 che il
          modello puo' inviare ad A (finito solo se ogni invio sta in uno
          stato "one-shot", cioe' raggiungibile e non appartenente a un
          ciclo); C_min = numero minimo di m2 che A consuma lungo i cammini
          dal suo stato iniziale a s. Se C_min >= N_max, quando A e' in s
          tutti gli m2 sono gia' stati consumati.
  4. Stati terminali: stati senza Transition, Goto o returnFromInterrupt.
     Se l'attore e' destinatario di messaggi, quelli che arrivano dopo
     l'ingresso nello stato restano non elaborati.
  5. Copertura delle reply: per ogni request accettata (whenRequest m -> S)
     si esplorano i cammini a partire da S; si segnala ogni cammino che
     arriva a uno stato terminale, o torna ad accettare m, senza aver
     eseguito replyTo m.

Limiti (analisi statica, sovra-approssimata)
  - non conosce il tempo ne' i dati: le condizioni di Goto ... if ... else
    e le alternative di una Transition sono considerate tutte possibili;
  - non vede le interazioni realizzate dentro il codice Kotlin
    (oggetti withobj, chiamate dirette a librerie);
  - il parsing e' basato su espressioni regolari, non sulla grammatica Xtext.
"""
import re
import sys


# ------------------------------------------------------------------ parsing
def clean(txt):
    txt = re.sub(r"/\*.*?\*/", "", txt, flags=re.S)        # commenti blocco
    txt = re.sub(r"//[^\n]*", "", txt)                      # commenti riga
    txt = re.sub(r"\[#.*?#\]", " ", txt, flags=re.S)        # codice delle parti
    return txt


def parse(txt):
    txt = clean(txt)
    msgs = {mid: kind for kind, mid in re.findall(
        r"^\s*(Dispatch|Request|Reply|Event)\s+(\w+)\s*:", txt, re.M)}
    actors = {}
    starts = [(m.start(), m.group(1))
              for m in re.finditer(r"QActor\s+(\w+)\s+context", txt)]
    for i, (pos, name) in enumerate(starts):
        body = txt[pos: starts[i + 1][0] if i + 1 < len(starts) else len(txt)]
        sp = [(m.start(), m.group(1), bool(m.group(2)))
              for m in re.finditer(r"State\s+(\w+)\s*(initial)?", body)]
        states, initial = {}, None
        for j, (spos, sname, isinit) in enumerate(sp):
            seg = body[spos: sp[j + 1][0] if j + 1 < len(sp) else len(body)]
            if isinit:
                initial = sname
            states[sname] = {
                "request":   re.findall(r"request\s+(\w+)\s+-m\s+(\w+)", seg),
                "forward":   re.findall(r"forward\s+(\w+)\s+-m\s+(\w+)", seg),
                "emit":      re.findall(r"\bemit(?:local|localstream)?\s+(\w+)", seg),
                "delegate":  re.findall(r"delegate\s+(\w+)\s+to\s+(\w+)", seg),
                "replyto":   re.findall(r"replyTo\s+(\w+)\s+with", seg),
                "accepts":   re.findall(
                    r"when(?:Msg|Request|Reply|Event|InterruptEvent)\s+(\w+)", seg),
                "onrequest": re.findall(r"whenRequest\s+(\w+)\s*->\s*(\w+)", seg),
                "next":      re.findall(r"(?:Goto|->|else)\s+(\w+)", seg),
                # archi etichettati: (messaggio consumato | None, stato destinazione)
                "trans":     re.findall(
                    r"when(?:Msg|Request|Reply|Event|InterruptEvent)\s+(\w+)[^\n]*?->\s*(\w+)", seg),
                "plain":     re.findall(r"whenTime\s+\w+\s*->\s*(\w+)", seg)
                             + re.findall(r"\b(?:Goto|else)\s+(\w+)", seg),
                "returns":   "returnFromInterrupt" in seg,
                "terminate": re.search(r"\bterminate\b", seg) is not None,
            }
        actors[name] = {"states": states, "initial": initial}
    return msgs, actors


# --------------------------------------------------------------- utilities
def incoming(actors, target):
    """Messaggi che il modello invia (o delega) all'attore target."""
    res = set()
    for a, ad in actors.items():
        for s, d in ad["states"].items():
            for b, m in d["request"] + d["forward"]:
                if b == target and a != target:
                    res.add(m)
            for m, b in d["delegate"]:
                if b == target:
                    res.add(m)
    return res


def edges(ad, s):
    d = ad["states"].get(s)
    if d is None:
        return []
    return list(d["trans"]) + [(None, t) for t in d["plain"]]


def reach(ad, start, banned=None):
    """Stati raggiungibili da start senza attraversare archi che consumano banned."""
    seen, todo = set(), [start] if start else []
    while todo:
        x = todo.pop()
        if x in seen or x not in ad["states"]:
            continue
        seen.add(x)
        todo += [t for m, t in edges(ad, x) if banned is None or m != banned]
    return seen


def one_shot(ad, s):
    """s raggiungibile e non appartenente ad alcun ciclo: eseguito al piu' una volta."""
    if s not in reach(ad, ad["initial"]):
        return False
    return all(s not in reach(ad, t) for _, t in edges(ad, s))


def min_consumed(ad, m, target):
    """0-1 BFS: minimo numero di consumi di m sui cammini initial -> target."""
    from collections import deque
    INF = float("inf")
    dist = {ad["initial"]: 0}
    dq = deque([ad["initial"]])
    while dq:
        x = dq.popleft()
        for lab, t in edges(ad, x):
            w = 1 if lab == m else 0
            if t in ad["states"] and dist[x] + w < dist.get(t, INF):
                dist[t] = dist[x] + w
                (dq.appendleft if w == 0 else dq.append)(t)
    return dist.get(target, INF)


def max_sent(actors, m, a):
    """Massimo numero di m che il modello puo' recapitare ad a (inf se illimitato).
    Si considerano gli invii diretti e quelli a un attore che delega m ad a."""
    targets = {a} | {x for x, ad in actors.items() for d in ad["states"].values()
                     for mm, b in d["delegate"] if mm == m and b == a}
    n, where = 0, []
    for b, bd in actors.items():
        live = reach(bd, bd["initial"])
        for bs, d in bd["states"].items():
            k = sum(1 for c, mm in d["request"] + d["forward"]
                    if c in targets and mm == m and b != a)
            if k == 0 or bs not in live:
                continue
            if not one_shot(bd, bs):
                return float("inf"), where
            n += k
            where.append(f"{b}.{bs}")
    return n, where


def delegated(actors):
    return {m for ad in actors.values() for d in ad["states"].values()
            for m, _ in d["delegate"]}


# ----------------------------------------------------------------- report
def report(msgs, actors):
    print("== 1. Grafo delle interazioni (mittente -> destinatario : messaggio) ==")
    for a, ad in actors.items():
        for s, d in ad["states"].items():
            for m, b in d["delegate"]:
                print(f"  {a} delega {m} a {b}")
            for b, m in d["request"]:
                print(f"  {a}.{s} --request {m}--> {b}")
            for b, m in d["forward"]:
                if b != a:
                    print(f"  {a}.{s} --dispatch {m}--> {b}")
            for e in d["emit"]:
                print(f"  {a}.{s} ==event {e}==> *")

    print("\n== 2. Eventi: chi li emette, quali stati li percepiscono ==")
    events = [m for m, k in msgs.items() if k == "Event"]
    if not events:
        print("  nessun evento dichiarato")
    for e in events:
        em = [f"{a}.{s}" for a, ad in actors.items()
              for s, d in ad["states"].items() if e in d["emit"]]
        pe = [f"{a}.{s}" for a, ad in actors.items()
              for s, d in ad["states"].items() if e in d["accepts"]]
        print(f"  {e}: emesso da {em or 'NESSUN attore del modello'}; "
              f"percepito in {pe or 'NESSUNO stato del modello'}")

    print("\n== 3. Attese circolari potenziali (request in attesa di reply) ==")
    found, excluded = False, []
    for a, ad in actors.items():
        for s, d in ad["states"].items():
            for b, m in d["request"]:
                acc = set(d["accepts"])
                bact = actors.get(b, {"states": {}, "initial": None})
                blive = reach(bact, bact["initial"])
                for bs, bd in bact["states"].items():
                    for c, m2 in bd["request"] + bd["forward"]:
                        if c == a and m2 not in acc:
                            if bs not in blive:
                                excluded.append(f"  {a}.{s} / {b}.{bs} '{m2}': stato "
                                                f"{b}.{bs} non raggiungibile")
                                continue
                            nmax, where = max_sent(actors, m2, a)
                            cmin = min_consumed(ad, m2, s)
                            if nmax != float("inf") and cmin >= nmax:
                                excluded.append(
                                    f"  {a}.{s} / {b}.{bs} '{m2}': inviato al piu' "
                                    f"{nmax} volta/e (stati one-shot {where}); per "
                                    f"raggiungere '{s}' {a} ne consuma almeno {cmin} "
                                    f"=> nessun '{m2}' pendente in '{s}'")
                                continue
                            found = True
                            kind = "request" if (c, m2) in bd["request"] else "dispatch"
                            print(f"  {a}.{s} attende la reply a '{m}' da {b}, ma {b}.{bs} "
                                  f"invia {kind} '{m2}' ad {a}, che in '{s}' non lo accetta"
                                  + ("  => DEADLOCK se accade" if kind == "request"
                                     else "  => messaggio differito (msgQueueStore)"))
    if not found:
        print("  nessuna")
    if excluded:
        print("  (casi esclusi dall'analisi di flusso)")
        for e in excluded:
            print(e)

    print("\n== 4. Stati terminali ==")
    found = False
    for a, ad in actors.items():
        inc = incoming(actors, a)
        accepted = {m for d in ad["states"].values() for m in d["accepts"]}
        for s, d in ad["states"].items():
            if d["next"] or d["returns"]:
                continue
            found = True
            if d["terminate"]:
                print(f"  {a}.{s}: terminazione esplicita (terminate)")
                continue
            pending = sorted(inc | accepted)
            if pending:
                print(f"  {a}.{s}: stato senza uscita; l'attore e' destinatario di "
                      f"{pending}: i messaggi che arrivano dopo l'ingresso in '{s}' "
                      f"restano non elaborati (dispatch/request in msgQueueStore, "
                      f"eventi scartati)")
            else:
                print(f"  {a}.{s}: stato senza uscita (nessun messaggio in arrivo "
                      f"previsto dal modello: fine del comportamento)")
    if not found:
        print("  nessuno")

    print("\n== 5. Copertura delle reply (request accettate senza replyTo) ==")
    found = False
    for a, ad in actors.items():
        sts = ad["states"]
        for s0, d0 in sts.items():
            for m, start in d0["onrequest"]:
                bad = []

                def dfs(st, path):
                    if st not in sts:
                        return
                    d = sts[st]
                    if m in d["replyto"]:
                        return                                   # cammino coperto
                    if st in path:                               # ciclo senza reply
                        if any(mm == m for mm, _ in sts[st]["onrequest"]):
                            bad.append(path + [st])
                        return
                    succ = d["next"]
                    if not succ and not d["returns"]:
                        bad.append(path + [st])                  # terminale senza reply
                        return
                    if st != start and any(mm == m for mm, _ in d["onrequest"]):
                        bad.append(path + [st])                  # torna ad accettare m
                        return
                    for n in succ:
                        dfs(n, path + [st])

                dfs(start, [])
                for p in bad:
                    found = True
                    print(f"  {a}: request '{m}' accettata in {s0} -> cammino "
                          f"{' -> '.join(p)} senza replyTo {m}")
    if not found:
        print("  ogni request accettata riceve una reply su tutti i cammini")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    for i, f in enumerate(sys.argv[1:]):
        if i:
            print("\n" + "=" * 72 + "\n")
        print(f"### {f}\n")
        with open(f, encoding="utf-8") as fh:
            report(*parse(fh.read()))
