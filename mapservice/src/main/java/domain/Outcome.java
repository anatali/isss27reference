package domain;

/**
 * R3 · Esito per il client (D6, D10): una e una sola risposta per richiesta.
 * Corrisponde alle Reply logisticdone / logisticfailed del modello qak.
 */
public sealed interface Outcome permits Done, Failed {}
