package robotsmart26tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;
import utils.qak.QakClientUtils;
import utils.qak.QakRequestFailedException;

/**
 * [Claude, Oct 2026] REQUEST FAILED / HIDDEN QActor  (JUnit 4)
 * ------------------------------------------------------------------
 * Prerequisiti: ctxrobotsmart attivo su localhost:8020 con la nuova
 * unibo.qakactor23 e, nel modello,
 *     QActor robotmnemo context ctxrobotsmart hidden ...
 *
 *  T1  request a un attore inesistente  -> QakRequestFailedException(unknowndest)
 *  T2  request diretta a robotmnemo      -> QakRequestFailedException(unknowndest)
 *      (stessa risposta di T1: dall'esterno hidden e inesistente non si distinguono)
 *  T3  la stessa request inviata alla facciata robotsmart (delegate) -> reply robotstate
 */
public class TestRequestFailed {
    static final String HOST = "localhost", PORT = "8020";
    static final int TOUT = 8000;   // > 10 x 500 ms usati dal contesto prima di rifiutare

    private Interaction connect() throws Exception {
        Interaction conn = ConnectionFactory.createClientSupport23(ProtocolType.tcp, HOST, PORT);
        assertNotNull("connessione a " + HOST + ":" + PORT, conn);
        return conn;
    }

    private void expectUnknownDest(String dest) throws Exception {
        Interaction conn = connect();
        IApplMessage req = CommUtils.buildRequest("tester", "getrobotstate", "getrobotstate(0)", dest);
        try {
            QakClientUtils.requestOrFail(conn, req, TOUT);
            fail("la request a " + dest + " non doveva essere soddisfatta");
        } catch (QakRequestFailedException e) {
            CommUtils.outmagenta("TestRequestFailed | " + dest + " -> " + e.getMessage());
            assertEquals("unknowndest", e.getReason());
        } finally {
            try { conn.close(); } catch (Exception ignored) { }
        }
    }

    @Test
    public void t1_unknownActor() throws Exception { expectUnknownDest("nobodyhere"); }

    @Test
    public void t2_hiddenActor() throws Exception { expectUnknownDest("robotmnemo"); }

    @Test
    public void t3_facade() throws Exception {
        Interaction conn = connect();
        IApplMessage req = CommUtils.buildRequest("tester", "getrobotstate", "getrobotstate(0)", "robotsmart");
        IApplMessage answer = QakClientUtils.requestOrFail(conn, req, TOUT);
        CommUtils.outmagenta("TestRequestFailed | robotsmart -> " + answer);
        assertEquals("robotstate", answer.msgId());
        conn.close();
    }
}
