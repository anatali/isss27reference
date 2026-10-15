package utils.qak;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;

/**
 * [Claude, Oct 2026] REQUEST FAILED
 * Utilita' per un client 'alieno' (non qak) che invia request a un contesto qak.
 *
 * Uso:
 *   IApplMessage req = CommUtils.buildRequest("tester", "getrobotstate", "getrobotstate(0)", "robotsmart");
 *   try {
 *       IApplMessage answer = QakClientUtils.requestOrFail(conn, req, 8000);
 *       ...
 *   } catch (QakRequestFailedException e) {
 *       if (e.isTimeout()) { ...riaprire la connessione... } else { ...destinatario sconosciuto... }
 *   }
 *
 * Il timeout deve superare il tempo che il contesto impiega a rifiutare una request
 * verso un destinatario sconosciuto (10 tentativi x 500 ms = circa 5 s).
 *
 * NB: dopo un timeout la connessione viene chiusa. Con request(msg,tout) di basicomm23
 * la lettura avviene in un thread separato che resta in attesa: una risposta in ritardo
 * verrebbe consumata da quel thread e la successiva request leggerebbe la risposta
 * sbagliata. Dopo un timeout occorre quindi aprire una nuova connessione.
 */
public final class QakClientUtils {

    public static final String REQUEST_FAILED = "requestfailed";
    private static final Pattern FAILED =
            Pattern.compile("requestfailed\\(\\s*([^,]+?)\\s*,\\s*([^)]+?)\\s*\\)");

    private QakClientUtils() { }

    public static IApplMessage requestOrFail(Interaction conn, IApplMessage req, int toutMs)
            throws QakRequestFailedException {
        IApplMessage answer;
        try {
            answer = conn.request(req, toutMs);
        } catch (Exception e) {
            try { conn.close(); } catch (Exception ignored) { }
            throw new QakRequestFailedException(req.msgId(), "timeout", e.getMessage());
        }
        if (answer == null) {
            throw new QakRequestFailedException(req.msgId(), "timeout", "connection closed");
        }
        if (REQUEST_FAILED.equals(answer.msgId())) {
            Matcher m = FAILED.matcher(answer.msgContent());
            String reason = m.find() ? m.group(2) : answer.msgContent();
            throw new QakRequestFailedException(req.msgId(), reason, "from " + answer.msgSender());
        }
        return answer;
    }
}
