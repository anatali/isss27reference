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

 