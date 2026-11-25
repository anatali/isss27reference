#!/usr/bin/env bash
# =============================================================================
# mapservice · predisposizione GitHub per l'analisi dei requisiti (§2.1, §3.1 azioni 1-4)
# -----------------------------------------------------------------------------
# Crea nel repository indicato:
#   - le etichette di §2.1
#   - la milestone "Sprint 1 · Analisi"
#   - la issue contenitore 'requisito' con la checklist R1-R6
#   - una issue 'domanda-committente' per ciascuna ambiguità D1-D10
#   - una issue 'assunzione' per ciascuna assunzione di lavoro (D8 resta solo domanda)
# e stampa la corrispondenza Dn -> #n da riportare in docs/requisiti.md.
#
# Uso:   gh auth login                       (una volta)
#        ./scripts/github_setup.sh OWNER/REPO
# Requisiti: GitHub CLI (https://cli.github.com) con permesso di scrittura sul repository.
# Lo script NON è idempotente per le issue: eseguirlo una sola volta.
# =============================================================================
set -euo pipefail

REPO="${1:?uso: $0 OWNER/REPO}"
MILESTONE="Sprint 1 · Analisi"

echo "== etichette"
label() { gh label create "$1" --repo "$REPO" --color "$2" --description "$3" --force >/dev/null; echo "   $1"; }
label requisito           1d5b78 "issue contenitore di un requisito"
label domanda-committente 0e8a16 "ambiguità: domanda per il committente"
label assunzione          fbca04 "assunzione di lavoro, aperta fino a conferma"
label questione           5319e7 "questione dell'analisi del problema"
label esperimento         c5def5 "affermazione da verificare con una misura"
label rischio             b60205 "rischio, con probabilità e impatto"
label decisione           6a3d8f "decisione con alternative (si chiude con un ADR)"
label feedback            d4c5f9 "osservazione dopo la dimostrazione del prototipo"

echo "== milestone"
if ! gh api "repos/$REPO/milestones?state=all" --jq '.[].title' | grep -qxF "$MILESTONE"; then
  gh api "repos/$REPO/milestones" -f title="$MILESTONE" \
     -f description="Analisi dei requisiti e analisi del problema di mapservice" >/dev/null
fi
echo "   $MILESTONE"

issue() {   # issue TITOLO ETICHETTA CORPO  -> stampa il numero
  gh issue create --repo "$REPO" --title "$1" --label "$2" --milestone "$MILESTONE" --body "$3" \
    | sed -E 's#.*/issues/([0-9]+)$#\1#'
}

echo "== issue contenitore"
REQ=$(issue "mapservice · analisi dei requisiti" requisito "$(cat <<'EOF'
Requisito del committente:

> Realizzare un microservizio denominato **mapservice** che calcoli e visualizzi il grafico
> della funzione iterativa nota come «mappa logistica».

Prodotti della fase (ciascuno entra in `main` con una PR):
- [ ] R1 `docs/requisiti.md` · tabella delle ambiguità, decisioni e assunzioni
- [ ] R2 `docs/glossario.md` · glossario
- [ ] R3 `src/main/java/domain/` · glossario tradotto in tipi (+ `LogisticRequestTest`)
- [ ] R4 `features/mapservice.feature` · scenari di accettazione (revisore: il committente)
- [ ] R5 `src/mapservice_requisiti.qak` · modello a scatola nera (PR prima in bozza)
- [ ] R6 `src/test/java/acceptance/` · test di accettazione (rossi)

Chiusura: issue chiusa e tag `req-v0.1`.
EOF
)")
echo "   #$REQ requisito"

