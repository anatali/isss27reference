package MyCode;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import it.unibo.kactor.sysUtil;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

public class TesttTCPfirstprj27_v3 {  

 
	private String requestStr = "mmsg(evalfun,request,tester,a,arg(VX),0)";
	private String Min = "'-3.0'";
	private String Max = "'3.0'";
	private String Dx  = "'0.2'";
	private String args = "args("+Min+","+Max+","+Dx+")";
	private IApplMessage evalRequest = CommUtils.buildRequest("tester", "evalfunvalues",  args, "a");

	@BeforeClass
	public static void setup() {
		it.unibo.ctxfirstprj27.MainCtxfirstprj27Kt.main(   ) ;				
 		CommUtils.delay(1000); //wait a while before calling
		CommUtils.outmagenta("Testtfirstprj27  | start the (micro)service - MQTT:" + sysUtil.hasMqtt());	
 	}
 
	@After
	public void down() {
 		CommUtils.outmagenta("Testtfirstprj27 | down");
	}
 	
	protected String  callTcp(IApplMessage req) {
		Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.tcp, "localhost", "8120");
		return docall(conn, req);
	}
	protected String callCoap(IApplMessage req) {
		Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.coap, "localhost:8120", "ctxfirstprj27/a");
		return docall(conn, req);
	}
	
	protected String docall(Interaction conn, IApplMessage req) {		 
		try {
			IApplMessage result   = conn.request(req);
			CommUtils.outyellow( "| docall answer=" + result);
			return result.msgContent();
		} catch (Exception e) {
 			return "fail";
		}
	}
	
	//da rivedere nelle evoluzioni successive
	@Test   
	public void test1Tcp_v3() {
		CommUtils.outgreen("======================= test1Tcp_v3  "  );
 		String result = callTcp(evalRequest);
		CommUtils.outgreen("test1Tcp result=" + result);
		boolean b = checkAnswerStructure( result );
 		assertTrue( b );
	} 
 
	@Test   
	public void test1Coap_v3() {
		CommUtils.outgreen("======================= test1Coap_v3  "  );
		String result = callCoap(evalRequest);
		CommUtils.outgreen("test1Coap result=" + result);
		boolean b = checkAnswerStructure( result );
 		assertTrue( b );
	} 

	protected boolean checkAnswerStructure(String result) {
		Struct  t = (Struct) Term.parse(result);
		CommUtils.outblue( "tester | checkAnswerStructure " ); //+ t.getArg(0).toString()
		String answerValues = t.getArg(0).toString().replaceAll("'","");
		//CommUtils.outyellow("tester | checkAnswerStructure answerValues=" + answerValues);
		String[] parti      = answerValues.split("###", -1); // -1 gestisce eventuali liste vuote
		//CommUtils.outgreen("tester | parti[0]  " +  parti[0]   + " parti[1]  " +  parti[1]  );
		ArrayList<String> labels = new ArrayList<>(Arrays.asList(parti[0].split(",")));
		ArrayList<String> values = new ArrayList<>(Arrays.asList(parti[1].split(",")));	  
		return labels.size() == values.size() ;
	}
}
