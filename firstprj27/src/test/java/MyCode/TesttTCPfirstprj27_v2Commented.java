package MyCode;

import static org.junit.Assert.assertTrue;
import org.junit.After;
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
 * COMMENTED BY CLAUDE
 * Classe di test JUnit che verifica il funzionamento di un (micro)servizio
 * "ctxfirstprj27" esponendo lo stesso endpoint applicativo su due protocolli
 * di trasporto diversi: TCP e CoAP.
 *
 * Il servizio remoto valuta una funzione  e
 * restituisce il risultato incapsulato in un messaggio applicativo del tipo
 * "value(<numero>)".
 *
 * L'architettura sottostante (pacchetti unibo.basicomm23 / it.unibo.kactor)
 * suggerisce un sistema ad attori (kactor) che comunica tramite messaggi
 * applicativi (IApplMessage) trasportabili indifferentemente su più protocolli
 * (TCP, CoAP, e potenzialmente MQTT, vedi sysUtil.hasMqtt()).
 */
public class TesttTCPfirstprj27_v2Commented {  

	// Versione "builder" del messaggio di richiesta, tenuta come riferimento/commento:
	// mostra come si potrebbe costruire il messaggio in modo più strutturato
	// invece che come stringa grezza.
	//private IApplMessage evalRequest = CommUtils.buildRequest("tester", "evalfun",  "arg(0)", "a");

	/*
	 * Il messaggio di richiesta viene tenuto come stringa (anziché come oggetto
	 * già costruito) proprio per poter sostituire dinamicamente il placeholder
	 * "VX" con il valore numerico dell'argomento da inviare al servizio,
	 * generando così richieste diverse a runtime senza dover ricostruire
	 * l'oggetto messaggio ogni volta.
	 *
	 * Formato del messaggio applicativo (stile "term" simil-Prolog/Erlang):
	 *   mmsg( <funzione>, <tipo msg>, <mittente>, <destinatario>, <argomenti>, <id/seq> )
	 *   - evalfun : nome della funzione/operazione richiesta al servizio
	 *   - request : tipo di messaggio (richiesta, non risposta)
	 *   - tester  : identificativo del mittente (questo client di test)
	 *   - a       : identificativo del destinatario/attore sul lato server
	 *   - arg(VX) : argomento della funzione, VX è il placeholder da sostituire
	 *   - 0       : probabilmente un id di correlazione/sequenza del messaggio
	 */
	private String requestStr = "mmsg(evalfun,request,tester,a,arg(VX),0)";

	/**
	 * Setup eseguito UNA SOLA VOLTA prima di tutti i test della classe
	 * (annotazione @BeforeClass -> metodo statico, per contratto JUnit).
	 *
	 * Avvia il servizio/contesto applicativo "ctxfirstprj27" che poi i test
	 * andranno a interrogare via TCP/CoAP sulla porta 8120.
	 */
	@BeforeClass
	public static void setup() {
		// Avvia il main del progetto Kotlin che inizializza il sistema di attori
		// e mette in ascolto il servizio sui protocolli configurati (TCP/CoAP/MQTT).
		it.unibo.ctxfirstprj27.MainCtxfirstprj27Kt.main(   ) ;				

		// Attesa "difensiva" di 1 secondo: dà tempo al servizio di completare
		// l'inizializzazione (bind delle porte, avvio thread, ecc.) prima che
		// i test comincino a inviare richieste, evitando "connection refused".
 		CommUtils.delay(1000); //wait a while before calling

		// Log informativo (colorato in magenta dalla utility CommUtils) che
		// conferma l'avvio del servizio e riporta se il supporto MQTT è attivo.
		CommUtils.outmagenta("Testtfirstprj27  | start the (micro)service - MQTT:" + sysUtil.hasMqtt());	
 	}
 
	/**
	 * Eseguito dopo OGNI singolo metodo di test (annotazione @After).
	 * Qui si limita a loggare la chiusura del test; non effettua un vero
	 * e proprio shutdown del servizio (che resta attivo per gli altri test
	 * della classe, dato che è stato avviato una sola volta in @BeforeClass).
	 */
	@After
	public void down() {
 		CommUtils.outmagenta("Testtfirstprj27 | down");
	}
 	
