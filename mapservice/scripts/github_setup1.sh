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
 