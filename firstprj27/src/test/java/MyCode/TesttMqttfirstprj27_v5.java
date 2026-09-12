package MyCode;

import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import it.unibo.kactor.sysUtil;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
 
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.mqtt.MqttSupport;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;


/*
 * Questo test va eseguito solo se esiste la dichiarazione
 * mqttBroker("localhost", "1883", "firstprj27rIn").
 * 
 * Emette un evento starteval con i parametri sulla topic "unibo/qak/a"
 * OPPURE sulla topic "firstprj27rIn"
 * Attiva un osservatore di eventi sulla topic "firstprj27rIn_out"
 * che potrebbe ricostruire gli stream di dati valutati
 * per fare un grafico
 * 
 */
public class TesttMqttfirstprj27_v5 {  
	private String name = "tester";
	private IApplMessage stoptevent = CommUtils.buildEvent(name, "stopeval",  "stopeval(ok)" );
	private String brokerAddr       = "tcp://localhost:1883"; //"tcp://192.168.137.1:1883"; //"tcp://192.168.1.68:1883"; //"tcp://test.mosquitto.org:1883"; //"tcp://broker.hivemq.com:1883"; //
	private ProtocolType protocol   = ProtocolType.mqtt;

	
	@BeforeClass
	public static void setup() {
 		it.unibo.ctxfirstprj27.MainCtxfirstprj27Kt.main(   ) ;	
		CommUtils.delay(1000); //wait a while before calling
		CommUtils.outmagenta("TesttMqttfirstprj27_v5  | start the (micro)service con MQTT:" + sysUtil.hasMqtt());	
		Assume.assumeTrue( sysUtil.hasMqtt() ); //deve essere vera per fare i test
 	}
 
	@After
	public void down() {
 		CommUtils.outmagenta("TesttMqttfirstprj27_v5 | down");
	}
 	
	protected void doMqtt( ) {
        Interaction conn = 
        		//new MqttInteraction("callermqtt",brokerAddr, "firstprj27rIn_out","unibo/qak/a");
				new MqttInteraction("callermqtt",brokerAddr, "firstprj27rIn_out","firstprj27rIn");
		addObservation( conn );
		addObservationUsingSupport();

		 String Min = "'-2.0'";
		 String Max = "'2.0'";
		 String Dx  = "'0.1'";
		 String args = "starteval("+Min+","+Max+","+Dx+")";
		 IApplMessage startevent = CommUtils.buildEvent(name, "starteval",  args );

		doEmitEvent(conn, startevent);
		CommUtils.delay(500); 
		
		doEmitEvent(conn, stoptevent);
		
	}
	protected void doEmitEvent(Interaction conn, IApplMessage event) {	
			try {
				conn.forward(event);  //forward anche se event (topic "unibo/qak/a" OPPURE "firstprj27rIn")
			} catch (Exception e) {
	 			CommUtils.outred("Error: "+e.getMessage());
			}		
	}
	
 
	@Test   
	public void test1Mqtt() {
		CommUtils.outgreen("=== test1Mqtt  "  );
		 doMqtt( );
//		 CommUtils.delay(600); 
		//CommUtils.outgreen("test1Mqtt result=" + result);
//		showData(result); //fare il grafico in fase di testing non è appropriato
// 		assertTrue(  checkAnswer(result)  );  
	} 
/*	
	protected boolean checkAnswer(String result) {
		Struct  t = (Struct) Term.parse(result);
		CommUtils.outblue(name + " | t=" + t.getArg(0).toString());
		//String answerValues = answer.msgContent().replaceAll("values(", "").replaceAll(")","")
		String answerValues = t.getArg(0).toString().replaceAll("'","");
		CommUtils.outyellow( "| checkAnswer answerValues=" + answerValues);
		String[] parti      = answerValues.split("###", -1); // -1 gestisce eventuali liste vuote
		CommUtils.outgreen("parti[0]  " +  parti[0]   + " parti[1]  " +  parti[1]  );
		ArrayList<String> labels = new ArrayList<>(Arrays.asList(parti[0].split(",")));
		ArrayList<String> values = new ArrayList<>(Arrays.asList(parti[1].split(",")));	  
		return labels.size() == values.size() ;
	}
 
	protected void showData(String answer) {
		Struct  t = (Struct) Term.parse(answer);
		CommUtils.outblue(name + " | t=" + t.getArg(0).toString());
		//String answerValues = answer.msgContent().replaceAll("values(", "").replaceAll(")","")
		String answerValues = t.getArg(0).toString().replaceAll("'","");
		CommUtils.outred(name + " | answerValues=" + answerValues);
		//Answer: values('-1.0,-0.2,0.6###-0.840,-0.200,0.560')
		String chartUrl = ChartUtils.buildMapChartUrl( "Funzione sin (test)", answerValues );
		ChartUtils.OpenChartInBrowser(chartUrl); 	
	}
*/	
	
	/*
	 * Gli obervers possono ricostruire lo stream dei dati (valori y)
	 * ma dovrebbero calcolare lo stream delle x in relazione ai dati emessi
	 * con l'evento startevent 
	 */
	protected void addObservationUsingSupport() {
		new Thread() {
			public void run() {
				try {
					CommUtils.outmagenta("!!!!!!!!!!! addObservation STARTS !!!!!!!!!!!!!!" );
					MqttSupport support = new MqttSupport();
					support.connectToBroker("anotherobs", "tcp://localhost:1883");
					MqttConnectionCallbackForReceive rec = new MqttConnectionCallbackForReceive("anotherobs");
					support.subscribe("firstprj27rIn_out", rec);
					CommUtils.outmagenta("subscribed ... :" );
					while(true) {
 						String m = rec.receive();
						CommUtils.outblack("anotherobs observed ... :" + m);
					}
				} catch (Exception e) {
					CommUtils.outred("callermqtt addObservation ERROR:" + e.getMessage() );
				}
				
			}
		}.start();
	}
	protected void addObservation( Interaction conn ) {
		new Thread() {
			public void run() {
				try {
					CommUtils.outmagenta("!!!!!!!!!!! addObservation STARTS !!!!!!!!!!!!!!" );
 					while(true) {
 						String m = conn.receiveMsg();
 						CommUtils.outmagenta("observed on conn ... :" + m);
					}
				} catch (Exception e) {
					CommUtils.outred("callermqtt addObservation ERROR:" + e.getMessage() );
				}
				
			}
		}.start();
	}
}
