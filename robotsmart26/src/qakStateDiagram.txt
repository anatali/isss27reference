#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
qak2diagram.py - Genera automaticamente lo schema a stati dei QActor
contenuti in un file QAK (Qak / Unibo QActor language).

Per ogni QActor viene prodotto un diagramma con:
  * tutti gli STATI (nodi), colorati per "fase" (commento /** STATE XXX: ... */)
  * tutte le TRANSIZIONI (whenReply / whenRequest / whenMsg / whenEvent / whenTime)
  * i Goto (linea tratteggiata)
  * le transizioni verso stati di errore (linea sottile)
  * i messaggi verso altri attori (Req / Reply / Fwd / Emit) come frecce "in uscita"
  * lo stato iniziale (freccia rossa) e una legenda

Uso:
    python qak2diagram.py cargobot.qak
    python qak2diagram.py cargobot.qak --format svg
    python qak2diagram.py cargobot.qak -o schemi --actor cargoservice
    python qak2diagram.py cargobot.qak --rankdir LR --format png,svg,pdf

Requisiti: Python 3.8+ e Graphviz (comando `dot` nel PATH) per il rendering.
Il file .dot non viene conservato (usa --keep-dot); se `dot` non e' installato
viene invece generato solo il file .dot.
"""

import argparse
import colorsys
import html
import random
import re
import shutil
import subprocess
import sys
import textwrap
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# --------------------------------------------------------------------------- #
#  STILE (colori ispirati allo schema di riferimento)
# --------------------------------------------------------------------------- #
# I colori delle fasi (/** STATE xxx: ... */) vengono assegnati automaticamente
# e in modo casuale: nessun nome di fase e' "speciale".
NO_PHASE_COLOR = ("#f5f5f5", "#666666")
ERROR_COLOR = ("#f8cecc", "#b85450")            # rosso
MISSING_COLOR = ("#ffffff", "#999999")

START_COLOR = "#a50022"

# --------------------------------------------------------------------------- #
#  MODELLO DATI
# --------------------------------------------------------------------------- #
@dataclass
class Transition:
    kind: str          # Reply, Request, Msg, Event, Dispatch, Time, TimeVar, Interrupt...
    arg: str           # nome messaggio (o millisecondi per Time)
    target: str


@dataclass
class Action:
    kind: str          # request | forward | emit | reply | askFor
    target: str        # attore destinatario (o messaggio a cui si risponde per 'reply')
    msg: str


@dataclass
class Goto:
    target: str
    cond: Optional[str] = None     # None | "if ..." | "else"


@dataclass
class State:
    name: str
    initial: bool = False
    phase: Optional[str] = None
    first_log: str = ""
    n_logs: int = 0
    n_red_logs: int = 0
    actions: List[Action] = field(default_factory=list)
    transitions: List[Transition] = field(default_factory=list)
    gotos: List[Goto] = field(default_factory=list)

    @property
    def is_error(self) -> bool:
        all_red = self.n_logs > 0 and self.n_red_logs == self.n_logs
        return all_red or bool(re.search(r"error|errore|fail|timeout", self.name, re.I))


@dataclass
class Actor:
    name: str
    context: str
    states: Dict[str, State] = field(default_factory=dict)

    @property
    def initial(self) -> Optional[State]:
        for s in self.states.values():
            if s.initial:
                return s
        return next(iter(self.states.values()), None)


# --------------------------------------------------------------------------- #
#  PARSER
# --------------------------------------------------------------------------- #
PHASE_RE = re.compile(r"\bSTATE\s+([A-Za-z_][A-Za-z_0-9]*)\s*:")
ACTOR_RE = re.compile(
    r"(?<![A-Za-z])(?:QActor\w*|CodedQActor)\s+(\w+)\s+context\s+(\w+)[^{]*\{"
)
STATE_RE = re.compile(r"\bState\s+(\w+)(\s+initial)?\s*\{")
GOTO_RE = re.compile(r"\bGoto\s+(\w+)(?:\s+if\s+\[#(.*?)#\]\s+else\s+(\w+))?", re.S)
TRANS_RE = re.compile(
    r"\bwhen(TimeVar|Time|Reply|Request|Msg|Event|Dispatch|InterruptEvent|Interrupt)"
    r"\s+([\w$.]+)(?:\s+and\s+\[#.*?#\])?\s*->\s*(\w+)",
    re.S,
)


def strip_comments(src: str) -> Tuple[str, List[Tuple[int, str]]]:
    """Rimuove i commenti (stringhe escluse). Restituisce il testo pulito e la
    lista di marcatori di fase (offset nel testo pulito, nome fase) trovati nei
    commenti del tipo '/** STATE INIT: ---- */'."""
    out: List[str] = []
    markers: List[Tuple[int, str]] = []
    length = 0
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            j = min(j + 1, n)
            out.append(src[i:j]); length += j - i
            i = j
        elif src.startswith("//", i):
            j = src.find("\n", i)
            j = n if j < 0 else j
            m = PHASE_RE.search(src[i:j])
            if m:
                markers.append((length, m.group(1).upper()))
            i = j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            m = PHASE_RE.search(src[i:j])
            if m:
                markers.append((length, m.group(1).upper()))
            out.append(" "); length += 1
            i = j
        else:
            out.append(c); length += 1
            i += 1
    return "".join(out), markers


def match_brace(t: str, i: int) -> int:
    """Dato l'indice di '{' restituisce l'indice della '}' corrispondente."""
    depth, n = 0, len(t)
    while i < n:
        c = t[i]
        if c == '"':
            i += 1
            while i < n and t[i] != '"':
                i += 2 if t[i] == "\\" else 1
        elif c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return n - 1


def parse_state_body(st: State, sbody: str) -> None:
    # primo println (descrizione) e colore
    for m in re.finditer(r'\bprintln\(\s*"((?:[^"\\]|\\.)*)"\s*\)(?:\s*color\s+(\w+))?', sbody):
        if not st.first_log:
            st.first_log = m.group(1)
        st.n_logs += 1
        if m.group(2) and m.group(2).lower() == "red":
            st.n_red_logs += 1

    # tolgo codice Kotlin [# ... #] e stringhe, per non avere falsi positivi
    clean = re.sub(r"\[#.*?#\]", " ", sbody, flags=re.S)
    clean = re.sub(r'"(?:[^"\\]|\\.)*"', '""', clean)

    for m in re.finditer(r"\b(request|forward|askFor)\s+(\w+)\s+-m\s+(\w+)", clean):
        st.actions.append(Action(m.group(1), m.group(2), m.group(3)))
    for m in re.finditer(r"\breplyTo\s+(\w+)\s+with\s+(\w+)", clean):
        st.actions.append(Action("reply", m.group(1), m.group(2)))
    for m in re.finditer(r"\bemit\s+(\w+)\s*:", clean):
        st.actions.append(Action("emit", "", m.group(1)))


def parse_tail(st: State, tail: str) -> None:
    for g in GOTO_RE.finditer(tail):
        if g.group(2) is not None:
            cond = " ".join(g.group(2).split())
            st.gotos.append(Goto(g.group(1), f"if {cond}"))
            st.gotos.append(Goto(g.group(3), "else"))
        else:
            st.gotos.append(Goto(g.group(1)))
    for t in TRANS_RE.finditer(tail):
        st.transitions.append(Transition(t.group(1), t.group(2), t.group(3)))


def parse_qak(text: str) -> List[Actor]:
    clean, markers = strip_comments(text)
    actors: List[Actor] = []
    for am in ACTOR_RE.finditer(clean):
        open_idx = am.end() - 1
        close_idx = match_brace(clean, open_idx)
        body = clean[open_idx + 1:close_idx]
        body_off = open_idx + 1
        actor = Actor(am.group(1), am.group(2))
        actor_markers = [(o, p) for (o, p) in markers if body_off <= o <= close_idx]

        found: List[Tuple[re.Match, int]] = []
        pos = 0
        while True:
            sm = STATE_RE.search(body, pos)
            if not sm:
                break
            close = match_brace(body, sm.end() - 1)
            found.append((sm, close))
            pos = close + 1

        for k, (sm, close) in enumerate(found):
            st = State(sm.group(1), initial=bool(sm.group(2)))
            abs_pos = body_off + sm.start()
            for off, ph in actor_markers:
                if off <= abs_pos:
                    st.phase = ph
            parse_state_body(st, body[sm.end():close])
            tail_end = found[k + 1][0].start() if k + 1 < len(found) else len(body)
            parse_tail(st, body[close + 1:tail_end])
            actor.states[st.name] = st
        actors.append(actor)
    return actors


# --------------------------------------------------------------------------- #
#  GENERAZIONE DOT
# --------------------------------------------------------------------------- #
def q(s: str) -> str:
    s = s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
    return '"' + s + '"'


def esc(s: str) -> str:
    return html.escape(s, quote=False)


def kind_label(t: Transition) -> str:
    if t.kind in ("Time", "TimeVar"):
        if t.arg.isdigit():
            secs = int(t.arg) / 1000
            return f"Timer {secs:g} s"
        return f"Timer {t.arg}"
    return f"When{t.kind} {t.arg}"


def short_desc(st: State, max_lines: int = 2, width: int = 30) -> List[str]:
    lines: List[str] = []
    txt = st.first_log
    txt = re.sub(r"^\s*\$name\s*\|\s*", "", txt)
    txt = " ".join(txt.split())
    if txt:
        wrapped = textwrap.wrap(txt, width=width)
        if len(wrapped) > max_lines:
            wrapped = wrapped[:max_lines]
            wrapped[-1] = wrapped[-1].rstrip(".,; ") + "…"
        lines = wrapped
    for t in st.transitions:
        if t.kind in ("Time", "TimeVar") and t.arg.isdigit():
            lines.append(f"Timer {int(t.arg) / 1000:g} s")
            break
    return lines


def random_phase_colors(phases: List[str], seed: Optional[int] = None) -> Dict[str, Tuple[str, str]]:
    """Assegna a ogni fase un colore pastello casuale (riempimento, bordo).
    Le tinte sono equidistanti (con partenza casuale) cosi' le fasi restano ben
    distinguibili; la zona del rosso e' evitata perche' riservata agli errori."""
    rnd = random.Random(seed)
    n = len(phases)
    if n == 0:
        return {}
    start = rnd.random()
    hues = []
    for i in range(n):
        h = (start + i / n) % 1.0
        if h < 0.04 or h > 0.97:          # troppo vicino al rosso degli errori
            h = 0.08 + rnd.random() * 0.05
        hues.append(h)
    rnd.shuffle(hues)

    def hx(h, l, sat):
        r, g, b = colorsys.hls_to_rgb(h, l, sat)
        return "#%02x%02x%02x" % (round(r * 255), round(g * 255), round(b * 255))

    return {ph: (hx(h, 0.88, 0.65), hx(h, 0.45, 0.45)) for ph, h in zip(phases, hues)}


def build_dot(actor: Actor, senders: Dict[str, List[str]], opts) -> str:
    # colori per fase: casuali, uno diverso per ogni fase trovata nei commenti
    phase_names: List[str] = []
    for st in actor.states.values():
        if st.phase and st.phase not in phase_names:
            phase_names.append(st.phase)
    phase_color = random_phase_colors(phase_names, opts.seed)

    L: List[str] = []
    L.append(f"digraph {q(actor.name)} {{")
    L.append(f'  graph [rankdir={opts.rankdir}, fontname="Helvetica", fontsize=18, labelloc=t, '
             f'label={q(f"QActor: {actor.name}   (context: {actor.context})")}, '
             f'nodesep=0.9, ranksep=1.2, newrank=true, pad=0.4, splines={opts.splines}];')
    LBL = "xlabel" if opts.splines == "ortho" else "label"   # ortho non supporta 'label' sugli archi
    L.append('  node  [shape=box, style=filled, fontname="Helvetica", fontsize=10, margin="0.16,0.08"];')
    L.append('  edge  [fontname="Helvetica", fontsize=9, arrowsize=0.8];')

    def node_decl(st: State) -> str:
        if opts.colors:
            fill, border = ERROR_COLOR if st.is_error else phase_color.get(st.phase, NO_PHASE_COLOR)
        else:
            fill, border = "#ffffff", "#333333"
        parts = [f"<B>{esc(st.name)}</B>"]
        if not opts.no_desc:
            parts += [esc(l) for l in short_desc(st)]
        label = "<" + "<BR/>".join(parts) + ">"
        return f"{q(st.name)} [label={label}, fillcolor={q(fill)}, color={q(border)}];"

    # --- nodi (con cluster per fase) ---
    has_phases = any(st.phase for st in actor.states.values())
    if has_phases:
        order: List[Optional[str]] = []
        for st in actor.states.values():
            if st.phase not in order:
                order.append(st.phase)
        border = '"#bbbbbb"' if opts.borders else '"transparent"'
        for ph in order:
            members = [s for s in actor.states.values() if s.phase == ph]
            if ph is None:
                for s in members:
                    L.append("  " + node_decl(s))
                continue
            L.append(f"  subgraph {q('cluster_' + ph)} {{")
            L.append(f'    label=<<B>STATE : {esc(ph)}</B>>; labeljust=l; fontname="Helvetica"; fontsize=11;')
            L.append(f"    style=\"rounded,dashed\"; color={border}; margin=14;")
            for s in members:
                L.append("    " + node_decl(s))
            L.append("  }")
    else:
        for s in actor.states.values():
            L.append("  " + node_decl(s))

    # --- stati citati ma non definiti ---
    missing = set()
    for s in actor.states.values():
        for tgt in [t.target for t in s.transitions] + [g.target for g in s.gotos]:
            if tgt not in actor.states:
                missing.add(tgt)
    for m in sorted(missing):
        L.append(f'  {q(m)} [label=<<B>{esc(m)}</B><BR/>(stato non definito)>, '
                 f'fillcolor={q(MISSING_COLOR[0])}, color={q(MISSING_COLOR[1])}, style="filled,dashed"];')

    def is_err_target(name: str) -> bool:
        return name in actor.states and actor.states[name].is_error

    # --- freccia di inizio ---
    init = actor.initial
    if init:
        L.append(f'  "__start__" [shape=point, width=0.14, color={q(START_COLOR)}];')
        L.append(f'  "__start__" -> {q(init.name)} [color={q(START_COLOR)}, penwidth=3, arrowsize=1.2];')

    # --- archi (raggruppati per (src, dst, stile)) ---
    edges: Dict[Tuple[str, str, str], List[str]] = {}
    order_keys: List[Tuple[str, str, str]] = []

    def add_edge(src: str, dst: str, style: str, label: str) -> None:
        key = (src, dst, style)
        if key not in edges:
            edges[key] = []
            order_keys.append(key)
        if label and label not in edges[key]:
            edges[key].append(label)

    for s in actor.states.values():
        for t in s.transitions:
            style = "error" if is_err_target(t.target) else "msg"
            add_edge(s.name, t.target, style, kind_label(t))
        for g in s.gotos:
            add_edge(s.name, g.target, "goto", g.cond or "")

    for (src, dst, style) in order_keys:
        label = "\n".join(edges[(src, dst, style)])
        attrs = [f"{LBL}={q(label)}"] if label else []
        if style == "goto":
            attrs += ['style=dashed', 'color="#333333"']
        elif style == "error":
            attrs += ['penwidth=0.6', 'color="#555555"']
        else:
            attrs += ['color="#000000"']
        L.append(f"  {q(src)} -> {q(dst)} [{', '.join(attrs)}];")

    # --- frecce "in uscita" verso altri attori ---
    if not opts.no_stubs:
        for s in actor.states.values():
            seen = set()
            for a in s.actions:
                if a.kind in ("request", "askFor"):
                    lab = f"Req : {a.target}"
                elif a.kind == "forward":
                    lab = f"Fwd : {a.target}"
                elif a.kind == "emit":
                    lab = f"Emit : {a.msg}"
                else:
                    dest = senders.get(a.target)
                    lab = "Reply : " + (", ".join(dest) if dest else a.target)
                if lab in seen:
                    continue
                seen.add(lab)
                sid = f"__out_{s.name}_{len(seen)}"
                L.append(f'  {q(sid)} [shape=point, width=0.01, style=invis, label=""];')
                L.append(f'  {q(s.name)} -> {q(sid)} [{LBL}={q(lab)}, color="#000000", fontcolor="#333333"];')

    # --- legenda (tabella di testo: robusta anche con splines=ortho) ---
    if not opts.no_legend:
        L.append('  subgraph "cluster_legend" {')
        L.append('    label=<<B>Legenda</B>>; labeljust=l; fontname="Helvetica"; fontsize=10; '
                 'style="rounded"; color="#bbbbbb";')
        L.append('    "__legend" [shape=plaintext, style="", fontsize=10, label=<'
                 '<TABLE BORDER="0" CELLBORDER="0" CELLSPACING="2">'
                 '<TR><TD ALIGN="LEFT">&#9472;&#9472;&#9472;&#9472;&#9654;</TD><TD ALIGN="LEFT">messages</TD></TR>'
                 '<TR><TD ALIGN="LEFT">- - - &#9654;</TD><TD ALIGN="LEFT">Goto</TD></TR>'
                 '<TR><TD ALIGN="LEFT"><FONT POINT-SIZE="7">&#9472;&#9472;&#9472;&#9472;</FONT>&#9654;</TD>'
                 '<TD ALIGN="LEFT">handle error (linea sottile)</TD></TR>'
                 '</TABLE>>];')
        L.append("  }")

    L.append("}")
    return "\n".join(L) + "\n"


# --------------------------------------------------------------------------- #
#  MAIN
# --------------------------------------------------------------------------- #
def render(dot_path: Path, fmt: str) -> bool:
    out = dot_path.with_suffix("." + fmt)
    cmd = ["dot", f"-T{fmt}"]
    if fmt == "png":
        cmd.append("-Gdpi=160")
    cmd += [str(dot_path), "-o", str(out)]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"  ! errore Graphviz ({fmt}): {r.stderr.strip()}", file=sys.stderr)
        return False
    print(f"  -> {out}")
    return True


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Genera lo schema a stati dei QActor da un file QAK.")
    ap.add_argument("qak", help="file .qak di input")
    ap.add_argument("-o", "--outdir", help="cartella di output (default: <nome>_schemi)")
    ap.add_argument("-a", "--actor", action="append",
                    help="genera solo questo QActor (ripetibile). Default: tutti")
    ap.add_argument("--rankdir", default="TB", choices=["LR", "TB", "RL", "BT"],
                    help="direzione del layout (default: TB)")
    ap.add_argument("--format", default="png,svg",
                    help="formati di output separati da virgola: png,svg,pdf (default: png,svg)")
    ap.add_argument("--splines", default="ortho", choices=["ortho", "line", "polyline", "spline"],
                    help="tipo di linee: ortho = dritte ad angolo retto (default), line = segmenti diretti, "
                         "polyline = spezzate, spline = curve")
    ap.add_argument("--seed", type=int, default=None,
                    help="seme per i colori casuali delle fasi (stesso seme = stessi colori)")
    ap.add_argument("--no-colors", dest="colors", action="store_false",
                    help="stati bianchi, senza colori per fase (default: colorati per fase)")
    ap.add_argument("--keep-dot", action="store_true",
                    help="conserva anche i file .dot (default: vengono cancellati dopo il rendering)")
    ap.add_argument("--no-desc", action="store_true", help="non mostrare la descrizione sotto il nome dello stato")
    ap.add_argument("--no-stubs", action="store_true", help="non mostrare le frecce verso gli altri attori")
    ap.add_argument("--no-legend", action="store_true", help="non mostrare la legenda")
    ap.add_argument("--borders", action="store_true", help="disegna il bordo attorno a ogni fase (cluster)")
    opts = ap.parse_args(argv)

    qak_path = Path(opts.qak)
    if not qak_path.is_file():
        print(f"File non trovato: {qak_path}", file=sys.stderr)
        return 1
    text = qak_path.read_text(encoding="utf-8", errors="replace")
    actors = parse_qak(text)
    if not actors:
        print("Nessun QActor trovato nel file.", file=sys.stderr)
        return 1

    if opts.actor:
        wanted = {a.lower() for a in opts.actor}
        actors_sel = [a for a in actors if a.name.lower() in wanted]
        if not actors_sel:
            print("Attori disponibili: " + ", ".join(a.name for a in actors), file=sys.stderr)
            return 1
    else:
        actors_sel = actors

    # mappa messaggio-richiesta -> attori che lo inviano (serve per le frecce "Reply : X")
    senders: Dict[str, List[str]] = {}
    for a in actors:
        for s in a.states.values():
            for act in s.actions:
                if act.kind in ("request", "askFor"):
                    lst = senders.setdefault(act.msg, [])
                    if a.name not in lst:
                        lst.append(a.name)

    outdir = Path(opts.outdir) if opts.outdir else qak_path.with_name(qak_path.stem + "_schemi")
    outdir.mkdir(parents=True, exist_ok=True)
    formats = [f.strip().lower() for f in opts.format.split(",") if f.strip()]
    have_dot = shutil.which("dot") is not None
    if not have_dot:
        print("ATTENZIONE: Graphviz ('dot') non trovato nel PATH: genero solo i file .dot.\n"
              "  Installazione: https://graphviz.org/download/ (apt/brew/choco install graphviz)",
              file=sys.stderr)

    for a in actors_sel:
        n_tr = sum(len(s.transitions) + len(s.gotos) for s in a.states.values())
        print(f"[{a.name}] {len(a.states)} stati, {n_tr} transizioni")
        dot_path = outdir / f"{a.name}.dot"
        dot_path.write_text(build_dot(a, senders, opts), encoding="utf-8")
        if have_dot:
            for fmt in formats:
                render(dot_path, fmt)
            if not opts.keep_dot:
                dot_path.unlink()
            else:
                print(f"  -> {dot_path}")
        else:
            print(f"  -> {dot_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
