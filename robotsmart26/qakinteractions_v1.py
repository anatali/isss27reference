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
    found = False
    for a, ad in actors.items():
        for s, d in ad["states"].items():
            for b, m in d["request"]:
                acc = set(d["accepts"])
                for bs, bd in actors.get(b, {}).get("states", {}).items():
                    for c, m2 in bd["request"] + bd["forward"]:
                        if c == a and m2 not in acc:
                            found = True
                            kind = "request" if (c, m2) in bd["request"] else "dispatch"
                            print(f"  {a}.{s} attende la reply a '{m}' da {b}, ma {b}.{bs} "
                                  f"invia {kind} '{m2}' ad {a}, che in '{s}' non lo accetta"
                                  + ("  => DEADLOCK se accade" if kind == "request"
                                     else "  => messaggio differito (msgQueueStore)"))
    if not found:
        print("  nessuna")

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
