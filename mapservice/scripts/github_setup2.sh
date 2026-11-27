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
#        ./scripts/github_setup0.sh anatali/isss27reference
# Requisiti: GitHub CLI (https://cli.github.com) con permesso di scrittura sul repository.
# Lo script NON è idempotente per le issue: eseguirlo una sola volta.
# =============================================================================
set -euo pipefail

REPO="${1:?uso: $0 OWNER/REPO}"
MILESTONE="Sprint 1 · Analisi"


echo "   requisito"

declare -A Q A
Q[D1]="Il requisito dice «il grafico della funzione iterativa». Interpretazioni:
(a) serie temporale x_n in funzione di n; (b) diagramma a ragnatela (cobweb);
(c) diagramma di biforcazione (x asintotici in funzione di r); (d) parabola f(x)=r·x·(1-x).
**Domanda:** quale di questi grafici? (allegare quattro immagini di esempio)
Impatto: (c) richiede un calcolo per molti valori di r e cambia il contenuto della richiesta."
A[D1]="Serie temporale x_n in funzione di n (D1-a): la più semplice e la base per (b) e (c)."

declare -A TITLE=(
  [D1]="Quale grafico intende il committente?"            }


 