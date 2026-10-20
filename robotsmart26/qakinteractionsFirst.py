import re, sys

def clean(txt):
    txt = re.sub(r"/\*.*?\*/", "", txt, flags=re.S)       # commenti blocco
    txt = re.sub(r"//[^\n]*", "", txt)                     # commenti riga
    txt = re.sub(r"\[#.*?#\]", " ", txt, flags=re.S)       # codice delle parti (Kotlin)
    return txt

def parse(txt):
    txt = clean(txt)
    msgs = {mid: kind for kind, mid in
            re.findall(r"^\s*(Dispatch|Request|Reply|Event)\s+(\w+)\s*:", txt, re.M)}
    actors = {}
    starts = [(m.start(), m.group(1)) for m in re.finditer(r"QActor\s+(\w+)\s+context", txt)]
    for i, (pos, name) in enumerate(starts):
        body = txt[pos: starts[i+1][0] if i+1 < len(starts) else len(txt)]
        sp = [(m.start(), m.group(1)) for m in re.finditer(r"State\s+(\w+)", body)]
        states = {}
        for j, (spos, s) in enumerate(sp):
            seg = body[spos: sp[j+1][0] if j+1 < len(sp) else len(body)]
            states[s] = {
              "request": re.findall(r"request\s+(\w+)\s+-m\s+(\w+)", seg),
              "forward": re.findall(r"forward\s+(\w+)\s+-m\s+(\w+)", seg),
              "emit":    re.findall(r"\bemit(?:local)?\s+(\w+)", seg),
              "accepts": re.findall(r"when(?:Msg|Request|Reply|Event|InterruptEvent)\s+(\w+)", seg)}
        actors[name] = states
    return msgs, actors

def report(msgs, actors):
    for e in [m for m, k in msgs.items() if k == "Event"]:
        em = [f"{a}.{s}" for a, st in actors.items() for s, d in st.items() if e in d["emit"]]
        pe = [f"{a}.{s}" for a, st in actors.items() for s, d in st.items() if e in d["accepts"]]
        print(f"{e}: emesso da {em or 'NESSUNO'}; percepito in {pe or 'NESSUNO'}")
    for a, st in actors.items():                       # attese circolari A -> B -> A
        for s, d in st.items():
            for b, m in d["request"]:
                for bs, bd in actors.get(b, {}).items():
                    for c, m2 in bd["request"] + bd["forward"]:
                        if c == a and m2 not in d["accepts"]:
                            kind = "request" if (c, m2) in bd["request"] else "dispatch"
                            print(f"{a}.{s} attende '{m}' da {b}; {b}.{bs} invia {kind} '{m2}' "
                                  f"non accettato" + (" => DEADLOCK" if kind == "request" else ""))

report(*parse(open(sys.argv[1], encoding="utf-8").read()))