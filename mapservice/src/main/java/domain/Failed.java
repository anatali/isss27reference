package domain;

/** Esito di fallimento con causa (D10). Payload qak: failed(CAUSE). */
public record Failed(Cause cause) implements Outcome {
    public Failed {
        if (cause == null) throw new IllegalArgumentException("cause obbligatoria");
    }
}
