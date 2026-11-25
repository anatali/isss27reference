# R1 · Requisiti riformulati: ambiguità, decisioni, assunzioni

Fase: **analisi dei requisiti** · Milestone: *Sprint 1 · Analisi* · Issue contenitore: `#<requisito>`

## Requisito del committente (testo originale)

> Realizzare un microservizio denominato **mapservice** che calcoli e visualizzi il grafico
> della funzione iterativa nota come «mappa logistica».

Mappa logistica: `x(n+1) = r · x(n) · (1 − x(n))`, con valore iniziale `x0` e parametro `r`.

## Come leggere la tabella

- Ogni riga corrisponde a una issue con etichetta `domanda-committente`.
- La colonna **Stato** vale `aperta`, `assunzione` (il committente non ha deciso: vale
  l'assunzione di lavoro, tracciata da una issue `assunzione`) oppure `decisa` (risposta del
  committente scritta qui).
- Una issue si chiude **con la PR che aggiorna questo file** (`Closes #n`), non con il commento
  che riporta la risposta: la decisione deve stare in un file del repository.
- Sostituire `#?` con il numero assegnato da GitHub (lo script `scripts/github_setup.sh` stampa
  la corrispondenza Dn → #n).

## Tabella delle ambiguità

| Id | Termine | Interpretazioni possibili | Domanda al committente | Assunzione di lavoro | Stato | Issue |
|----|---------|---------------------------|------------------------|----------------------|-------|-------|
| D1 | il grafico della mappa logistica | (a) serie temporale x_n in funzione di n; (b) diagramma a ragnatela (cobweb); (c) diagramma di biforcazione; (d) parabola f(x) = r·x·(1−x) | Quale dei quattro grafici? (mostrarli tutti e quattro) | (a) serie temporale | assunzione | #? |
| D2 | microservizio | libreria nello stesso processo; componente di rete dispiegabile in modo indipendente | Chi lo usa: una persona con un browser, un altro servizio, entrambi? | componente di rete; client: altri servizi e persone con un browser | assunzione | #? |
| D3 | calcoli | parametri fissi in configurazione; r, x0, N forniti a ogni richiesta | Chi sceglie i parametri? Esistono limiti su N? | parametri a ogni richiesta; r ∈ [0,4], x0 ∈ [0,1]; N ≤ N_MAX (valore da concordare, oggi 10 000) | assunzione | #? |
| D4 | visualizzi | schermo della macchina del servizio; dispositivo di chi ha fatto la richiesta | Dove si trova chi guarda il grafico? | dispositivo del client, tramite browser, anche su un'altra macchina | assunzione | #? |
| D5 | (non detto) modalità | grafico a calcolo finito; grafico che cresce durante il calcolo; calcolo con avvio e arresto | Il grafico deve aggiornarsi durante il calcolo? | grafico a calcolo finito | assunzione | #? |
| D6 | (non detto) contenuto della risposta | conferma; valori; immagine; indirizzo di una pagina | Che cosa riceve chi ha fatto la richiesta? | indirizzo di una pagina; valori ottenibili dai client software | assunzione | #? |
| D7 | (non detto) la traiettoria | x0…xN (N+1 valori); x1…xN (N valori) | Il valore iniziale compare nel grafico? | N+1 valori, da x0 a xN | assunzione | #? |
| D8 | (non detto) correttezza | identici a un riferimento; entro tolleranza; regime riconosciuto | Come verifica che il grafico è giusto? | tolleranza nelle prime iterazioni + proprietà generali (da riproporre dopo Q6) | aperta | #? |
| D9 | (non detto) dipendenze | grafico prodotto da un servizio di terzi; prodotto dal sistema | È accettabile dipendere da un sito esterno? Deve funzionare senza Internet? | nessuna dipendenza da servizi di terzi | assunzione | #? |
| D10 | (non detto) errori e tempi | nessuna garanzia; risposta sempre, entro un tempo massimo | Che cosa deve accadere se la richiesta è errata o il servizio è in difficoltà? | una e una sola risposta per richiesta, entro T = 5 s, di successo o di fallimento con causa | assunzione | #? |

> Le righe D5–D10 riguardano aspetti che il requisito non nomina. Senza D6 non si scrive un
> client; senza D8 non si scrive un test di accettazione; senza D10 un client può restare in
> attesa per sempre.

## Requisiti riformulati (valgono con le assunzioni correnti)

| Id | Requisito riformulato | Da | Verificato da |
|----|----------------------|----|---------------|
| RQ1 | Il sistema accetta richieste di rete `evallogistic : args(R,X0,N)` da client remoti. | D2, D3 | `MapserviceContractTest.contractIsPublished` |
| RQ2 | Per ogni richiesta il sistema invia **esattamente una** risposta entro 5 s: `logisticdone : done(URL)` oppure `logisticfailed : failed(CAUSE)`. | D6, D10 | scenari *convergenza*, *parametri non validi* |
| RQ3 | Una richiesta con r ∉ [0,4], x0 ∉ [0,1], N ∉ [0,N_MAX] o non numerici riceve `failed(wrongparams)` e non produce pagine. | D3, D10 | scenari *parametri non validi*, *non numerici* |
| RQ4 | `URL` indica una pagina, consultabile da un browser su un'altra macchina, con il grafico della serie temporale x0…xN (N+1 punti). | D1, D4, D6, D7 | scenario *convergenza* |
| RQ5 | Il grafico è prodotto senza servizi di terzi. | D9 | (fase di analisi del problema, V6) |
| RQ6 | I valori rispettano i criteri di correttezza di D8. | D8 | scenario *regime caotico* (provvisorio) |

## Registro delle decisioni

| Data | Id | Decisione | Fonte (commento della issue) | PR |
|------|----|-----------|------------------------------|----|
| | | | | |