	/**
	 * Apre una connessione client TCP verso il servizio in ascolto su
	 * "localhost:8120" e inoltra la richiesta testuale passata come parametro.
	 *
	 * @param req messaggio applicativo (stringa) da inviare
	 * @return il contenuto della risposta ricevuta dal servizio
	 */
	protected String  callTcp(String req) {
		Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.tcp, "localhost", "8120");
		return docall(conn, req);
	}

	/**
	 * Apre una connessione client CoAP verso lo stesso servizio, ma
	 * indirizzando la richiesta alla risorsa "ctxfirstprj27/a" (path CoAP),
	 * a dimostrazione che lo stesso servizio applicativo è raggiungibile
	 * anche con un protocollo IoT-oriented come CoAP.
	 *
	 * @param req messaggio applicativo (stringa) da inviare
	 * @return il contenuto della risposta ricevuta dal servizio
	 */
	protected String callCoap(String req) {
		Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.coap, "localhost:8120", "ctxfirstprj27/a");
		return docall(conn, req);
	}
	
	/**
	 * Metodo di supporto comune a entrambi i protocolli: incapsula la stringa
	 * di richiesta in un IApplMessage, la invia in modalità sincrona
	 * (request/response) tramite l'oggetto Interaction fornito, e ne
	 * restituisce il contenuto testuale.
	 *
	 * In caso di eccezione (es. timeout, connessione rifiutata, errore di
	 * protocollo) il metodo non propaga l'errore ma restituisce la stringa
	 * "fail", che i test possono poi confrontare esplicitamente se necessario.
	 *
	 * @param conn connessione/interazione già configurata per un protocollo specifico
	 * @param req  messaggio applicativo da inviare, in formato stringa
	 * @return il contenuto della risposta, oppure "fail" se la richiesta non va a buon fine
	 */
	protected String docall(Interaction conn, String req) {		 
		IApplMessage reqmsg = new ApplMessage(req);
		CommUtils.outyellow( "| docall=" + req);		
		try {
			// Invio sincrono: il thread resta bloccato finché non arriva la risposta
			// (o scatta un eventuale timeout/errore gestito internamente da Interaction).
			IApplMessage result   = conn.request(reqmsg);
			CommUtils.outyellow( "| docall answer=" + result);
			return result.msgContent();
		} catch (Exception e) {
			// Gestione "silenziosa" dell'errore: si perde il dettaglio dell'eccezione
			// (nessun log dello stack trace), utile da rivedere per il debugging
			// in caso di fallimenti intermittenti dei test.
 			return "fail";
		}
	}
	
	// TODO: da rivedere per evoluzioni successive
	// (es. copertura di più valori, gestione esplicita del caso "fail",
	//  parametrizzazione del test invece di duplicare test1Tcp/test1Coap)
	@Test   
	public void test1Tcp() {
		CommUtils.outgreen("======================= test1Tcp  "  );

		// Sostituisce il placeholder VX con "0": ci si aspetta quindi che
		// il servizio, valutando la funzione con argomento 0, risponda "value(0.0)".
		String req = requestStr.replaceAll("VX", "0");
		CommUtils.outgreen("test1Tcp req=" + req);

		String result = callTcp(req);
		CommUtils.outgreen("test1Tcp result=" + result);

		// Asserzione: il risultato atteso per argomento 0 è esattamente "value(0.0)"
		// (coerente ad esempio con una funzione seno: sin(0) = 0).
		assertTrue( result.equals("value(0.0)") );
	} 
 
	@Test   
	public void test1Coap() {
		CommUtils.outgreen("Testtfirstprj27.java test1Coap  "  );

		// Stesso test logico del precedente, ma via CoAP e con argomento PI/2:
		// ci si aspetta "value(1.0)" (coerente con sin(PI/2) = 1).
		String req = requestStr.replaceAll("VX", ""+Math.PI/2);
		CommUtils.outgreen("test1Coap request=" + req);

		String result = callCoap(req);
		CommUtils.outgreen("test1Coap result=" + result);

		assertTrue( result.equals("value(1.0)") );
	} 
	
}