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
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;


/*
 * Questo test va eseguito SOLO SE ESISTE la dichiarazione
 * mqttBroker("localhost", "1883", "firstprj27rIn").
 * 
 * FUNZIONA ANCHE CON firstprj27_v5
 * Invia una request evalfunvalues via MQTT sulla topic "unibo/qak/a"
 * 
 */
public class TesttMqttfirstprj27_v4 {  
	private String name = "tester";
	
	@BeforeClass
	public static void setup() {
 		it.unibo.ctxfirstprj27.MainCtxfirstprj27Kt.main(   ) ;	
		CommUtils.delay(1000); //wait a while before calling
		CommUtils.outmagenta("TesttMqttfirstprj27_v4  | start the (micro)service con MQTT:" + sysUtil.hasMqtt());	
		
		Assume.assumeTrue( sysUtil.hasMqtt() ); //deve essere vera per fare i test
 	}
 
	@After
	public void down() {
 		CommUtils.outmagenta("TesttMqttfirstprj27_v4 | down");
	}
 	
	protected String  callMqtt( ) {
        String brokerAddr       = "tcp://localhost:1883"; //"tcp://192.168.137.1:1883"; //"tcp://192.168.1.68:1883"; //"tcp://test.mosquitto.org:1883"; //"tcp://broker.hivemq.com:1883"; //
        ProtocolType protocol   = ProtocolType.mqtt;
        Interaction conn = 
        		new MqttInteraction("callermqtt",brokerAddr, "firstprj27rIn_out","unibo/qak/a");
		return docall(conn);
	}
 	
	protected String docall(Interaction conn) {		 
 		CommUtils.outyellow( "| docall=" + conn);		
		 String Min = "'-3.0'";
		 String Max = "'3.0'";
		 String Dx  = "'0.2'";
		 String args = "args("+Min+","+Max+","+Dx+")";
		 IApplMessage evalRequest = CommUtils.buildRequest(name, "evalfunvalues",  args, "a");
		try {
			IApplMessage result   = conn.request(evalRequest);
			//String result       = answer.msgContent();
			String answer = result.msgContent() ;
			CommUtils.outyellow( "| docall answer=" + answer);
			return answer	;
		} catch (Exception e) {
 			return "fail";
		}
	}
	
	@Test   
	public void test1Mqtt() {
		CommUtils.outgreen("=== test1Mqtt  "  );
		String result = callMqtt( );
		//CommUtils.outgreen("test1Mqtt result=" + result);
		showData(result); //fare il grafico in fase di testing non è appropriato
 		assertTrue(  checkAnswer(result)  );  
	} 
	
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
	
}
