package domain;

/**
 * R3 · Parametri di una richiesta (glossario: r, x0, N).
 * Gli intervalli vengono dal dominio (D3).
 *
 * Le condizioni sono scritte come !(a <= v && v <= b) e non come v < a || v > b:
 * la seconda forma lascia passare NaN, che una stringa malformata può produrre.
 */
public record LogisticRequest(double r, double x0, int n) {

    /** D3: valore da concordare con il committente (vedi docs/requisiti.md). */
    public static final int N_MAX = 10_000;

    public LogisticRequest {
        if (!(r  >= 0.0 && r  <= 4.0)) throw new IllegalArgumentException("r fuori da [0,4]: " + r);
        if (!(x0 >= 0.0 && x0 <= 1.0)) throw new IllegalArgumentException("x0 fuori da [0,1]: " + x0);
        if (n < 0 || n > N_MAX)        throw new IllegalArgumentException("n fuori da [0," + N_MAX + "]: " + n);
    }

    /**
     * Costruisce una richiesta a partire dai tre argomenti testuali del messaggio
     * evallogistic : args(R,X0,N). Testi non numerici producono IllegalArgumentException,
     * cioè l'esito failed(wrongparams) (D10).
     */
    public static LogisticRequest parse(String r, String x0, String n) {
        try {
            return new LogisticRequest(Double.parseDouble(r.trim()),
                                       Double.parseDouble(x0.trim()),
                                       Integer.parseInt(n.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("parametri non numerici: " + r + "," + x0 + "," + n, e);
        }
    }

    /** Vero se la terna testuale è una richiesta valida (usabile come guardia nel modello qak). */
    public static boolean isValid(String r, String x0, String n) {
        try { parse(r, x0, n); return true; }
        catch (IllegalArgumentException e) { return false; }
    }
}