declare -A Q A
Q[D1]="Il requisito dice «il grafico della funzione iterativa». Interpretazioni:
(a) serie temporale x_n in funzione di n; (b) diagramma a ragnatela (cobweb);
(c) diagramma di biforcazione (x asintotici in funzione di r); (d) parabola f(x)=r·x·(1-x).
**Domanda:** quale di questi grafici? (allegare quattro immagini di esempio)
Impatto: (c) richiede un calcolo per molti valori di r e cambia il contenuto della richiesta."
A[D1]="Serie temporale x_n in funzione di n (D1-a): la più semplice e la base per (b) e (c)."
Q[D2]="«Microservizio»: libreria nello stesso processo, o componente di rete dispiegabile in modo indipendente?
**Domanda:** chi lo usa: una persona con un browser, un altro servizio, entrambi?"
A[D2]="Componente di rete. Client: altri servizi e persone con un browser."
Q[D3]="«Calcoli»: parametri fissi in configurazione, o r, x0, N forniti a ogni richiesta?
**Domanda:** chi sceglie i parametri? Esistono limiti su N?"
A[D3]="Parametri a ogni richiesta; r ∈ [0,4], x0 ∈ [0,1] (dal dominio); N ≤ N_MAX = 10000 (valore da concordare)."
Q[D4]="«Visualizzi»: sullo schermo della macchina del servizio, o sul dispositivo di chi ha fatto la richiesta?
**Domanda:** dove si trova chi guarda il grafico?"
A[D4]="Sul dispositivo del client, tramite un browser, anche su un'altra macchina."
Q[D5]="(non detto) Modalità: grafico a calcolo finito; grafico che cresce durante il calcolo; calcolo con avvio e arresto.
**Domanda:** il grafico deve aggiornarsi durante il calcolo?"
A[D5]="Grafico a calcolo finito; l'aggiornamento incrementale resta un'evoluzione possibile."
Q[D6]="(non detto) Contenuto della risposta: conferma; valori calcolati; immagine; indirizzo di una pagina.
**Domanda:** che cosa riceve chi ha fatto la richiesta?"
A[D6]="Indirizzo di una pagina che mostra il grafico; i valori restano ottenibili dai client software."
Q[D7]="(non detto) La traiettoria: x0…xN (N+1 valori) oppure x1…xN (N valori)?
**Domanda:** il valore iniziale compare nel grafico?"
A[D7]="N+1 valori, da x0 a xN: il punto di partenza è visibile."
Q[D8]="(non detto) Correttezza: valori identici a un riferimento; entro una tolleranza; regime riconosciuto.
**Domanda:** come verifica che il grafico è giusto?
Nota: da riproporre dopo aver spiegato il caos (questione Q6, esperimento E1)."
Q[D9]="(non detto) Dipendenze: grafico prodotto da un servizio di terzi o dal sistema stesso?
**Domanda:** è accettabile dipendere da un sito esterno? Il servizio deve funzionare senza Internet?"
A[D9]="Nessuna dipendenza da servizi di terzi."
Q[D10]="(non detto) Errori e tempi: nessuna garanzia, oppure risposta sempre entro un tempo massimo.
**Domanda:** che cosa deve accadere se la richiesta è errata o il servizio è in difficoltà?"
A[D10]="Una e una sola risposta per richiesta, entro T = 5 s, di successo o di fallimento con causa."

declare -A TITLE=(
  [D1]="Quale grafico intende il committente?"            [D2]="Che cosa si intende per microservizio?"
  [D3]="Chi sceglie i parametri e con quali limiti?"     [D4]="Dove si trova chi guarda il grafico?"
  [D5]="Il grafico deve aggiornarsi durante il calcolo?" [D6]="Che cosa riceve chi fa la richiesta?"
  [D7]="Il valore iniziale fa parte della traiettoria?"  [D8]="Come si stabilisce che il grafico è corretto?"
  [D9]="Sono ammesse dipendenze da servizi esterni?"     [D10]="Che cosa accade con richieste errate o guasti?" )

echo "== domande e assunzioni"
MAP=""
for D in D1 D2 D3 D4 D5 D6 D7 D8 D9 D10; do
  NQ=$(issue "$D · ${TITLE[$D]}" domanda-committente "${Q[$D]}

Parte di #$REQ. Si chiude con la PR che aggiorna \`docs/requisiti.md\` (\`Closes #n\`), non con un commento.")
  LINE="$D -> domanda #$NQ"
  if [[ -n "${A[$D]:-}" ]]; then
    SHORT="${A[$D]%%[;:.]*}"; SHORT="${SHORT:0:70}"
    NA=$(issue "$D · assunzione: $SHORT" assunzione "Assunzione di lavoro in attesa di risposta a #$NQ:

${A[$D]}

Resta aperta finché il committente non la conferma o la smentisce.")
    LINE="$LINE, assunzione #$NA"
  fi
  echo "   $LINE"; MAP="$MAP$LINE"$'\n'
done

echo
echo "Riportare in docs/requisiti.md (colonna Issue):"
printf "%s" "$MAP"
