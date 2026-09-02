package MyCode;

import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

public class Testtfirstprj27 {  
	//private IApplMessage evalRequest = CommUtils.buildRequest("tester", "evalfun",  "arg(0)", "a");
	/*
	 * Definiamo il messaggio come string per poter modificare VX
	 */
	private String requestStr = "mmsg(evalfun,request,tester,a,arg(VX),0)";

	@BeforeClass
	public static void setup() {
		CommUtils.outmagenta("Testtfirstprj27  | start the (micro)service ");	
		it.unibo.ctxfirstprj27.MainCtxfirstprj27Kt.main(   ) ;		
		CommUtils.delay(1000); //wait a while before calling
 	}
 
	@After
	public void down() {
 		CommUtils.outmagenta("Testtfirstprj27 | down");
	}
 	
	protected String  callTcp(String req) {
		Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.tcp, "localhost", "8120");
		return docall(conn, req);
	}
	protected String callCoap(String req) {
		Interaction conn = ConnectionFactory.createClientSupport(ProtocolType.coap, "localhost:8120/ctxfirstprj27/a", "");
		return docall(conn, req);
	}
	
	protected String docall(Interaction conn, String req) {		 
		IApplMessage reqmsg = new ApplMessage(req);
		CommUtils.outyellow( "| docall=" + req);		
		try {
			IApplMessage result   = conn.request(reqmsg);
			//String result       = answer.msgContent();
			CommUtils.outyellow( "| docall answer=" + result);
			return result.msgContent();
		} catch (Exception e) {
 			return "fail";
		}
	}
	
	@Test   
	public void test1Tcp() {
		CommUtils.outgreen("=== test1Tcp  "  );
		String req = requestStr.replaceAll("VX", "0");
		CommUtils.outgreen("test1Tcp req=" + req);
		String result = callTcp(req);
		CommUtils.outgreen("test1Tcp result=" + result);
		assertTrue( result.equals("value(0.0)") );
	} 
 
	@Test   
	public void test1Coap() {
		CommUtils.outgreen("=== test1Coap  "  );
		String req = requestStr.replaceAll("VX", ""+Math.PI/2);
		CommUtils.outgreen("test1Coap request=" + req);
		String result = callCoap(req);
		CommUtils.outgreen("test1Coap result=" + result);
		assertTrue( result.equals("value(1.0)") );
	} 
	
}
