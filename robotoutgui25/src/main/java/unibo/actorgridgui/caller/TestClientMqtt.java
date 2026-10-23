package unibo.actorgridgui.caller;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.utils.CommUtils;


//@ClientEndpoint
public class TestClientMqtt {
    private Interaction conn;
    
    public TestClientMqtt() { 
        try {     	
    		String broker = System.getenv("MQTTBROKER_URL");
    		if( broker == null ) {
    			//String lcoalAddr = CommUtils.getServerLocalIp();
    			broker = "tcp://192.168.1.132:1883";   //in locale outof docker  Con localhost NON VA   			
    			CommUtils.outgreen(  "TestClientMqtt |  outside docker " + broker  );			
    		} 
    		else {
    			broker = "tcp://HOST:1883".replace("HOST", broker);
    			CommUtils.outgreen( "wsactorguimqtt | useMqttInteraction in docker to " + broker ); 	
    		}					
	        
	        conn = new MqttInteraction(  "clientMqtt", broker, "actorapplin", "guiin" );
	        CommUtils.outyellow("TestClientMqtt " + conn );	 
	        
	        
			new Thread() {
				public void run() { //activateReceive(); NECESSARIO SE Life non usa WS
					try {
						CommUtils.outblue( "TestClientMqtt |  activate receiver ..."  );
						while (true) {
		 					String msg = "";
							msg = conn.receiveMsg();
	 						CommUtils.outblue("TestClientMqtt |  receives via mqtt: " + msg);
							//elabMsgMqtt( msg );
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();		
	        
	        
	         
        } catch (Exception e) {
        	CommUtils.outred("clientMqtt | ERROR:" +e.getMessage());
        }    	   	
    }

     
    public void workWithGui( ) {
        try {
        	CommUtils.outblue("workWithGui cell");
        	//conn.forward("clear");       	
        	//CommUtils.delay(2000);     	
//        	conn.forward("stop");
        	
        	conn.forward("cell(0,0,1)");
        	conn.forward("cell(1,1,1)");
          
//            CommUtils.delay(1000);
//            CommUtils.outblue("workWithGui clear");
//            conn.forward("clear");    
            
        	CommUtils.delay(10000); //To chcek broadcasted messages
         	CommUtils.outmagenta("clientMqtt | BYE" );
            System.exit(0);
        
        } catch (Exception e) {
        	CommUtils.outred("clientMqtt | ERROR:" +e.getMessage());
        }    	
    }
    
 

    public static void main(String[] args) {
    	TestClientMqtt caller = new TestClientMqtt();
    	caller.workWithGui(); 
    }
 
} 
