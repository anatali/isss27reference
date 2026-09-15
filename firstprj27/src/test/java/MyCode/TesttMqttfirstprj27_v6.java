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
import unibo.basicomm23.coap.CoapConnection;
import unibo.basicomm23.coap.CoapInteraction;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.Connection;
import unibo.basicomm23.utils.ConnectionFactory;

public class TesttMqttfirstprj27_v6 {
	private String name = "testercoap";
	
	@BeforeClass
	public static void setup() {
// 		CommUtils.outmagenta("TesttMqttfirstprj27_v6  | start the (micro)service con CoAP:" + sysUtil.hasMqtt());	
		Assume.assumeTrue( ! sysUtil.hasMqtt() ); //deve essere vera per fare i test

		it.unibo.ctxfirstprj27.MainCtxfirstprj27Kt.main(   ) ;	
//		CommUtils.delay(1000); //wait a while before calling
		CommUtils.outmagenta("TesttMqttfirstprj27_v6  | start the (micro)service con CoAP:" + sysUtil.hasMqtt());	
		
 	}

	@After
	public void down() {
 		CommUtils.outmagenta("TesttMqttfirstprj27_v6 | down");
	}

	protected String doRequestUsingCoap( ) {
		//CoapConnection conn = new CoapConnection("localhost:8120", "ctxfirstprj27/a" );
        //conn.trace = true;
        
        Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.coap, "localhost:8120", "ctxfirstprj27/a");
        
        CommUtils.outmagenta("TesttMqttfirstprj27_v6 | doRequestUsingCoap connection DONE");
 
		 String Min = "'-2.0'";
		 String Max = "'2.0'";
		 String Dx  = "'0.1'";
		 String args = "args("+Min+","+Max+","+Dx+")";
		 IApplMessage evalRequest = CommUtils.buildRequest(name, "evalfunvalues",  args, "a");
 
			try {
				IApplMessage result   = conn.request(evalRequest);
				conn.forward(evalRequest);
				//String result       = answer.msgContent();
				String answer = result.msgContent() ;
				CommUtils.outyellow( "| docall answer=" + answer);
 				return answer	;
			} catch (Exception e) {
 	 			return "fail";
			}
	}
	
	@Test   
	public void testCoap() {
		CommUtils.outgreen("=== testCoap  "  );
 		String result = doRequestUsingCoap( );
// 		showData(result); //fare il grafico in fase di testing non è appropriato
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
