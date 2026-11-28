#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
qakSequenceDiag.py  -  Diagramma di sequenza (stile UML) delle interazioni dei QActor
di un modello qak.                                                  [Claude, Nov 2026]

Complementare a:
    qak2svg.py           architettura (contesti, attori, messaggi)       -> <modello>_arch.svg
    qakStateDiagram.py   automa di ogni QActor                            -> <modello>_states/
    qakSequenceDiag.py   una esecuzione possibile, nel tempo              -> <modello>_seq.svg

Uso:
    python qakSequenceDiag.py modello.qak                    -> modello_seq.svg
    python qakSequenceDiag.py modello.qak -o fig.svg
    python qakSequenceDiag.py src/*.qak                      (piu' modelli)
    python qakSequenceDiag.py modello.qak --steps 120        (limite di passi della simulazione)
    python qakSequenceDiag.py modello.qak --stimulus mapservice:evallogistic:"args(2.0,0.7,8)"
    python qakSequenceDiag.py modello.qak --else mapservice.checkparams   (ramo else di un if)
    python qakSequenceDiag.py modello.qak --no-states --trace

Solo libreria standard Python (>= 3.8). Nessuna dipendenza da Graphviz.

COME SI OTTIENE IL DIAGRAMMA
----------------------------
Un diagramma di sequenza mostra UNA esecuzione (uno scenario), non tutte. Il programma
la ottiene eseguendo in modo simbolico gli automi dei QActor del modello:

  * ogni QActor ha una coda di messaggi (mailbox); in uno stato con Transition preleva il
    PRIMO messaggio della coda che corrisponde a una transizione (whenMsg / whenRequest /
    whenReply / whenEvent / whenInterrupt*); gli altri restano in coda;
  * le azioni di uno stato (request, forward, emit, replyTo, autodispatch, delegate,
    create, updateResource, delay) sono eseguite in ordine; onMsg(M:...) vale solo se il
    messaggio corrente e' M; il codice Kotlin [# ... #] non e' interpretato;
  * le condizioni (if [# ... #], Goto ... if [# ... #] else ..., guardie 'and [# #]') non si
    possono valutare: si sceglie il ramo 'then' (o 'else' con --else ATTORE.STATO), e il
    diagramma lo segnala con una nota;
  * il tempo e' LOGICO: le azioni sono istantanee e una scadenza whenTime N scatta solo
    quando nessun messaggio puo' piu' essere consegnato (il sistema e' "veloce" rispetto
    ai timeout). 'delay N' compare come nota, senza effetto sull'ordine;
  * ordine di scelta quando nessun attore puo' proseguire:
        1. messaggi in coda consegnabili (il piu' vecchio per primo)
        2. stimoli esterni: richieste/dispatch gestiti da un attore ma inviati da nessun
           attore del modello (lifeline <<esterno>> 'client'), uno alla volta
        3. risposte di attori PASSIVI (ExternalQActor, CodedQActor, non dichiarati): la
           prima Reply dichiarata 'for' la richiesta e attesa da chi l'ha inviata
        4. eventi esterni attesi da un attore ma emessi da nessun attore (lifeline
           <<ambiente>>), preferendo quelli non di interrupt
        5. scadenze whenTime (la piu' vicina per prima)
    la simulazione termina quando non c'e' piu' nulla da fare o al limite --steps.

Notazione (UML 2, adattata a qak)
---------------------------------
  request   freccia continua, punta piena            (colori come in qak2svg.py)
  dispatch  freccia continua, punta aperta
  reply     freccia tratteggiata, punta aperta
  event     linea doppia ("binario") verso ogni attore che lo percepisce; se nessuno lo
            percepisce: messaggio perso (punta su un cerchio pieno)
  delegate  freccia a tratto-punto: il messaggio e' inoltrato all'attore delegato
  barra di attivazione: l'attore sta eseguendo (dal prelievo di un messaggio al blocco)
  [stato]   stato in cui entra l'attore (disattivabile con --no-states)
  clessidra scadenza whenTime;  t = ... ms   avanzamento del tempo logico
"""

import argparse
import glob
import html
import os
import re
import sys

# =====================================================================
# 1. PARSING
# =====================================================================

def strip_comments(text):
    """Rimuove // e /* */ rispettando stringhe "..." e codice [# ... #] (le // dentro il
    codice Kotlin sono comunque commenti Kotlin e possono essere tolte)."""
    out, i, n = [], 0, len(text)
    while i < n:
        c = text[i]
        if c == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == '\\' else 1
            out.append(text[i:j + 1]); i = j + 1; continue
        if c == "'":
            # atomo Prolog fra apici (es. 'http://host/x'): '//' al suo interno non e' un
            # commento. Vale solo se l'apice si chiude sulla stessa riga.
            j = text.find("'", i + 1)
            nl = text.find('\n', i + 1)
            if j > 0 and (nl < 0 or j < nl):
                out.append(text[i:j + 1]); i = j + 1; continue
        if text.startswith('//', i):
            j = text.find('\n', i); i = n if j < 0 else j; continue
        if text.startswith('/*', i):
            j = text.find('*/', i + 2); j = n if j < 0 else j + 2
            out.append('\n' * text[i:j].count('\n')); i = j; continue
        out.append(c); i += 1
    return ''.join(out)


def skip_str(t, i, q='"'):
    j = i + 1
    while j < len(t) and t[j] != q:
        j += 2 if t[j] == '\\' else 1
    return j + 1


def skip_kcode(t, i):
    j = t.find('#]', i + 2)
    return len(t) if j < 0 else j + 2


def match_brace(t, i):
    """Indice della '}' che chiude la '{' in posizione i (salta stringhe e [# ... #])."""
    depth, n = 0, len(t)
    while i < n:
        if t.startswith('[#', i):
            i = skip_kcode(t, i); continue
        c = t[i]
        if c == '"':
            i = skip_str(t, i); continue
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return n - 1


def scan_term(t, i):
    """Legge un termine qak (PHead) a partire da i: atomo, numero, stringa, variabile,
    $Var, struttura f(...). Restituisce (testo, fine)."""
    n = len(t)
    while i < n and t[i] in ' \t\r\n':
        i += 1
    s = i
    if i < n and t[i] == '"':
        e = skip_str(t, i)
        return t[s:e], e
    while i < n and (t[i].isalnum() or t[i] in '_$#@.-+'):
        i += 1
    if i < n and t[i] == '(':
        depth = 0
        while i < n:
            c = t[i]
            if c in '"\'':
                i = skip_str(t, i, c); continue
            if c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    i += 1; break
            i += 1
    return ' '.join(t[s:i].split()), i


class Act:
    """Azione di uno stato. kind: send | reply | emit | auto | delegate | create |
    update | delay | onmsg | if | rfi (returnFromInterrupt)"""
    def __init__(self, kind, **kw):
        self.kind = kind
        self.__dict__.update(kw)

    def __repr__(self):
        return 'Act(%s)' % ', '.join('%s=%r' % kv for kv in self.__dict__.items())


WORD = re.compile(r'[A-Za-z_]\w*')


def parse_block(t, i):
    """t[i] == '{' : restituisce (testo interno, indice dopo la '}')."""
    e = match_brace(t, i)
    return t[i + 1:e], e + 1


def skip_ws(t, i):
    while i < len(t) and t[i] in ' \t\r\n':
        i += 1
    return i


def parse_actions(t):
    acts, i, n = [], 0, len(t)
    while i < n:
        if t.startswith('[#', i):
            i = skip_kcode(t, i); continue
        c = t[i]
        if c == '"':
            i = skip_str(t, i); continue
        m = WORD.match(t, i)
        if not m:
            i += 1; continue
        w, j = m.group(0), m.end()
        rest = t[i:]
        if w == 'onMsg':
            r = re.match(r'onMsg\s*\(\s*(\w+)\s*:', rest)
            if r:
                _, k = scan_term(t, i + r.end())         # onMsg( M : termine )
                p = t.find(')', k)
                k = skip_ws(t, p + 1 if p >= 0 else k)
                body, els = '', ''
                if k < n and t[k] == '{':
                    body, k = parse_block(t, k)
                k2 = skip_ws(t, k)
                if t.startswith('else', k2):
                    k3 = skip_ws(t, k2 + 4)
                    if k3 < n and t[k3] == '{':
                        els, k = parse_block(t, k3)
                acts.append(Act('onmsg', msg=r.group(1), then=parse_actions(body), els=parse_actions(els)))
                i = k; continue
        elif w in ('if', 'ifSolved'):
            k = skip_ws(t, j)
            cond = ''
            if w == 'if' and t.startswith('[#', k):
                e = skip_kcode(t, k); cond = ' '.join(t[k + 2:e - 2].split()); k = skip_ws(t, e)
            if k < n and t[k] == '{':
                body, k = parse_block(t, k)
                els = ''
                k2 = skip_ws(t, k)
                if t.startswith('else', k2):
                    k3 = skip_ws(t, k2 + 4)
                    if k3 < n and t[k3] == '{':
                        els, k = parse_block(t, k3)
                acts.append(Act('if', cond=cond or 'ifSolved', then=parse_actions(body), els=parse_actions(els)))
                i = k; continue
        elif w in ('request', 'forward', 'requestsynch'):
            r = re.match(r'(request|forward|requestsynch)\s+(\w+)\s+-m\s+(\w+)\s*:', rest)
            if r:
                pay, k = scan_term(t, i + r.end())
                kind = 'dispatch' if w == 'forward' else 'request'
                acts.append(Act('send', mkind=kind, dest=r.group(2), msg=r.group(3), payload=pay))
                i = k; continue
        elif w == 'replyTo':
            r = re.match(r'replyTo\s+(\w+)\s+(?:ofsender\s+\$?\w+\s+)?with\s+(\w+)\s*:', rest)
            if r:
                pay, k = scan_term(t, i + r.end())
                acts.append(Act('reply', req=r.group(1), msg=r.group(2), payload=pay))
                i = k; continue
        elif w in ('emit', 'emitlocal', 'emitlocalstream'):
            r = re.match(r'\w+\s+(\w+)\s*:', rest)
            if r:
                pay, k = scan_term(t, i + r.end())
                acts.append(Act('emit', msg=r.group(1), payload=pay, mode=w))
                i = k; continue
        elif w == 'publish':
            r = re.match(r'publish\s+"([^"]*)"\s+-m\s+(\w+)\s*:', rest)
            if r:
                pay, k = scan_term(t, i + r.end())
                acts.append(Act('emit', msg=r.group(2), payload=pay, mode='publish'))
                i = k; continue
        elif w in ('autodispatch', 'autorequest'):
            r = re.match(r'\w+\s+(\w+)\s*:', rest)
            if r:
                pay, k = scan_term(t, i + r.end())
                acts.append(Act('auto', mkind='dispatch' if w == 'autodispatch' else 'request',
                                msg=r.group(1), payload=pay))
                i = k; continue
        elif w == 'delegate':
            r = re.match(r'delegate\s+(\w+)\s+to\s+(\w+)', rest)
            if r:
                acts.append(Act('delegate', msg=r.group(1), dest=r.group(2)))
                i += r.end(); continue
        elif w == 'delegateCurrentMsgTo':
            r = re.match(r'delegateCurrentMsgTo\s+(\w+)', rest)
            if r:
                acts.append(Act('delegatecur', dest=r.group(1)))
                i += r.end(); continue
        elif w == 'create':
            r = re.match(r'create\s+(\w+)', rest)
            if r:
                acts.append(Act('create', dest=r.group(1)))
                i += r.end(); continue
        elif w == 'updateResource':
            k = skip_ws(t, j)
            e = skip_kcode(t, k) if t.startswith('[#', k) else k
            acts.append(Act('update', val=' '.join(t[k + 2:e - 2].split()) if e > k else ''))
            i = e; continue
        elif w in ('delay', 'delayVar', 'delayVarRef'):
            r = re.match(r'\w+\s+\$?(\w+)', rest)
            if r:
                acts.append(Act('delay', val=r.group(1)))
                i += r.end(); continue
        elif w == 'returnFromInterrupt':
            acts.append(Act('rfi')); i = j; continue
        i = j
    return acts


class Trans:
    def __init__(self):
        self.goto = None          # (target, cond, else_target)
        self.timeout = None       # (ms or None, label, target)
        self.triggers = []        # (wkind, msg, guard, target)
        self.elsegoto = None


TRIG_KIND = {'whenMsg': 'dispatch', 'whenInterrupt': 'dispatch', 'whenRequest': 'request',
             'whenReply': 'reply', 'whenEvent': 'event', 'whenInterruptEvent': 'event'}


def parse_tail(tail):
    tr = Trans()
    g = re.match(r'\s*Goto\s+(\w+)(?:\s+if\s+\[#(.*?)#\]\s+else\s+(\w+))?', tail, re.S)
    if g:
        tr.goto = (g.group(1), ' '.join((g.group(2) or '').split()), g.group(3))
        return tr
    if not re.match(r'\s*Transition\b', tail):
        return None
    r = re.search(r'\bwhenTime\s+(\d+)\s*->\s*(\w+)', tail)
    if r:
        tr.timeout = (int(r.group(1)), r.group(1) + ' ms', r.group(2))
    r = re.search(r'\bwhenTime(Var|VarRef|Sol)\s+[$@]?(\w+)\s*->\s*(\w+)', tail)
    if r:
        tr.timeout = (None, r.group(2), r.group(3))
    for r in re.finditer(r'\b(whenMsg|whenRequest|whenReply|whenEvent|whenInterruptEvent|whenInterrupt)\s+(\w+)'
                         r'(?:\s+and\s+\[#(.*?)#\])?\s*->\s*(\w+)', tail, re.S):
        tr.triggers.append((r.group(1), r.group(2), ' '.join((r.group(3) or '').split()), r.group(4)))
    r = re.search(r'\belse\s+Goto\s+(\w+)', tail)
    if r:
        tr.elsegoto = r.group(1)
    return tr


class State:
    def __init__(self, name, initial):
        self.name, self.initial = name, initial
        self.actions, self.trans = [], None


class Actor:
    def __init__(self, name, kind, ctx):
        self.name, self.kind, self.ctx = name, kind, ctx
        self.dynamic = self.hidden = False
        self.states, self.initial = {}, None

    @property
    def active(self):          # ha un automa da eseguire
        return self.kind == 'QActor' and bool(self.states)


class Model:
    def __init__(self):
        self.name = '?'
        self.messages = {}     # nome -> (kind, sig, for)
        self.contexts = {}     # nome -> (host, port)
        self.ctx_order = []
        self.actors = {}
        self.order = []


def parse(text):
    m = Model()
    code = strip_comments(text)
    r = re.search(r'\bSystem\s+(?:-\w+\s+)*(\w+)', code)
    if r: m.name = r.group(1)
    starts = [x.start() for x in re.finditer(r'^\s*(QActor|ExternalQActor|CodedQActor)\b', code, re.M)]
    head = code[:starts[0]] if starts else code
    for r in re.finditer(r'\b(Event|Dispatch|Request|Reply|Signal|Token|Invitation|RequestSynch|ReplyToSynch)\s+(\w+)\s*:\s*'
                         r'(\w+\s*(?:\([^\n]*?\))?)(?:\s+for\s+(\w+))?', head):
        kind, name, sig, forr = r.groups()
        if name not in m.messages or kind == 'Reply':
            m.messages[name] = (kind, ' '.join(sig.split()), forr)
    for r in re.finditer(r'\bContext\s+(\w+)\s+ip\s*\[\s*host\s*=\s*"([^"]*)"\s+port\s*=\s*(\d+)\s*\]', head):
        m.contexts[r.group(1)] = (r.group(2), r.group(3)); m.ctx_order.append(r.group(1))
    bounds = starts + [len(code)]
    for k in range(len(starts)):
        chunk = code[bounds[k]:bounds[k + 1]]
        r = re.match(r'\s*(QActor|ExternalQActor|CodedQActor)\s+(\w+)\s+context\s+(\w+)', chunk)
        if not r:
            continue
        a = Actor(r.group(2), r.group(1), r.group(3))
        if a.kind == 'QActor':
            b = chunk.find('{')
            header = chunk[r.end():b if b >= 0 else len(chunk)]
            a.dynamic = 'dynamicOnly' in header
            a.hidden = bool(re.search(r'\bhidden\b', header))
            if b >= 0:
                body, _ = parse_block(chunk, b)
                parse_states(a, body)
        m.actors[a.name] = a
        m.order.append(a.name)
    return m


def parse_states(a, body):
    found, pos = [], 0
    while True:
        s = re.compile(r'\bState\s+(\w+)(\s+initial)?\s*\{').search(body, pos)
        if not s:
            break
        # salta occorrenze dentro [# ... #]
        kc = [(x.start(), x.end()) for x in re.finditer(r'\[#.*?#\]', body[:s.start()] + ' ', re.S)]
        if any(st < s.start() < en for st, en in kc):
            pos = s.end(); continue
        close = match_brace(body, s.end() - 1)
        found.append((s, close)); pos = close + 1
    for k, (s, close) in enumerate(found):
        st = State(s.group(1), bool(s.group(2)))
        st.actions = parse_actions(body[s.end():close])
        tail_end = found[k + 1][0].start() if k + 1 < len(found) else len(body)
        st.trans = parse_tail(body[close + 1:tail_end])
        a.states[st.name] = st
        if st.initial and not a.initial:
            a.initial = st.name
    if not a.initial and a.states:
        a.initial = next(iter(a.states))


# =====================================================================
# 2. SIMULAZIONE
# =====================================================================

CLIENT, ENV = '__client', '__env'


class Msg:
    seq = 0

    def __init__(self, mid, kind, sender, dest, payload, origin=None):
        Msg.seq += 1
        self.seq = Msg.seq
        self.id, self.kind, self.sender, self.dest, self.payload = mid, kind, sender, dest, payload
        self.origin = origin or sender      # mittente originale (per le delegazioni)


class RT:
    def __init__(self, actor):
        self.a = actor
        self.state = None
        self.queue, self.pending, self.delegations = [], {}, {}
        self.cur = None
        self.waiting = self.halted = self.started = False
        self.deadline = None
        self.act_open = None
        self.interrupted = None      # stato a cui tornare con returnFromInterrupt
        self.rfi = False


class Stop(Exception):
    pass


class Sim:
    def __init__(self, m, opts):
        self.m, self.o = m, opts
        self.rows = []
        self.acts = {}               # attore -> [(i0, i1)]
        self.clock = 0
        self.steps = 0
        self.notes = []
        self.lifelines = []          # in ordine di apparizione/dichiarazione
        self.rts = {n: RT(a) for n, a in m.actors.items()}
        self.used = set()            # messaggi scambiati
        self.perceivers = {}         # evento -> attori che lo percepiscono
        for a in m.actors.values():
            for st in a.states.values():
                for wk, msg, _, _ in (st.trans.triggers if st.trans else []):
                    if TRIG_KIND[wk] == 'event':
                        self.perceivers.setdefault(msg, [])
                        if a.name not in self.perceivers[msg]:
                            self.perceivers[msg].append(a.name)
        self.stimuli = self.find_stimuli()
        self.ext_count = {}          # evento esterno -> volte in cui e' stato simulato
        self.loops = {}              # (attore, stato) -> ripetizioni del ramo then di un Goto if

    # ---------- utilita'
    def lane(self, name):
        if name not in self.lifelines:
            self.lifelines.append(name)

    def row(self, **r):
        for k in ('src', 'dst', 'actor'):
            if r.get(k): self.lane(r[k])
        self.rows.append(r)

    def step(self):
        self.steps += 1
        if self.steps > self.o.steps:
            raise Stop()

    def choose_else(self, actor, state):
        return ('%s.%s' % (actor, state)) in (self.o.else_ or [])

    def open_act(self, rt):
        if rt.act_open is None:
            rt.act_open = len(self.rows)

    def close_act(self, rt):
        if rt.act_open is not None:
            if len(self.rows) > rt.act_open:
                self.acts.setdefault(rt.a.name, []).append((rt.act_open, len(self.rows) - 1))
            rt.act_open = None

    # ---------- stimoli esterni (richieste/dispatch che nessun attore del modello invia)
    def find_stimuli(self):
        if self.o.no_stimuli:
            return []
        if self.o.stimulus:
            out = []
            for s in self.o.stimulus:
                p = s.split(':', 2)
                dest, mid = p[0], p[1] if len(p) > 1 else ''
                md = self.m.messages.get(mid, ('Request', mid, None))
                out.append((dest, mid, 'request' if md[0] == 'Request' else 'dispatch',
                            p[2] if len(p) > 2 else md[1]))
            return out
        sent = set()
        def walk(acts):
            for x in acts:
                if x.kind in ('send', 'auto'): sent.add(x.msg)
                if x.kind in ('onmsg', 'if'): walk(x.then); walk(x.els)
        for a in self.m.actors.values():
            for st in a.states.values():
                walk(st.actions)
        out = []
        for n in self.m.order:
            a = self.m.actors[n]
            for st in a.states.values():
                for wk, msg, _, _ in (st.trans.triggers if st.trans else []):
                    k = TRIG_KIND[wk]
                    if k in ('request', 'dispatch') and msg not in sent and \
                            not any(o[1] == msg for o in out) and not self.delegated_internally(msg):
                        md = self.m.messages.get(msg, ('', msg, None))
                        dest = self.front_of(a.name, msg)
                        out.append((dest, msg, k, md[1]))
        return out

    def delegated_internally(self, msg):
        return False

    def front_of(self, actor, msg):
        """Se un altro attore delega msg ad 'actor', il client lo invia al delegante."""
        def walk(acts):
            for x in acts:
                if x.kind == 'delegate' and x.msg == msg and x.dest == actor: return True
                if x.kind in ('onmsg', 'if') and (walk(x.then) or walk(x.els)): return True
            return False
        for a in self.m.actors.values():
            if any(walk(st.actions) for st in a.states.values()):
                return a.name
        return actor

    # ---------- invio
    def send(self, src, dst, kind, mid, payload, origin=None):
        self.step()
        self.used.add(mid)
        rt = self.rts.get(dst)
        if rt and mid in rt.delegations and kind in ('request', 'dispatch'):
            target = rt.delegations[mid]
            self.row(type='msg', src=src, dst=dst, kind=kind, mid=mid, payload=payload)
            self.row(type='msg', src=dst, dst=target, kind='delegate', mid=mid, payload='delegate')
            self.deliver(Msg(mid, kind, dst, target, payload, origin=origin or src))
            return
        self.row(type='msg', src=src, dst=dst, kind=kind, mid=mid, payload=payload)
        self.deliver(Msg(mid, kind, src, dst, payload, origin=origin or src))

    def deliver(self, msg):
        rt = self.rts.get(msg.dest)
        if rt and rt.a.active and rt.started:
            rt.queue.append(msg)

    def emit(self, src, mid, payload):
        self.step()
        self.used.add(mid)
        recv = [r for r in self.perceivers.get(mid, []) if r != src and self.rts[r].started]
        if not recv:
            self.row(type='lost', src=src, kind='event', mid=mid, payload=payload)
            return
        for r in recv:
            self.row(type='msg', src=src, dst=r, kind='event', mid=mid, payload=payload)
            self.rts[r].queue.append(Msg(mid, 'event', src, r, payload))

    # ---------- esecuzione di uno stato
    def exec_actions(self, rt, acts):
        a = rt.a.name
        for x in acts:
            if x.kind == 'onmsg':
                ok = rt.cur is not None and rt.cur.id == x.msg
                self.exec_actions(rt, x.then if ok else x.els)
            elif x.kind == 'if':
                e = self.choose_else(a, rt.state)
                if x.els or e:
                    self.row(type='note', actor=a, text='if [%s] \u2192 %s' % (trunc(x.cond, 40), 'else' if e else 'then'))
                self.exec_actions(rt, x.els if e else x.then)
            elif x.kind == 'send':
                dst = a if x.dest == 'myself' else x.dest
                if dst not in self.m.actors:
                    self.rts.setdefault(dst, None)
                self.send(a, dst, x.mkind, x.msg, x.payload)
                if x.mkind == 'request':
                    pass
            elif x.kind == 'auto':
                self.send(a, a, x.mkind, x.msg, x.payload)
            elif x.kind == 'reply':
                self.reply(rt, x)
            elif x.kind == 'emit':
                self.emit(a, x.msg, x.payload)
            elif x.kind == 'delegate':
                rt.delegations[x.msg] = x.dest
                self.lane(x.dest)
            elif x.kind == 'delegatecur':
                if rt.cur:
                    self.step()
                    self.row(type='msg', src=a, dst=x.dest, kind='delegate', mid=rt.cur.id, payload='delegate')
                    self.deliver(Msg(rt.cur.id, rt.cur.kind, a, x.dest, rt.cur.payload, origin=rt.cur.origin))
            elif x.kind == 'create':
                self.step()
                self.row(type='create', src=a, dst=x.dest)
                t = self.rts.get(x.dest)
                if t and t.a.active and not t.started:
                    self.start(t)
            elif x.kind == 'update':
                self.row(type='note', actor=a, text='updateResource ' + trunc(x.val, 34), small=True)
            elif x.kind == 'delay':
                self.row(type='note', actor=a, text='delay ' + x.val + (' ms' if x.val.isdigit() else ''), small=True)
            elif x.kind == 'rfi':
                rt.rfi = True

    def reply(self, rt, x):
        a = rt.a.name
        req = None
        if rt.cur is not None and rt.cur.kind == 'request' and rt.cur.id == x.req:
            req = rt.cur
            if req in rt.pending.get(x.req, []):
                rt.pending[x.req].remove(req)
        elif rt.pending.get(x.req):
            req = rt.pending[x.req].pop()
        if req is None:
            self.row(type='note', actor=a, text='replyTo %s: nessuna richiesta pendente' % x.req)
            return
        self.step()
        self.used.add(x.msg)
        dst = req.origin
        self.row(type='msg', src=a, dst=dst, kind='reply', mid=x.msg, payload=x.payload)
        if dst in self.rts and self.rts[dst] and self.rts[dst].a.active:
            self.rts[dst].queue.append(Msg(x.msg, 'reply', a, dst, x.payload))

    def enter(self, rt, target, show=True):
        if target not in rt.a.states:
            self.row(type='note', actor=rt.a.name, text='stato %s non definito' % target)
            rt.halted = True
            return False
        rt.state = target
        if show and not self.o.no_states:
            self.row(type='state', actor=rt.a.name, text=target)
        return True

    def match(self, rt):
        tr = rt.a.states[rt.state].trans
        if not tr or tr.goto:
            return None
        for msg in rt.queue:
            for wk, mid, guard, target in tr.triggers:
                if mid == msg.id and TRIG_KIND[wk] == msg.kind:
                    return msg, wk, guard, target
        return None

    def run(self, rt, skip_actions=False):
        """Esegue l'attore finche' non si blocca (Transition senza messaggi) o termina."""
        self.open_act(rt)
        guard = 0
        while True:
            guard += 1
            if guard > 200:
                self.row(type='note', actor=rt.a.name, text='ciclo di Goto interrotto')
                rt.halted = True; break
            st = rt.a.states[rt.state]
            rt.rfi = False
            if not skip_actions:
                self.exec_actions(rt, st.actions)
            skip_actions = False
            if getattr(rt, 'rfi', False) and rt.interrupted:
                back = rt.interrupted; rt.interrupted = None
                self.row(type='note', actor=rt.a.name, text='returnFromInterrupt \u2192 ' + back)
                rt.state = back
                skip_actions = True
                continue
            tr = st.trans
            if tr is None:
                rt.halted = True; break
            if tr.goto:
                tgt, cond, els = tr.goto
                if cond:
                    e = self.choose_else(rt.a.name, st.name)
                    key = (rt.a.name, st.name)
                    why = ''
                    if not e:
                        self.loops[key] = self.loops.get(key, 0) + 1
                        if self.loops[key] > self.o.loop:      # ciclo: dopo N giri si esce
                            e, why = True, '  (dopo %d ripetizioni, --loop)' % self.o.loop
                            self.loops[key] = 0
                    self.row(type='note', actor=rt.a.name,
                             text='[%s] \u2192 %s' % (trunc(cond, 34), (els if e else tgt) +
                                                       (' (else)' if e else '') + why))
                    tgt = els if e else tgt
                if not self.enter(rt, tgt):
                    break
                continue
            mt = self.match(rt)
            if mt:
                self.consume(rt, *mt)
                continue
            if tr.elsegoto:
                if not self.enter(rt, tr.elsegoto):
                    break
                continue
            rt.waiting = True
            rt.deadline = (self.clock + (tr.timeout[0] if tr.timeout[0] is not None else 1000)) if tr.timeout else None
            break
        self.close_act(rt)

    def consume(self, rt, msg, wk, guard, target):
        rt.queue.remove(msg)
        rt.cur = msg
        rt.waiting = False
        if msg.kind == 'request':
            rt.pending.setdefault(msg.id, []).append(msg)
        if wk.startswith('whenInterrupt'):
            rt.interrupted = rt.state
        if guard:
            self.row(type='note', actor=rt.a.name, text='and [%s]' % trunc(guard, 34), small=True)
        self.enter(rt, target)

    def start(self, rt):
        rt.started = True
        self.lane(rt.a.name)
        self.open_act(rt)
        rt.state = rt.a.initial
        if not self.o.no_states:
            self.row(type='state', actor=rt.a.name, text=rt.state)
        self.run(rt)

    # ---------- passi del sistema
    def passive_reply(self):
        """Un attore passivo (ExternalQActor, CodedQActor, non dichiarato) risponde a una
        richiesta attesa da un attore bloccato."""
        for n in self.lifelines_order():
            rt = self.rts.get(n)
            if not rt or not rt.waiting:
                continue
            tr = rt.a.states[rt.state].trans
            for wk, mid, _, _ in tr.triggers:
                if TRIG_KIND[wk] != 'reply':
                    continue
                req = (self.m.messages.get(mid) or (None, None, None))[2]
                for r in reversed(self.rows):
                    if r['type'] == 'msg' and r['kind'] == 'request' and r['src'] == n and r['mid'] == req:
                        d = r['dst']
                        if d in self.m.actors and self.m.actors[d].active:
                            break                    # risponde (se lo fa) l'attore stesso
                        if r.get('answered'):
                            break
                        r['answered'] = True
                        self.step()
                        self.used.add(mid)
                        sig = (self.m.messages.get(mid) or ('', mid, None))[1]
                        self.row(type='msg', src=d, dst=n, kind='reply', mid=mid, payload=sig, passive=True)
                        rt.queue.append(Msg(mid, 'reply', d, n, sig))
                        return True
        return False

    def external_event(self):
        emitted = set()
        def walk(acts):
            for x in acts:
                if x.kind == 'emit': emitted.add(x.msg)
                if x.kind in ('onmsg', 'if'): walk(x.then); walk(x.els)
        for a in self.m.actors.values():
            for st in a.states.values(): walk(st.actions)
        for n in self.lifelines_order():
            rt = self.rts.get(n)
            if not rt or not rt.waiting:
                continue
            tr = rt.a.states[rt.state].trans
            cands = [t for t in tr.triggers if TRIG_KIND[t[0]] == 'event' and t[1] not in emitted
                     and self.ext_count.get(t[1], 0) < self.o.ext_events]
            cands.sort(key=lambda t: t[0] == 'whenInterruptEvent')
            if cands:
                mid = cands[0][1]
                sig = (self.m.messages.get(mid) or ('', mid, None))[1]
                self.ext_count[mid] = self.ext_count.get(mid, 0) + 1
                self.step()
                self.used.add(mid)
                for r in self.perceivers.get(mid, []):
                    if self.rts[r].started:
                        self.row(type='msg', src=ENV, dst=r, kind='event', mid=mid, payload=sig)
                        self.rts[r].queue.append(Msg(mid, 'event', ENV, r, sig))
                return True
        return False

    def lifelines_order(self):
        return [n for n in self.m.order]

    def simulate(self):
        try:
            if self.stimuli:
                self.lane(CLIENT)
            # tutti gli attori (non dinamici) esistono dall'avvio dei contesti: prima si
            # creano le mailbox, poi si eseguono gli stati iniziali nell'ordine del modello
            for n in self.m.order:
                rt = self.rts[n]
                if rt.a.active and not rt.a.dynamic:
                    rt.started = True
            for n in self.m.order:
                rt = self.rts[n]
                if rt.a.active and not rt.a.dynamic:
                    self.start(rt)
            while True:
                cands = []
                for rt in self.rts.values():
                    if rt and rt.waiting:
                        mt = self.match(rt)
                        if mt:
                            cands.append((mt[0].seq, rt, mt))
                if cands:
                    _, rt, mt = min(cands, key=lambda c: c[0])
                    self.open_act(rt)
                    self.consume(rt, *mt)
                    self.run(rt)
                    continue
                if self.stimuli:
                    dest, mid, kind, pay = self.stimuli.pop(0)
                    self.send(CLIENT, dest, kind, mid, pay)
                    continue
                if self.passive_reply():
                    continue
                if self.external_event():
                    continue
                timers = [rt for rt in self.rts.values() if rt and rt.waiting and rt.deadline is not None]
                if timers:
                    rt = min(timers, key=lambda r: r.deadline)
                    tr = rt.a.states[rt.state].trans
                    if rt.deadline > self.clock:
                        self.clock = rt.deadline
                        self.row(type='time', t=self.clock)
                    self.step()
                    rt.waiting = False
                    rt.cur = None
                    self.open_act(rt)
                    self.row(type='timeout', actor=rt.a.name, text='whenTime %s \u2192 %s' % (tr.timeout[1], tr.timeout[2]))
                    if self.enter(rt, tr.timeout[2]):
                        self.run(rt)
                    else:
                        self.close_act(rt)
                    continue
                break
            self.end_reason = 'nessun altro evento possibile: ' + self.quiescence()
        except Stop:
            self.end_reason = 'interrotta al limite di %d passi (--steps)' % self.o.steps
            for rt in self.rts.values():
                if rt: self.close_act(rt)
        self.row(type='end', text=self.end_reason)

    def quiescence(self):
        w = ['%s in %s' % (rt.a.name, rt.state) for rt in self.rts.values()
             if rt and rt.started and rt.waiting]
        h = ['%s terminato in %s' % (rt.a.name, rt.state) for rt in self.rts.values()
             if rt and rt.started and rt.halted]
        return '; '.join(filter(None, ['in attesa: ' + ', '.join(w) if w else '', ', '.join(h)])) or 'sistema fermo'


# =====================================================================
# 3. DISEGNO SVG
# =====================================================================

MSGCOLOR = {'dispatch': '#2e7d32', 'request': '#1565c0', 'reply': '#00838f',
            'event': '#e65100', 'delegate': '#8d6e00', 'create': '#555555'}
FILL = {'QActor': ('#e3f0fc', '#1f3b5c'), 'CodedQActor': ('#efe6fa', '#3e2a5c'),
        'ExternalQActor': ('#eeeeee', '#555555'), 'unknown': ('#ffffff', '#999999'),
        'client': ('#ffffff', '#333333')}
ROWH = {'msg': 34, 'lost': 34, 'create': 34, 'state': 21, 'note': 21, 'timeout': 26, 'time': 24, 'end': 30}


def esc(s):
    return html.escape(str(s), quote=True)


def tw(s, fs):
    return len(s) * fs * 0.56


def trunc(s, n):
    s = str(s)
    return s if len(s) <= n else s[:max(1, n - 1)] + '\u2026'


def tint(color, f=0.88):
    c = color.lstrip('#')
    r, g, b = (int(c[i:i + 2], 16) for i in (0, 2, 4))
    return '#%02x%02x%02x' % tuple(round(v + (255 - v) * f) for v in (r, g, b))


def render(m, sim, opts):
    names = list(sim.lifelines)
    # ordine: client, poi per contesto (ordine di dichiarazione), poi ambiente
    ctx_rank = {c: i for i, c in enumerate(m.ctx_order)}
    def key(n):
        if n == CLIENT: return (-1, 0)
        if n == ENV: return (10 ** 6, 0)
        a = m.actors.get(n)
        if not a: return (10 ** 5, 0)
        return (ctx_rank.get(a.ctx, 10 ** 4), m.order.index(n))
    names.sort(key=key)

    # --- colonne
    colw = {}
    for n in names:
        a = m.actors.get(n)
        label = 'client' if n == CLIENT else ('ambiente' if n == ENV else n)
        colw[n] = max(130, tw(label, 13) + 40, tw('\u00abExternalQActor\u00bb', 9.5) + 26 if a else 0)
    # un contesto deve contenere la propria etichetta: si allarga la sua ultima colonna
    i = 0
    while i < len(names):
        a = m.actors.get(names[i])
        c = a.ctx if a else None
        j = i
        while j + 1 < len(names) and m.actors.get(names[j + 1]) and m.actors[names[j + 1]].ctx == c:
            j += 1
        if c is not None:
            hp = m.contexts.get(c)
            need = tw('Context %s%s' % (c, '  %s:%s' % hp if hp else ''), 10.5) + 20
            have = sum(colw[n] for n in names[i:j + 1]) + 40 * (j - i)
            if need > have:
                colw[names[j]] += need - have
        i = j + 1
    X, x = {}, 80
    prev = None
    for n in names:
        a = m.actors.get(n)
        c = a.ctx if a else None
        if prev is not None and c != prev[0] and (c is not None or prev[0] is not None):
            x += 24                                 # spazio fra contesti diversi
        X[n] = x + colw[n] / 2
        x += colw[n] + 40
        prev = (c,)
    W = max(x + 220, 760)

    S_back, S_mid, S_front = [], [], []
    defs = set()

    def marker(color, kind):
        mid = '%s%s' % (kind, color.strip('#'))
        if mid not in defs:
            defs.add(mid)
            if kind == 'f':      # punta piena
                d = '<marker id="%s" viewBox="0 0 10 10" refX="9.5" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="%s"/></marker>' % (mid, color)
            else:                # punta aperta
                d = '<marker id="%s" viewBox="0 0 10 10" refX="9.5" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10" fill="none" stroke="%s" stroke-width="1.5"/></marker>' % (mid, color)
            DEFS.append(d)
        return mid
    DEFS = []

    def text(L, x, y, s, fs=11, anchor='start', weight='normal', fill='#111', italic=False, mono=False):
        L.append('<text x="%.1f" y="%.1f" font-size="%s" text-anchor="%s" font-weight="%s" fill="%s"%s%s>%s</text>'
                 % (x, y, fs, anchor, weight, fill, ' font-style="italic"' if italic else '',
                    ' font-family="Courier New, monospace"' if mono else '', esc(s)))

    # --- intestazioni (con raggruppamento per contesto)
    top = 64
    HH = 48
    groups = []
    for n in names:
        a = m.actors.get(n)
        c = a.ctx if a else None
        if groups and groups[-1][0] == c:
            groups[-1][1].append(n)
        else:
            groups.append((c, [n]))
    for c, ns in groups:
        if c is None:
            continue
        x0 = X[ns[0]] - colw[ns[0]] / 2 - 12
        x1 = X[ns[-1]] + colw[ns[-1]] / 2 + 12
        ext = all(m.actors[n].kind == 'ExternalQActor' for n in ns)
        S_back.append('<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" rx="8" fill="%s" stroke="%s"%s/>'
                      % (x0, top - 22, x1 - x0, HH + 32, '#ffffff' if ext else '#fbfbfb', '#555' if ext else '#222',
                         ' stroke-dasharray="6 4"' if ext else ''))
        hp = m.contexts.get(c)
        text(S_back, x0 + 8, top - 8, 'Context %s%s' % (c, '  %s:%s' % hp if hp else ''), 10.5, weight='bold', fill='#333')
    head_bottom = top + HH + 18
    for n in names:
        a = m.actors.get(n)
        cx, w = X[n], colw[n] - 16
        if n in (CLIENT, ENV):
            # attore UML (omino)
            hx, hy = cx, top + 6
            S_front.append('<circle cx="%.1f" cy="%.1f" r="6" fill="#fff" stroke="#333" stroke-width="1.3"/>' % (hx, hy + 4))
            S_front.append('<path d="M%.1f,%.1f v14 M%.1f,%.1f h18 M%.1f,%.1f l-8,10 M%.1f,%.1f l8,10" stroke="#333" stroke-width="1.3" fill="none"/>'
                           % (hx, hy + 10, hx - 9, hy + 15, hx, hy + 24, hx, hy + 24))
            text(S_front, cx, top + HH + 4, '\u00abesterno\u00bb client' if n == CLIENT else '\u00abambiente\u00bb', 10.5, 'middle', 'bold', '#333')
            continue
        kind = a.kind if a else 'unknown'
        fill, stroke = FILL.get(kind, FILL['unknown'])
        dash = ' stroke-dasharray="6 4"' if kind in ('ExternalQActor', 'unknown') else (' stroke-dasharray="3 3"' if a and a.hidden else '')
        S_front.append('<rect x="%.1f" y="%.1f" width="%.1f" height="%s" rx="8" fill="%s" stroke="%s" stroke-width="1.5"%s/>'
                       % (cx - w / 2, top, w, HH, fill, stroke, dash))
        st = {'QActor': '\u00abQActor\u00bb', 'CodedQActor': '\u00abCodedQActor\u00bb',
              'ExternalQActor': '\u00abExternalQActor\u00bb'}.get(kind, '\u00abnon dichiarato\u00bb')
        if a and a.hidden: st = st[:-1] + ' hidden\u00bb'
        if a and a.dynamic: st = st[:-1] + ' dyn\u00bb'
        text(S_front, cx, top + 17, st, 9.5, 'middle', fill='#555')
        text(S_front, cx, top + 36, n, 13, 'middle', 'bold')

    # --- righe
    ys, y = [], head_bottom + 14
    for r in sim.rows:
        h = ROWH.get(r['type'], 24)
        if r['type'] == 'msg' and r['src'] == r['dst']:
            h = 42
        if r['type'] == 'note' and r.get('small'):
            h = 19
        ys.append((y, h))
        y += h
    bottom = y + 16
    if sim.rows and sim.rows[-1]['type'] == 'end':        # le lifeline finiscono prima della nota finale
        bottom = ys[-1][0] + 2

    # lifeline tratteggiate
    for n in names:
        S_back.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="#888" stroke-width="1" stroke-dasharray="5 4"/>'
                      % (X[n], head_bottom - 18 + (0 if n not in (CLIENT, ENV) else 8), X[n], bottom))

    # barre di attivazione
    active_at = {}
    for n, ivs in sim.acts.items():
        if n not in X:
            continue
        for i0, i1 in ivs:
            y0 = ys[i0][0] + 2
            y1 = ys[i1][0] + ys[i1][1] - 2
            S_mid.append('<rect x="%.1f" y="%.1f" width="10" height="%.1f" fill="%s" stroke="%s" stroke-width="1"/>'
                         % (X[n] - 5, y0, max(8, y1 - y0), FILL['QActor'][0], FILL['QActor'][1]))
            for i in range(i0, i1 + 1):
                active_at[(n, i)] = True

    def lx(n, i, toward):
        """x di partenza/arrivo su una lifeline (bordo della barra se attiva)."""
        if active_at.get((n, i)):
            return X[n] + (5 if toward > X[n] else -5)
        return X[n]

    for i, r in enumerate(sim.rows):
        y0, h = ys[i]
        t = r['type']
        yl = y0 + h - 9
        if t in ('msg', 'lost'):
            kind = r['kind']
            col = MSGCOLOR.get(kind, '#333')
            label = r['mid'] + ('' if r['payload'] in ('', 'delegate') else ' : ' + r['payload'])
            if kind == 'delegate':
                label = 'delegate ' + r['mid']
            if r.get('passive'):
                label += '  (presunta)'
            if t == 'lost':
                x1 = lx(r['src'], i, X[r['src']] + 1)
                x2 = X[names[-1]] + colw[names[-1]] / 2 + 60 if names else x1 + 120
                x2 = max(x2, x1 + 120)
                draw_arrow(S_mid, x1, yl, x2 - 6, yl, kind, col, marker)
                S_mid.append('<circle cx="%.1f" cy="%.1f" r="5" fill="%s"/>' % (x2, yl, col))
                draw_label(S_front, text, (x1 + x2) / 2, yl - 6, label + '   (nessun ricevente)', col, x1, x2)
                continue
            s, d = r['src'], r['dst']
            if s == d:
                x0 = lx(s, i, X[s] + 1)
                pts = [(x0, y0 + 10), (x0 + 36, y0 + 10), (x0 + 36, y0 + 30), (x0 + 1, y0 + 30)]
                draw_poly(S_mid, pts, kind, col, marker)
                text(S_front, x0 + 42, y0 + 22, trunc(label, 70), 10, fill=col)
                continue
            x1 = lx(s, i, X[d])
            x2 = lx(d, i, X[s])
            draw_arrow(S_mid, x1, yl, x2, yl, kind, col, marker)
            draw_label(S_front, text, (x1 + x2) / 2, yl - 6, label, col, x1, x2)
        elif t == 'create':
            col = MSGCOLOR['create']
            x1, x2 = lx(r['src'], i, X[r['dst']]), X[r['dst']]
            draw_arrow(S_mid, x1, yl, x2, yl, 'dispatch', col, marker, dash='6 4')
            draw_label(S_front, text, (x1 + x2) / 2, yl - 6, '\u00abcreate\u00bb', col, x1, x2)
        elif t == 'state':
            n = r['actor']
            if n not in X: continue
            w = tw(r['text'], 9.5) + 14
            S_front.append('<rect x="%.1f" y="%.1f" width="%.1f" height="15" rx="7" fill="#fff" stroke="#777" stroke-width="0.9"/>'
                           % (X[n] + 9, y0 + 3, w))
            text(S_front, X[n] + 9 + w / 2, y0 + 14, r['text'], 9.5, 'middle', fill='#444', italic=True)
        elif t == 'note':
            n = r['actor']
            if n not in X: continue
            s = trunc(r['text'], 60)
            w = tw(s, 9.5) + 16
            xn = X[n] + 9
            S_front.append('<path d="M%.1f,%.1f h%.1f l6,6 v%.1f h%.1f z" fill="#fffbe6" stroke="#b0a060" stroke-width="0.8"/>'
                           % (xn, y0 + 2, w - 6, (h - 6) - 6, -w))
            text(S_front, xn + 6, y0 + h - 8, s, 9.5, fill='#5a4a00')
        elif t == 'timeout':
            n = r['actor']
            if n not in X: continue
            hx, hy = X[n] - 22, y0 + 4
            S_front.append('<path d="M%.1f,%.1f h12 l-12,16 h12 z" fill="#fff" stroke="#a3341f" stroke-width="1.2"/>' % (hx, hy))
            text(S_front, X[n] + 9, y0 + 17, r['text'], 10, fill='#a3341f', weight='bold')
        elif t == 'time':
            S_back.append('<line x1="20" y1="%.1f" x2="%.1f" y2="%.1f" stroke="#a3341f" stroke-width="0.8" stroke-dasharray="2 3"/>'
                          % (y0 + h / 2, W - 20, y0 + h / 2))
            text(S_back, 20, y0 + h / 2 - 3, 't = %d ms' % r['t'], 10, fill='#a3341f', weight='bold')
        elif t == 'end':
            text(S_front, 20, y0 + 20, '\u25a0 fine della simulazione: ' + trunc(r['text'], 160), 10.5, fill='#333', italic=True)

    # --- legenda e note
    y = bottom + 24
    y = legend(S_front, text, marker, y, W, sim)
    # messaggi dichiarati ma non scambiati in questa esecuzione
    unused = [n for n, (k, _, _) in m.messages.items() if n not in sim.used and k not in ('Token', 'Signal', 'Invitation')]
    lines = []
    if unused:
        lines.append('Messaggi dichiarati ma non presenti in questa esecuzione: ' + ', '.join(unused))
    lines.append('Una sola esecuzione possibile: condizioni [# ... #] non valutate (ramo then, salvo --else), '
                 'tempo logico (whenTime scatta solo a sistema fermo), risposte presunte per gli attori passivi.')
    for l in lines:
        for chunk in wrap(l, int((W - 40) / (10.5 * 0.56))):
            text(S_front, 20, y + 16, chunk, 10.5, fill='#444')
            y += 15
    H = y + 26
    title = ('<text x="20" y="30" font-size="18" font-weight="bold" fill="#111">System %s \u2014 diagramma di sequenza</text>'
             % esc(m.name))
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w:.0f} {h:.0f}" width="{w:.0f}" height="{h:.0f}" '
            'font-family="Helvetica, Arial, sans-serif">\n<defs>{defs}</defs>\n<rect width="100%" height="100%" fill="#fff"/>\n'
            '{title}\n{b}\n{mid}\n{f}\n</svg>\n').format(w=W, h=H, defs=''.join(DEFS), title=title,
                                                       b='\n'.join(S_back), mid='\n'.join(S_mid), f='\n'.join(S_front))


