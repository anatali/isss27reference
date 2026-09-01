package callers;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

public class CallerTcp {
	private Interaction conn ;
	private String name = "acaller";
	private String v    = ""+Math.PI / 2;
	private IApplMessage evalRequest = CommUtils.buildRequest(name, "evalfun",  "arg("+v+")", "a");
	
	public CallerTcp() {
		doJob();
	}
	
	protected void doJob() {
		conn = ConnectionFactory.createClientSupport(ProtocolType.tcp, "localhost", "8120");
		CommUtils.outblue(name + " | sending=" + evalRequest);
		try {
			IApplMessage answer = conn.request(evalRequest);
			CommUtils.outblue(name + " | answer=" + answer.msgContent());
		} catch (Exception e) {
 			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new CallerTcp();
	}

}