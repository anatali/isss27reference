package logisticmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.kactor.sysUtil;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

/**
 * Test di integrazione JUnit4 per il mapservice QActor.
 * 
 * Testa l'interazione con il mapservice definito in logisticmap_analisi1.qak:
 * - Parametri validi -> risposta "done"
 * - Parametri invalidi (r fuori range) -> risposta "parameter_wrong"
 * - Parametri invalidi (x0 fuori range) -> risposta "parameter_wrong"
 * - Parametri invalidi (y negativo) -> risposta "parameter_wrong"
 * 
 * NOTE:
 * - Questo test assume che il sistema QAK NON usi MQTT (sysUtil.hasMqtt() == false)
 * - Le richieste vengono inviate via TCP al mapservice sulla porta 8333
 * - Ogni test invia una richiesta "evallogistic" e verifica la risposta
 */
public class MapserviceIntegrationTest {
    
    // Timeout per le operazioni di connessione
    private static final int TIMEOUT_MS = 5000;
    // Host e porta del mapservice
    private static final String HOST = "localhost";
    private static final int PORT = 8333;
    
    /**
     * Setup iniziale: avvia il sistema logisticmap
     * Viene eseguito una sola volta all'inizio di tutti i test
     */
    @BeforeClass
    public static void setup() {
        // Avvia il contesto logisticmap
        try {
            // Riflection per evitare errori di compilazione dovuti a Kotlin/generazione
            Class<?> mainClass = Class.forName("it.unibo.ctxlogisticmap.MainCtxlogisticmapKt");
            java.lang.reflect.Method mainMethod = mainClass.getMethod("main");
            mainMethod.invoke(null);
            CommUtils.outmagenta("MapserviceIntegrationTest | Sistema logisticmap avviato");
            // Aspetta un po' perché il sistema si avvii completamente
            CommUtils.delay(2000);
        } catch (Exception e) {
            CommUtils.outred("MapserviceIntegrationTest | Errore durante l'avvio del sistema");
            e.printStackTrace();
            fail("Impossibile avviare il sistema logisticmap");
        }
        
        // Assicurati che il sistema NON usi MQTT
        Assume.assumeTrue(!sysUtil.hasMqtt());
        CommUtils.outmagenta("MapserviceIntegrationTest | MQTT non richiesto, test prosegue");
    }
    
    /**
     * Cleanup dopo ogni test
     */
    @After
    public void down() {
        CommUtils.outmagenta("MapserviceIntegrationTest | down");
    }
    
