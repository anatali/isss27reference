package unibo.actorgridgui.caller;

import java.util.Observable;
import unibo.basicomm23.interfaces.IObserver;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;
import unibo.basicomm23.ws.WsConnection;


public class TestClientWs implements IObserver{
    private Interaction conn;
    
    public TestClientWs() { 
        try {     	
//        	conn = ConnectionFactory.createClientSupport(ProtocolType.ws, "192.168.1.132:7110", "wsupdates");
//	        CommUtils.outyellow("TestClientWs on 7110" );	        
        	conn = ConnectionFactory.createClientSupport(ProtocolType.ws, "192.168.1.132:8085", "wsupdates");
	        CommUtils.outyellow("TestClientWs on 8085" );	        
	        ((WsConnection) conn).addObserver(this);
        } catch (Exception e) {
        	CommUtils.outred("TestClientWs | ERROR:" +e.getMessage());
        }    	   	
    }

     
    public void workWithGui( ) {
        try {
        	CommUtils.outblue("workWithGui cell");
        	//conn.forward("clear");       	
        	//CommUtils.delay(2000);     	
//        	conn.forward("stop");
        	
        	conn.forward("cell(1,2,1)");
        	conn.forward("cell(3,1,1)");
          
//            CommUtils.delay(1000);
//            CommUtils.outblue("workWithGui clear");
//            conn.forward("clear");    
            
        	CommUtils.delay(10000); //To chcek broadcasted messages
         	CommUtils.outmagenta("TestClientWs | BYE" );
            System.exit(0);
        
        } catch (Exception e) {
        	CommUtils.outred("TestClientWs | ERROR:" +e.getMessage());
        }    	
    }
    
	@Override 
	public void update(Observable o, Object arg) {
		CommUtils.outyellow("TestClientWs | riceve da observale: " + o + " info:" + arg);		
		update(arg.toString() );
	}


	@Override
	public void update(String message) {
		CommUtils.outcyan("TestClientWs | update elabora: " + message);
	}
 

    public static void main(String[] args) {
    	TestClientWs caller = new TestClientWs();
    	caller.workWithGui(); 
    }
 
} 
