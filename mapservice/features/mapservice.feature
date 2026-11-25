# language: en
# R4 · Scenari di accettazione (Gherkin)
# Ogni scenario corrisponde a un metodo di
#   src/test/java/acceptance/MapserviceAcceptanceTest.java   (R6)
# Il nome del metodo è riportato nel tag @test=... dello scenario.
# La PR che introduce o modifica questo file ha il committente fra i revisori.

Feature: calcolo e visualizzazione della mappa logistica
  Un client remoto chiede la traiettoria della mappa logistica per una terna (r, x0, N)
  e riceve, entro un tempo massimo, l'indirizzo di una pagina con il grafico
  oppure un esito di fallimento con la causa.

  @test=fixedPointScenario @D1 @D4 @D6 @D7 @D10
  Scenario: convergenza a un punto fisso
    When un client chiede la traiettoria con r=2.0, x0=0.7, N=8
    Then entro 5 secondi riceve un esito di successo con l'indirizzo di una pagina
    And un browser su un'altra macchina, aprendo l'indirizzo, vede un grafico con 9 punti
    And l'ultimo valore differisce da 0.5 per meno di 1e-6

  @test=wrongParamsScenario @D3 @D10
  Scenario: parametri non validi
    When un client chiede la traiettoria con r=-2.0, x0=0.7, N=8
    Then entro 5 secondi riceve un esito di fallimento con causa "wrongparams"
    And nessuna pagina viene pubblicata

  @test=notNumericParamsScenario @D3 @D10
  Scenario: parametri non numerici
    When un client chiede la traiettoria con r="abc", x0=0.7, N=8
    Then riceve un esito di fallimento con causa "wrongparams"

  @test=chaoticRegimeScenario @D7 @D8
  Scenario: regime caotico
    When un client chiede la traiettoria con r=3.8, x0=0.1, N=200
    Then tutti i 201 valori sono compresi fra 0 e 1
    And i primi 20 valori coincidono con quelli di riferimento entro 1e-9
