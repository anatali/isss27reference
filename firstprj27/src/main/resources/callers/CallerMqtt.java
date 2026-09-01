package callers;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttConnection;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;

 
/*
 * La comunicazione avviene  a livello QakActor
 */
public class CallerMqtt {

	private IApplMessage reqPI_2  = CommUtils.buildRequest("testappl", "evalr", "argr("+Math.PI/2+")", "sistemas");
	private IApplMessage reqPI_6  = CommUtils.buildRequest("testappl", "evalr", "argr("+Math.PI/6+")", "sistemas");
	
	public void doJob() {
		CommUtils.outblue("callermqtt STARTS"  );
        String brokerAddr       = "tcp://broker.hivemq.com:1883"; //"tcp://192.168.137.1:1883"; //"tcp://192.168.1.68:1883"; //"tcp://test.mosquitto.org:1883"; //"tcp://broker.hivemq.com:1883"; //
        ProtocolType protocol   = ProtocolType.mqtt;
        Interaction conn = 
        		new MqttInteraction("callermqtt",brokerAddr, "topicin","unibo/qak/sistemas");
        //addObservation( conn );
        
        try {
        	
        	CommUtils.outgreen("callermqtt publish " + reqPI_2 + " on nibo/qak/sistemas" );
        	IApplMessage reply = conn.request(reqPI_2);
        	CommUtils.outmagenta("callermqtt | reply=" + reply   );
        	System.exit(0);
		} catch (Exception e) {
 			CommUtils.outred("callermqtt ERROR:" + e.getMessage() );
		}
	}
	
	protected void addObservation(Interaction conn) {
		new Thread() {
			public void run() {
				try {
					while(true) {
						String m = conn.receiveMsg();
						CommUtils.outmagenta("mqtt observed:" + m);
					}
				} catch (Exception e) {
					CommUtils.outred("callermqtt addObservation ERROR:" + e.getMessage() );
				}
				
			}
		}.start();
	}

	 public static void main( String[] args ){
		 new CallerMqtt().doJob();
	 }
} 
