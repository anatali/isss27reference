# mapservice

Microservizio che calcola e visualizza il grafico della **mappa logistica**
`x(n+1) = r · x(n) · (1 − x(n))`.

Stato: **analisi dei requisiti** (Sprint 1 · Analisi). Il processo segue il documento
*mapservice: analisi dei requisiti e analisi del problema* (`claude/mapservice_analisi.html`).

## Organizzazione del repository (§2.4)

```
mapservice/
├── docs/
│   ├── requisiti.md              # R1 tabella delle ambiguità e decisioni      [compilato]
│   ├── glossario.md              # R2 glossario in linguaggio naturale         [compilato]
│   ├── problema.md               # A1, A5 questioni e livelli                  [schema, fase successiva]
│   ├── adr/0001-tecnologia.md    # A6 decisione tecnologica                    [schema, fase successiva]
│   └── esperimenti/              # A3 risultati degli esperimenti              [schema, fase successiva]
├── features/mapservice.feature   # R4 scenari di accettazione (Gherkin)        [compilato]
├── src/
│   ├── main/java/domain/         # R3 tipi di dominio; (A2 funzione pura)      [R3 compilato]
│   ├── mapservice_requisiti.qak  # R5 modello a scatola nera                   [compilato, ATTIVO]
│   └── mapservice_analisi.qaktt  # A7 architettura logica                      [segnaposto, INATTIVO]
├── src/test/java/
│   ├── domain/                   # test dei tipi R3 (verdi)
│   └── acceptance/               # R6 accettazione (rossi)
├── scripts/github_setup.sh       # etichette, milestone, issue D1–D10 e assunzioni
└── userDocs/mapservice.yml       # workflow GitHub Actions (da copiare in .github/workflows)
```

## Un solo modello qak attivo alla volta

Il generatore qak elabora tutti i file `.qak` di `src/`. I due modelli dichiarano lo stesso
`System mapservice` e lo stesso `Context ctxmapservice`, quindi genererebbero le stesse classi.
Si segue la convenzione di `firstprj27`: il modello non attivo ha estensione `.qaktt`.
Al passaggio all'analisi del problema si scambiano le estensioni. I test di R6 restano gli stessi
e misurano il progresso.

## Primo avvio

1. **Eclipse con il plugin qak**: aprire `src/mapservice_requisiti.qak`. Il generatore produce
   `src/main/java/it/unibo/...`, `mapservice.pl` e `sysRules.pl`. Vanno committati:
   la CI non esegue il generatore.
2. Copiare da `firstprj27` gli script `qak2svg.py` e `qakStateDiagram.py` (figure dei modelli).
   Senza gli script la build continua, ma non produce le figure.
3. `gradlew build`: compila ed esegue i test unitari (`src/test/java/domain`). Devono essere verdi.
4. `gradlew acceptanceTest`: esegue R6 contro il modello attivo. Il report è in
   `build/reports/tests/acceptance/`. Contro il modello dei requisiti l'esito atteso è
   1 verde (`contractIsPublished`) e 4 rossi.
5. `gradlew run`: avvia il Context su `localhost:8333`. La pagina
   `http://localhost:8333/` mostra il contratto (`/info.json?actor=mapservice` in JSON).

## GitHub: issue e pull request

```
gh auth login
./scripts/github_setup.sh OWNER/REPO
```

Lo script crea le etichette di §2.1, la milestone *Sprint 1 · Analisi*, la issue contenitore
con la checklist R1–R6 e le issue D1–D10 (`domanda-committente`) con le relative `assunzione`.
Alla fine stampa la corrispondenza Dn → #n, da riportare nella colonna *Issue* di
`docs/requisiti.md`.

Pull request previste per questa fase (§3.1), una per prodotto coerente:

| PR | File | Collega |
|----|------|---------|
| R1 requisiti riformulati | `docs/requisiti.md` | `Closes` delle domande risolte |
| modello del dominio | `docs/glossario.md`, `src/main/java/domain/`, `src/test/java/domain/` | `Refs` D3, D6, D7, D10 |
| scenari | `features/mapservice.feature` | revisore: il committente |
| contratto del servizio (**bozza**) | `src/mapservice_requisiti.qak` + codice generato | pronta quando D6 e D10 sono chiuse |
| test di accettazione (rossi) | `src/test/java/acceptance/` | `Refs` R4 |

Chiusura della fase: issue contenitore chiusa, `git tag req-v0.1 && git push origin req-v0.1`.