    /**
     * Metodo helper per inviare una richiesta TCP al mapservice e ottenere la risposta
     * 
     * @param r parametro di controllo della mappa logistica
     * @param x0 condizione iniziale
     * @param y numero di iterazioni
     * @return il contenuto del messaggio di risposta (es. "result(done)" oppure "result(parameter_wrong)")
     */
    private String callMapservice(double r, double x0, int y) {
        // Crea il messaggio di richiesta nel formato: mmsg(evallogistic,request,tester,mapservice,args(r,x0,y),0)
        String requestStr = String.format(
            "mmsg(evallogistic,request,tester,mapservice,args(%s,%s,%d),0)",
            r, x0, y
        );
        
        CommUtils.outblue("MapserviceIntegrationTest | Invio richiesta: " + requestStr);
        
        Interaction conn = null;
        try {
            // Crea una connessione TCP verso il mapservice
            conn = ConnectionFactory.createClientSupport(
                ProtocolType.tcp,
                HOST,
                "" + PORT
            );
            
            // Crea il messaggio di applicazione
            IApplMessage reqmsg = new ApplMessage(requestStr);
            
            // Invia la richiesta e aspetta la risposta
            IApplMessage resultMsg = conn.request(reqmsg);
            String resultContent = resultMsg.msgContent();
            
            CommUtils.outgreen("MapserviceIntegrationTest | Risposta ricevuta: " + resultContent);
            return resultContent;
            
        } catch (Exception e) {
            CommUtils.outred("MapserviceIntegrationTest | Errore nella comunicazione con mapservice");
            e.printStackTrace();
            return "ERROR";
        } finally {
            // Chiudi la connessione se esiste
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    // Ignora errori nella chiusura
                }
            }
        }
    }
    
    // ===================================================================
    // TEST SUITE
    // ===================================================================
    
    /**
     * Test 1: Parametri validi (caso di convergenza)
     * Input: r=2.0, x0=0.7, y=8
     * Attesa: "result(done)" e serie di valori valutata correttamente
     */
    @Test
    public void testValidParameters_Convergence() {
        CommUtils.outgreen("=== TEST 1: Valid Parameters (Convergence) ===");
        double r = 2.0;
        double x0 = 0.7;
        int y = 8;
        
        String result = callMapservice(r, x0, y);
        
        // La risposta deve contenere "result(done)"
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 1 passato: parametri validi accettati");
    }
    
    /**
     * Test 2: Parametri validi (caso caotico r=3.8)
     * Input: r=3.8, x0=0.1, y=50
     * Attesa: "result(done)"
     */
    @Test
    public void testValidParameters_Chaotic() {
        CommUtils.outgreen("=== TEST 2: Valid Parameters (Chaotic) ===");
        double r = 3.8;
        double x0 = 0.1;
        int y = 50;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 2 passato: parametri validi (caso caotico) accettati");
    }
    
    /**
     * Test 3: Parametri validi (boundary r=0)
     * Input: r=0.0, x0=0.5, y=10
     * Attesa: "result(done)" (r=0 è dentro il range [0, 4])
     */
    @Test
    public void testValidParameters_BoundaryR_Zero() {
        CommUtils.outgreen("=== TEST 3: Valid Parameters (r=0, boundary) ===");
        double r = 0.0;
        double x0 = 0.5;
        int y = 10;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 3 passato: boundary r=0 accettato");
    }
    
    /**
     * Test 4: Parametri validi (boundary r=4)
     * Input: r=4.0, x0=0.5, y=10
     * Attesa: "result(done)" (r=4 è il limite superiore del range)
     */
    @Test
    public void testValidParameters_BoundaryR_Max() {
        CommUtils.outgreen("=== TEST 4: Valid Parameters (r=4, boundary) ===");
        double r = 4.0;
        double x0 = 0.5;
        int y = 10;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 4 passato: boundary r=4 accettato");
    }
    
    /**
     * Test 5: Parametri validi (boundary x0=0)
     * Input: r=2.0, x0=0.0, y=10
     * Attesa: "result(done)" (x0=0 è il limite inferiore)
     */
    @Test
    public void testValidParameters_BoundaryX0_Zero() {
        CommUtils.outgreen("=== TEST 5: Valid Parameters (x0=0, boundary) ===");
        double r = 2.0;
        double x0 = 0.0;
        int y = 10;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 5 passato: boundary x0=0 accettato");
    }
    
    /**
     * Test 6: Parametri validi (boundary x0=1)
     * Input: r=2.0, x0=1.0, y=10
     * Attesa: "result(done)" (x0=1 è il limite superiore)
     */
    @Test
    public void testValidParameters_BoundaryX0_One() {
        CommUtils.outgreen("=== TEST 6: Valid Parameters (x0=1, boundary) ===");
        double r = 2.0;
        double x0 = 1.0;
        int y = 10;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 6 passato: boundary x0=1 accettato");
    }
    
    /**
     * Test 7: Parametri validi (y=0)
     * Input: r=2.0, x0=0.5, y=0
     * Attesa: "result(done)"
     */
    @Test
    public void testValidParameters_Y_Zero() {
        CommUtils.outgreen("=== TEST 7: Valid Parameters (y=0) ===");
        double r = 2.0;
        double x0 = 0.5;
        int y = 0;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Risposta attesa 'result(done)', ricevuto: " + result,
            result.contains("result(done)")
        );
        CommUtils.outgreen("✓ Test 7 passato: y=0 accettato");
    }
    
    /**
     * Test 8: PARAMETRO INVALIDO - R negativo
     * Input: r=-2.0, x0=0.7, y=8
     * Attesa: "result(parameter_wrong)" (r < 0 non è ammesso)
     */
    @Test
    public void testInvalidParameter_R_Negative() {
        CommUtils.outgreen("=== TEST 8: Invalid Parameter (r=-2.0) ===");
        double r = -2.0;
        double x0 = 0.7;
        int y = 8;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Parametro r negativo deve essere rifiutato. Risposta: " + result,
            result.contains("parameter_wrong")
        );
        CommUtils.outgreen("✓ Test 8 passato: r negativo rifiutato");
    }
    
    /**
     * Test 9: PARAMETRO INVALIDO - R troppo grande
     * Input: r=4.5, x0=0.7, y=8
     * Attesa: "result(parameter_wrong)" (r > 4 non è ammesso)
     */
    @Test
    public void testInvalidParameter_R_TooLarge() {
        CommUtils.outgreen("=== TEST 9: Invalid Parameter (r=4.5) ===");
        double r = 4.5;
        double x0 = 0.7;
        int y = 8;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Parametro r > 4 deve essere rifiutato. Risposta: " + result,
            result.contains("parameter_wrong")
        );
        CommUtils.outgreen("✓ Test 9 passato: r > 4 rifiutato");
    }
    
    /**
     * Test 10: PARAMETRO INVALIDO - X0 negativo
     * Input: r=2.0, x0=-0.5, y=8
     * Attesa: "result(parameter_wrong)" (x0 < 0 non è ammesso)
     */
    @Test
    public void testInvalidParameter_X0_Negative() {
        CommUtils.outgreen("=== TEST 10: Invalid Parameter (x0=-0.5) ===");
        double r = 2.0;
        double x0 = -0.5;
        int y = 8;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Parametro x0 negativo deve essere rifiutato. Risposta: " + result,
            result.contains("parameter_wrong")
        );
        CommUtils.outgreen("✓ Test 10 passato: x0 negativo rifiutato");
    }
    
    /**
     * Test 11: PARAMETRO INVALIDO - X0 troppo grande
     * Input: r=2.0, x0=1.5, y=8
     * Attesa: "result(parameter_wrong)" (x0 > 1 non è ammesso)
     */
    @Test
    public void testInvalidParameter_X0_TooLarge() {
        CommUtils.outgreen("=== TEST 11: Invalid Parameter (x0=1.5) ===");
        double r = 2.0;
        double x0 = 1.5;
        int y = 8;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Parametro x0 > 1 deve essere rifiutato. Risposta: " + result,
            result.contains("parameter_wrong")
        );
        CommUtils.outgreen("✓ Test 11 passato: x0 > 1 rifiutato");
    }
    
    /**
     * Test 12: PARAMETRO INVALIDO - Y negativo
     * Input: r=2.0, x0=0.7, y=-5
     * Attesa: "result(parameter_wrong)" (y < 0 non è ammesso)
     */
    @Test
    public void testInvalidParameter_Y_Negative() {
        CommUtils.outgreen("=== TEST 12: Invalid Parameter (y=-5) ===");
        double r = 2.0;
        double x0 = 0.7;
        int y = -5;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Parametro y negativo deve essere rifiutato. Risposta: " + result,
            result.contains("parameter_wrong")
        );
        CommUtils.outgreen("✓ Test 12 passato: y negativo rifiutato");
    }
    
    /**
     * Test 13: PARAMETRI INVALIDI - Più errori contemporanei
     * Input: r=-1.0, x0=1.5, y=-3 (tutti e tre i parametri fuori range)
     * Attesa: "result(parameter_wrong)"
     */
    @Test
    public void testInvalidParameter_MultipleErrors() {
        CommUtils.outgreen("=== TEST 13: Invalid Parameters (multiple errors) ===");
        double r = -1.0;
        double x0 = 1.5;
        int y = -3;
        
        String result = callMapservice(r, x0, y);
        
        assertTrue(
            "Parametri multipli errati devono essere rifiutati. Risposta: " + result,
            result.contains("parameter_wrong")
        );
        CommUtils.outgreen("✓ Test 13 passato: errori multipli rifiutati");
    }
}
