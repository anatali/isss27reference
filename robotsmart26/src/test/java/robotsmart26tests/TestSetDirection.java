package robotsmart26tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

/**
 * TestSetDirection (JUnit 4)
 * ------------------------------------------------------------------
 * Verifica il comando  setdirection  del modello robotsmart26.qak:
 *
 *   Request setdirection     : dir( D )   D = up|down|left|right
 *   Reply   setdirectiondone : pos(PX,PY) for setdirection
 *
 * gestito da robotmnemo (stati dosetrobotdirection / changeDirection /
 * nextMove / setDirReply).
 *
 * "Eseguito in modo corretto" significa:
 *  P1  la risposta e' la Reply setdirectiondone, inviata da robotmnemo;
 *  P2  il payload pos(PX,PY) coincide con la posizione corrente del robot;
 *  P3  dopo la risposta, getrobotstate riporta direzione = D;
 *  P4  la rotazione non cambia la cella del robot;
 *  P5  P1..P4 valgono per ogni coppia (direzione iniziale, direzione finale):
 *      0 rotazioni (stessa direzione), 1 rotazione (90 gradi),
 *      2 rotazioni (180 gradi).
 *
 * NOTA 1. setdirection va inviata a robotmnemo: robotsmart NON la delega
 *         (in finishstart mancano  delegate setdirection to robotmnemo),
 *         quindi una request indirizzata a robotsmart non riceve risposta.
 *         robotmnemo e' nello stesso contesto (porta 8020), quindi e'
 *         raggiungibile dalla stessa connessione TCP.
 * NOTA 2. getrobotstate legge la "mappa di contesto" (rpos), non il robot
 *         fisico. Il test verifica la coerenza del modello e del protocollo;
 *         l'orientamento reale del robot si osserva su WEnv.
 * NOTA 3. Il test non usa setrobotstate per fissare la direzione iniziale:
 *         setrobotstate cambia solo la mente (rpos) e la renderebbe
 *         incoerente con il robot fisico. Si parte dallo stato corrente.
 *
 * Prerequisito: sistema attivo (es. docker-compose -f robotsmart26.yaml up).
 * Parametri opzionali: -Drobotsmart.host=...  -Drobotsmart.port=...
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSetDirection {

    private static final String HOST       = System.getProperty("robotsmart.host", "192.168.1.132");
    private static final String PORT       = System.getProperty("robotsmart.port", "8020");
    private static final String CALLER     = "testsetdir";
    private static final String RECEIVER   = "robotmnemo";
    private static final int    TIMEOUT_MS = 10000;   //3 rotazioni con delay: ampiamente sufficiente

    private static final String[] DIRS = { "up", "down", "left", "right" };

    /*
     * Sequenza che, partendo da down, attraversa TUTTE le 16 coppie ordinate
     * (da, a) di direzioni, incluse le 4 coppie (D,D).
     * E' un circuito euleriano sul grafo completo con cappi: ogni arco una volta.
     */
    private static final String[] TOUR = {
        "down", "down", "left", "left", "up", "up", "right", "right",
        "down", "up", "left", "right", "up", "down", "right", "left", "down"
    };

    private static Interaction     conn;
    private static ExecutorService executor;
    private static String          initialDir;

    /* ================================================================
     * Setup / teardown
     * ================================================================ */
    @BeforeClass
    public static void connect() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        conn = ConnectionFactory.createClientSupport23(ProtocolType.tcp, HOST, PORT);
        assertNotNull("Impossibile connettersi a " + HOST + ":" + PORT
                      + " (il sistema robotsmart26 e' attivo?)", conn);
        initialDir = getRobotState()[2];
        CommUtils.outcyan("TestSetDirection | stato iniziale direzione=" + initialDir);
    }

    @AfterClass
    public static void restoreAndClose() throws Exception {
        try {
            if (conn != null && initialDir != null) setDirection(initialDir);
        } finally {
            if (conn != null) conn.close();
            if (executor != null) executor.shutdownNow();
        }
    }

    /* ================================================================
     * Test
     * ================================================================ */

    /** P1, P2: forma della risposta e posizione riportata. */
    @Test
    public void t01_replyIsSetdirectiondoneWithCurrentPosition() throws Exception {
        String[] before = getRobotState();                 // {X, Y, DIR}
        CommUtils.outmagenta("TestSetDirection | stato prima di setdirection=" + before[0] + "," + before[1] + "," + before[2]);
        IApplMessage reply = setDirection("left");

        assertEquals("tipo di messaggio",  "reply",            reply.msgType());
        assertEquals("id della reply",     "setdirectiondone", reply.msgId());
        assertEquals("mittente della reply", RECEIVER,         reply.msgSender());

        Struct pos = parse(reply.msgContent());
        CommUtils.outcyan("TestSetDirection | pos=" + pos.toString());
        assertEquals("funtore del payload", "pos", pos.getName());
        assertEquals("arita' del payload",  2,     pos.getArity());
        assertEquals("PX nella reply", before[0], pos.getArg(0).toString());
        assertEquals("PY nella reply", before[1], pos.getArg(1).toString());
    }

    /** P3, P4: per ogni direzione D la mente riporta D e la stessa cella. */
