package domain;

/** Cause di fallimento (D10). Il nome qak è il nome in minuscolo: wrongparams, unavailable, timeout. */
public enum Cause {
    WRONG_PARAMS("wrongparams"), UNAVAILABLE("unavailable"), TIMEOUT("timeout");

    private final String qakName;
    Cause(String qakName) { this.qakName = qakName; }
    public String qakName() { return qakName; }
}
