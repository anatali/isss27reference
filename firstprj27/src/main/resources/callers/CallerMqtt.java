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
public class CallerMqtt {


	private String name = "callermqtt";
	private String Min = "'-3.0'";
	private String Max = "'3.0'";
	private String Dx  = "'0.2'";
	private String args = "args("+Min+","+Max+","+Dx+")";
	private IApplMessage evalRequest = CommUtils.buildRequest(name, "evalfunvalues",  args, "a");
	
	public void doJob() {
		CommUtils.outblue(name + "  STARTS"  );
        String brokerAddr       = "tcp://localhost:1883"; //"tcp://192.168.137.1:1883"; //"tcp://192.168.1.68:1883"; //"tcp://test.mosquitto.org:1883"; //"tcp://broker.hivemq.com:1883"; //
        ProtocolType protocol   = ProtocolType.mqtt;
        Interaction conn = 
        		new MqttInteraction("callermqtt",brokerAddr, "topicin","unibo/qak/a");
        //addObservation( conn );
        
        try {      	
        	CommUtils.outgreen(name + " | publish " + evalRequest + " on nibo/qak/a" );
        	IApplMessage reply = conn.request(evalRequest);
        	CommUtils.outmagenta(name + " | reply=" + reply   );
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
		 new CallerMqttWithChart().doJob();
	 }
} 