//    @Test
    public void t02_eachDirectionIsReached() throws Exception {
        String[] start = getRobotState();
        for (String d : DIRS) {
            checkTransition(start[0], start[1], d);
        }
    }

    /** P5: tutte le 16 coppie (direzione iniziale, direzione finale). */
//    @Test
    public void t03_allTransitionsBetweenDirections() throws Exception {
        //verifica della sequenza stessa: deve coprire 16 coppie distinte
        Set<String> pairs = new HashSet<>();
        for (int i = 0; i + 1 < TOUR.length; i++) pairs.add(TOUR[i] + "->" + TOUR[i + 1]);
        assertEquals("la sequenza TOUR deve coprire tutte le coppie", 16, pairs.size());

        String[] start = getRobotState();
        for (String d : TOUR) {
            checkTransition(start[0], start[1], d);
        }
    }

    /** P4 in forma ripetuta: molte rotazioni non introducono deriva nella cella. */
//    @Test
    public void t04_repeatedRotationsDoNotChangeCell() throws Exception {
        String[] start = getRobotState();
        for (int lap = 0; lap < 3; lap++) {
            for (String d : new String[] { "left", "up", "right", "down" }) {
                setDirection(d);
            }
        }
        String[] end = getRobotState();
        assertEquals("X dopo 12 rotazioni", start[0], end[0]);
        assertEquals("Y dopo 12 rotazioni", start[1], end[1]);
        assertEquals("direzione finale",    "down",   end[2]);
    }

    /* ================================================================
     * Utilities
     * ================================================================ */

    /** Esegue setdirection(d) e verifica P1..P4 rispetto alla cella (x,y). */
    private static void checkTransition(String x, String y, String d) throws Exception {
        String from = getRobotState()[2];
        IApplMessage reply = setDirection(d);
        assertEquals(from + "->" + d + ": id reply", "setdirectiondone", reply.msgId());

        Struct pos = parse(reply.msgContent());
        assertEquals(from + "->" + d + ": PX nella reply", x, pos.getArg(0).toString());
        assertEquals(from + "->" + d + ": PY nella reply", y, pos.getArg(1).toString());

        String[] after = getRobotState();
        assertEquals(from + "->" + d + ": direzione",  d, after[2]);
        assertEquals(from + "->" + d + ": X invariata", x, after[0]);
        assertEquals(from + "->" + d + ": Y invariata", y, after[1]);
        CommUtils.outgreen("TestSetDirection | " + from + " -> " + d + " ok  pos(" + x + "," + y + ")");
    }

    /** Request setdirection : dir(D) a robotmnemo. */
    private static IApplMessage setDirection(String d) throws Exception {
        IApplMessage req = CommUtils.buildRequest(CALLER, "setdirection", "dir(" + d + ")", RECEIVER);
        return requestWithTimeout(req);
    }

    /** Request getrobotstate; restituisce {X, Y, DIR} da robotstate(pos(X,Y),DIR). */
    private static String[] getRobotState() throws Exception {
        IApplMessage req = CommUtils.buildRequest(CALLER, "getrobotstate", "getrobotstate(test)", RECEIVER);
        IApplMessage reply = requestWithTimeout(req);
        assertEquals("id reply di getrobotstate", "robotstate", reply.msgId());

        Struct st = parse(reply.msgContent());
        assertEquals("funtore di robotstate", "robotstate", st.getName());
        Struct pos = (Struct) st.getArg(0).getTerm();
        String dir = st.getArg(1).toString();
        assertTrue("direzione non valida: " + dir, isDirection(dir));
        return new String[] { pos.getArg(0).toString(), pos.getArg(1).toString(), dir };
    }

    /**
     * Invia una request e attende la reply al piu' TIMEOUT_MS.
     * Il timeout evita che il test resti bloccato se la reply non arriva
     * (es. request inviata a un attore che non la gestisce).
     */
    private static IApplMessage requestWithTimeout(IApplMessage req) throws Exception {
        Future<IApplMessage> f = executor.submit(() -> conn.request(req));
        CommUtils.outgreen("TestSetDirection | requestWithTimeout "  + req.msgId() + " inviata a " + req.msgReceiver());
       try {
            IApplMessage reply = f.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            CommUtils.outgreen("TestSetDirection |  reply ricevuta da " + reply );
            assertNotNull("reply nulla per " + req.msgId(), reply);
            return reply;
        } catch (TimeoutException e) {
            f.cancel(true);
            fail("nessuna reply a " + req + " entro " + TIMEOUT_MS + " ms");
            return null;   //non raggiunto
        }
    }

    private static Struct parse(String content) {
        return (Struct) Term.createTerm(content);
    }

    private static boolean isDirection(String d) {
        for (String x : DIRS) if (x.equals(d)) return true;
        return false;
    }
}
