package callers;

import MyCode.ChartUtils;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;

 
/*
 * La comunicazione avviene  a livello QakActor
 */
public class CallerMqttWithChart {

//	private IApplMessage reqPI_2  = CommUtils.buildRequest("testappl", "evalr", "argr("+Math.PI/2+")", "sistemas");
//	private IApplMessage reqPI_6  = CommUtils.buildRequest("testappl", "evalr", "argr("+Math.PI/6+")", "sistemas");

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
        		new MqttInteraction("callermqtt",brokerAddr, "firstprj27rIn_out","unibo/qak/a");
        addObservation( conn );
        
        try {
        	
        	CommUtils.outgreen(name + " | publish " + evalRequest + " on unibo/qak/a" );
        	IApplMessage reply = conn.request(evalRequest);
//        	CommUtils.outmagenta(name + " | reply=" + reply   );
        	showData( reply );
//        	System.exit(0);
		} catch (Exception e) {
 			CommUtils.outred("callermqtt ERROR:" + e.getMessage() );
		}
	}
	
	protected void showData(IApplMessage answer) {
		Struct  t = (Struct) Term.parse(answer.msgContent());
		CommUtils.outblue(name + " | t=" + t.getArg(0).toString());
		//String answerValues = answer.msgContent().replaceAll("values(", "").replaceAll(")","")
		String answerValues = t.getArg(0).toString().replaceAll("'","");
//		CommUtils.outblue(name + " | answerValues=" + answerValues);
		//Answer: values('-1.0,-0.2,0.6###-0.840,-0.200,0.560')

		String chartUrl = ChartUtils.buildMapChartUrl( answerValues );
		ChartUtils.OpenChartInBrowser(chartUrl); 
		
	}
	
	protected void addObservation(Interaction conn) {
		new Thread() {
			public void run() {
				CommUtils.outmagenta("addObservation Thread started" );
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