def wrap(s, n):
    out, cur = [], ''
    for w in s.split(' '):
        if len(cur) + len(w) + 1 > n and cur:
            out.append(cur); cur = w
        else:
            cur = (cur + ' ' + w).strip()
    if cur: out.append(cur)
    return out


def draw_arrow(L, x1, y1, x2, y2, kind, col, marker, dash=None):
    if kind == 'event':
        rail(L, [(x1, y1), (x2, y2)], col, marker(col, 'o'))
        return
    head = marker(col, 'f' if kind == 'request' else 'o')
    da = dash or {'reply': '6 4', 'delegate': '9 3 2 3'}.get(kind, '')
    L.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-width="1.4"%s marker-end="url(#%s)"/>'
             % (x1, y1, x2, y2, col, ' stroke-dasharray="%s"' % da if da else '', head))


def draw_poly(L, pts, kind, col, marker):
    p = ' '.join('%.1f,%.1f' % q for q in pts)
    if kind == 'event':
        rail(L, pts, col, marker(col, 'o')); return
    head = marker(col, 'f' if kind == 'request' else 'o')
    da = {'reply': '6 4', 'delegate': '9 3 2 3'}.get(kind, '')
    L.append('<polyline points="%s" fill="none" stroke="%s" stroke-width="1.4"%s marker-end="url(#%s)"/>'
             % (p, col, ' stroke-dasharray="%s"' % da if da else '', head))


