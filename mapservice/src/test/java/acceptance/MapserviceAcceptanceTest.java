package acceptance;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.BeforeClass;
import org.junit.Test;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

/*
================================================================================
R6 · Test di accettazione (uno per scenario di features/mapservice.feature)
--------------------------------------------------------------------------------
Usano SOLO ciò che il modello dichiara: nomi dei messaggi, struttura dei payload,
esiti possibili. Gli stessi test si rieseguono, senza modifiche, contro il modello
di analisi e poi contro il sistema finale.

Esito atteso contro src/mapservice_requisiti.qak (comportamento fittizio):
  contractIsPublished       VERDE
  fixedPointScenario        ROSSO  (la pagina restituita non esiste: PageReader, Q4)
  wrongParamsScenario       ROSSO  (il modello risponde sempre done: validazione da modellare)
  notNumericParamsScenario  ROSSO  (idem)
  chaoticRegimeScenario     ROSSO  (nessuna traiettoria osservabile)
I rossi sono l'informazione voluta. Il task Gradle 'acceptanceTest' li esegue
senza far fallire la build (ignoreFailures); il task 'test' li esclude.
================================================================================
*/
public class MapserviceAcceptanceTest {

    static final String HOST = "localhost", PORT = "8333";     // Context dichiarato nel modello
    static Interaction conn;

    @BeforeClass
    public static void setup() throws Exception {
        it.unibo.ctxmapservice.MainCtxmapserviceKt.main();      // codice generato dal modello .qak attivo
        CommUtils.delay(1500);
        conn = ConnectionFactory.createClientSupport(ProtocolType.tcp, HOST, PORT);
    }

    private IApplMessage evallogistic(String args) throws Exception {
        return conn.request(CommUtils.buildRequest("tester", "evallogistic", args, "mapservice"));
    }

    /** RQ1 · Il contratto è pubblicato dal Context (GET /info.json?actor=mapservice). */
    @Test(timeout = 5000)
    public void contractIsPublished() throws Exception {
        String info = httpGet("http://" + HOST + ":" + PORT + "/info.json?actor=mapservice").body();
        assertTrue("evallogistic non dichiarato",  info.contains("evallogistic"));
        assertTrue("esiti non dichiarati", info.contains("logisticdone") && info.contains("logisticfailed"));
    }

    /** Scenario: convergenza a un punto fisso (D1, D4, D6, D7, D10). */
    @Test(timeout = 5000)
    public void fixedPointScenario() throws Exception {
        IApplMessage reply = evallogistic("args(2.0,0.7,8)");
        assertEquals("logisticdone", reply.msgId());                        // esito di successo

        URI page = Payloads.url(reply);                                     // done(URL)
        HttpResponse<String> r = httpGet(page.toString());
        assertEquals("la pagina " + page + " non è consultabile", 200, r.statusCode());   // RQ4
        // TODO(Q4): PageReader.fromRemoteBrowser(page) -> Trajectory, poi
        //   assertEquals(9, t.values().length);  assertEquals(0.5, t.values()[8], 1e-6);
        fail("PageReader non ancora definito: come si osserva la pagina 'da un'altra macchina' (Q4)?");
    }

    /** Scenario: parametri non validi (D3, D10). */
    @Test(timeout = 5000)
    public void wrongParamsScenario() throws Exception {
        IApplMessage reply = evallogistic("args(-2.0,0.7,8)");
        assertEquals("logisticfailed", reply.msgId());
        assertTrue(reply.msgContent().contains("wrongparams"));
    }

    /** Scenario: parametri non numerici (D3, D10). */
    @Test(timeout = 5000)
    public void notNumericParamsScenario() throws Exception {
        IApplMessage reply = evallogistic("args(abc,0.7,8)");
        assertEquals("logisticfailed", reply.msgId());
        assertTrue(reply.msgContent().contains("wrongparams"));
    }

    /** Scenario: regime caotico (D7, D8 provvisorio). */
    @Test(timeout = 5000)
    public void chaoticRegimeScenario() throws Exception {
        IApplMessage reply = evallogistic("args(3.8,0.1,200)");
        assertEquals("logisticdone", reply.msgId());
        // TODO(D8): leggere i 201 valori dalla pagina e confrontare i primi 20 con il riferimento
        fail("criterio di correttezza (D8) e lettura della pagina (Q4) non ancora definiti");
    }

    private static HttpResponse<String> httpGet(String url) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
