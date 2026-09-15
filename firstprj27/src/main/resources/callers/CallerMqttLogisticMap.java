package callers;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttConnection;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;

 
/*
 * La comunicazione avviene  a livello MQTT
 */
public class CallerMqttLogisticMap {


	private String name = "callermqtt";
 	
	public void doJob() {
		CommUtils.outblue(name + "  STARTS"  );
        String brokerAddr       = "ws://localhost:9001"; //"tcp://192.168.137.1:1883"; //"tcp://192.168.1.68:1883"; //"tcp://test.mosquitto.org:1883"; //"tcp://broker.hivemq.com:1883"; //
//        ProtocolType protocol   = ProtocolType.mqtt;
        MqttConnection conn = 
        		new MqttConnection("callermqtt",brokerAddr, "topicin","logistic/r");
         
        try {      	
        	CommUtils.outgreen(name + " | publish r"   );
        	conn.publish("logistic/r", "3.5");
        	CommUtils.outmagenta(name + " | done="    );
        	System.exit(0);
		} catch (Exception e) {
 			CommUtils.outred("callermqtt ERROR:" + e.getMessage() );
		}
	}
	
//	protected void addObservation(Interaction conn) {
//		new Thread() {
//			public void run() {
//				try {
//					while(true) {
//						String m = conn.receiveMsg();
//						CommUtils.outmagenta("mqtt observed:" + m);
//					}
//				} catch (Exception e) {
//					CommUtils.outred("callermqtt addObservation ERROR:" + e.getMessage() );
//				}
//				
//			}
//		}.start();
//	}

	 public static void main( String[] args ){
		 new CallerMqttLogisticMap().doJob();
	 }
} 
