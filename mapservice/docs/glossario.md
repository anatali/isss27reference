# R2 · Glossario

Vocabolario concordato con il committente. Ogni termine ha un corrispondente nei tipi di
`src/main/java/domain` (R3) o nei messaggi di `src/mapservice_requisiti.qak` (R5).

| Termine | Definizione concordata | Corrispondente formale |
|---------|------------------------|------------------------|
| parametro di crescita r | numero reale in [0,4] | `LogisticRequest.r` |
| valore iniziale x0 | numero reale in [0,1]: frazione della capacità massima | `LogisticRequest.x0` |
| numero di passi N | intero in [0, N_MAX] (N_MAX da concordare, D3) | `LogisticRequest.n`, `LogisticRequest.N_MAX` |
| traiettoria | la sequenza x0, x1, …, xN ottenuta applicando N volte la regola (N+1 valori, D7) | `Trajectory` |
| grafico | rappresentazione della traiettoria: ascissa n, ordinata x_n (D1-a) | — (pagina) |
| richiesta | messaggio con cui un client chiede una traiettoria per una terna (r, x0, N) | `Request evallogistic : args(R,X0,N)` |
| esito | risposta a una richiesta: *successo* con l'indirizzo della pagina, oppure *fallimento* con una causa | `Outcome` = `Done` \| `Failed`; `Reply logisticdone`, `Reply logisticfailed` |
| causa | motivo di un fallimento: `wrongparams`, `unavailable`, `timeout` | `Cause` |
| pagina | documento consultabile da un browser, anche da un'altra macchina, che mostra il grafico | `Done.page` (URI) |
| tempo massimo T | intervallo entro cui ogni richiesta riceve il suo esito (assunzione: 5 s, D10) | `@Test(timeout=5000)` nei test R6 |

## Termini da non usare

| Termine ambiguo | Usare invece | Perché |
|-----------------|--------------|--------|
| «il grafico» senza specificazione | «grafico della serie temporale» | D1 ammette quattro grafici diversi |
| «i valori» | «la traiettoria (N+1 valori)» | D7 |
| «errore» | «esito di fallimento con causa» | un errore non segnalato al client viola D10 |