def rail(L, pts, col, mk):
    p = ' '.join('%.1f,%.1f' % q for q in pts)
    L.append('<polyline points="%s" fill="none" stroke="%s" stroke-width="4.2" stroke-linejoin="round"/>' % (p, col))
    L.append('<polyline points="%s" fill="none" stroke="#fff" stroke-width="1.6" stroke-linejoin="round"/>' % p)
    L.append('<polyline points="%s" fill="none" stroke="none" stroke-width="1.4" marker-end="url(#%s)"/>' % (p, mk))


def draw_label(L, text, cx, y, label, col, x1, x2):
    span = max(abs(x2 - x1) - 10, 280)          # l'etichetta puo' uscire dalla campata
    s = label if tw(label, 10) <= span else trunc(label, int(span / (10 * 0.56)))
    w = tw(s, 10) + 8
    cx = max(cx, 8 + w / 2)
    L.append('<rect x="%.1f" y="%.1f" width="%.1f" height="14" rx="2" fill="#fff" fill-opacity="0.85"/>' % (cx - w / 2, y - 11, w))
    text(L, cx, y, s, 10, 'middle', fill=col)


def legend(L, text, marker, y, W, sim):
    items = [('request', 'request (asincrona; la risposta arriva come reply)'),
             ('dispatch', 'dispatch (forward, autodispatch)'),
             ('reply', 'reply (replyTo)'),
             ('event', 'event (emit): una freccia per ogni attore che lo percepisce'),
             ('delegate', 'delegate: messaggio inoltrato all\'attore delegato')]
    used = {r.get('kind') for r in sim.rows}
    items = [it for it in items if it[0] in used]
    h = 30 + 20 * (len(items) + 2)
    L.append('<rect x="20" y="%.1f" width="%.1f" height="%.1f" rx="6" fill="#fff" stroke="#999"/>' % (y, min(W - 40, 760), h))
    text(L, 32, y + 20, 'Legenda', 12, weight='bold')
    yy = y + 40
    for k, lab in items:
        col = MSGCOLOR[k]
        draw_arrow(L, 34, yy - 4, 104, yy - 4, k, col, marker)
        text(L, 118, yy, lab, 10.5)
        yy += 20
    L.append('<rect x="64" y="%.1f" width="10" height="16" fill="%s" stroke="%s"/>' % (yy - 14, FILL['QActor'][0], FILL['QActor'][1]))
    text(L, 118, yy, 'barra di attivazione: l\'attore esegue (dal prelievo di un messaggio al blocco)', 10.5)
    yy += 20
    L.append('<rect x="44" y="%.1f" width="50" height="15" rx="7" fill="#fff" stroke="#777"/>' % (yy - 12))
    text(L, 69, yy - 1, 'stato', 9.5, 'middle', fill='#444', italic=True)
    text(L, 118, yy, 'stato in cui entra l\'attore;   \u29d7 scadenza whenTime;   nota gialla: scelta o azione non visibile', 10.5)
    return y + h


