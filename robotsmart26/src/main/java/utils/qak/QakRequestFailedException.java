package utils.qak;

/**
 * [Claude, Oct 2026] REQUEST FAILED
 * Eccezione lanciata LATO CLIENT da QakClientUtils.requestOrFail quando una
 * request a un attore qak non puo' essere soddisfatta.
 *
 *   reason = "unknowndest" : il contesto ha risposto con la reply
 *                            requestfailed(REQID,unknowndest): destinatario
 *                            inesistente oppure hidden (indistinguibili)
 *   reason = "timeout"     : nessuna risposta entro il tempo stabilito
 *                            (rete interrotta, contesto spento, canale MQTT, ...)
 *
 * Le eccezioni non viaggiano sulla rete: il contesto invia un messaggio
 * (la reply requestfailed) ed e' il client a trasformarlo in eccezione.
 */
public class QakRequestFailedException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String requestId;
    private final String reason;

    public QakRequestFailedException(String requestId, String reason, String detail) {
        super("request " + requestId + " failed: " + reason + (detail == null ? "" : " (" + detail + ")"));
        this.requestId = requestId;
        this.reason    = reason;
    }
    public String getRequestId() { return requestId; }
    public String getReason()    { return reason; }
    public boolean isTimeout()   { return "timeout".equals(reason); }
}