# =====================================================================
# 4. MAIN
# =====================================================================

def trace_text(sim):
    out = []
    for r in sim.rows:
        t = r['type']
        if t == 'msg':
            out.append('%-12s %-9s %s -> %s : %s : %s' % ('', r['kind'], name(r['src']), name(r['dst']), r['mid'], r['payload']))
        elif t == 'lost':
            out.append('%-12s %-9s %s -> (nessuno) : %s' % ('', r['kind'], r['src'], r['mid']))
        elif t == 'state':
            out.append('%-12s [%s]' % (r['actor'], r['text']))
        elif t in ('note', 'timeout'):
            out.append('%-12s %s' % (r['actor'], r['text']))
        elif t == 'time':
            out.append('---- t = %d ms' % r['t'])
        elif t == 'create':
            out.append('%-12s create %s' % (r['src'], r['dst']))
        elif t == 'end':
            out.append('==== ' + r['text'])
    return '\n'.join(out)


def name(n):
    return {CLIENT: 'client', ENV: 'ambiente'}.get(n, n)


def main(argv=None):
    ap = argparse.ArgumentParser(description='Diagramma di sequenza (SVG, stile UML) di modelli qak.')
    ap.add_argument('files', nargs='+', help='file .qak (anche pattern glob)')
    ap.add_argument('-o', '--out', help='file SVG di uscita (solo con un unico modello)')
    ap.add_argument('--steps', type=int, default=80, help='limite di messaggi/passi della simulazione (default 80)')
    ap.add_argument('--stimulus', action='append',
                    help='stimolo esterno DEST:MSG[:PAYLOAD] (ripetibile); sostituisce quelli automatici')
    ap.add_argument('--no-stimuli', action='store_true', help='nessuno stimolo esterno automatico')
    ap.add_argument('--else', dest='else_', action='append', metavar='ATTORE.STATO',
                    help='nello stato indicato scegli il ramo else delle condizioni (ripetibile)')
    ap.add_argument('--loop', type=int, default=3,
                    help='dopo N ripetizioni del ramo then di uno stesso "Goto ... if" si prende il ramo else (default 3)')
    ap.add_argument('--ext-events', type=int, default=1,
                    help='quante volte al massimo si simula ciascun evento esterno (default 1; 0 = mai)')
    ap.add_argument('--no-states', action='store_true', help='non mostrare gli stati raggiunti')
    ap.add_argument('--trace', action='store_true', help='stampa anche la traccia testuale')
    ap.add_argument('--png', action='store_true', help='genera anche PNG (richiede cairosvg)')
    args = ap.parse_args(argv)

    files = []
    for f in args.files:
        files += glob.glob(f) or [f]
    rc = 0
    for f in files:
        try:
            with open(f, encoding='utf-8', errors='replace') as fh:
                m = parse(fh.read())
        except OSError as e:
            print('%s: %s' % (f, e), file=sys.stderr); rc = 1; continue
        if not any(a.active for a in m.actors.values()):
            print('%s: nessun QActor con stati: diagramma non generato' % f, file=sys.stderr); rc = 1; continue
        Msg.seq = 0
        sim = Sim(m, args)
        sim.simulate()
        svg = render(m, sim, args)
        out = args.out if (args.out and len(files) == 1) else os.path.splitext(f)[0] + '_seq.svg'
        with open(out, 'w', encoding='utf-8') as fh:
            fh.write(svg)
        print('%s -> %s  (%d righe; %s)' % (f, out, len(sim.rows), sim.end_reason))
        if args.trace:
            print(trace_text(sim))
        if args.png:
            try:
                import cairosvg
                cairosvg.svg2png(url=out, write_to=os.path.splitext(out)[0] + '.png')
                print('   + PNG')
            except ImportError:
                print('   (PNG non generato: pip install cairosvg)')
    return rc


if __name__ == '__main__':
    sys.exit(main())
