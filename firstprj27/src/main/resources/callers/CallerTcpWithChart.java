package callers;

import MyCode.ChartUtils;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

public class CallerTcpWithChart {
	private Interaction conn ;
	private String name = "callerchart";
	private String Min = "'-3.0'";
	private String Max = "'3.0'";
	private String Dx  = "'0.2'";
	private String args = "args("+Min+","+Max+","+Dx+")";
	private IApplMessage evalRequest = CommUtils.buildRequest(name, "evalfunvalues",  args, "a");
	
	public CallerTcpWithChart() {
		doJob();
	}
	
	protected void doJob() {
		try {
			conn = ConnectionFactory.createClientSupport(ProtocolType.tcp, "localhost", "8120");
			 
			CommUtils.outblue(name + " | sending=" + evalRequest);
			IApplMessage answer = conn.request(evalRequest);
			CommUtils.outblue(name + " | answer=" + answer.msgContent());
			
			Struct  t = (Struct) Term.parse(answer.msgContent());
			CommUtils.outblue(name + " | t=" + t.getArg(0).toString());
			//String answerValues = answer.msgContent().replaceAll("values(", "").replaceAll(")","")
			String answerValues = t.getArg(0).toString().replaceAll("'","");
			CommUtils.outblue(name + " | answerValues=" + answerValues);
			//Answer: values('-1.0,-0.2,0.6###-0.840,-0.200,0.560')

			String chartUrl = ChartUtils.splitResultString( answerValues );
			ChartUtils.OpenChartInBrowser(chartUrl); 

//			String answer = FSinSeries.evalSinPoints(-3,3,0.1);
//			String chartUrl = splitResultString( answer );
			
		} catch (Exception e) {
 			e.printStackTrace();
		}
	}
 
	

	
	public static void main(String[] args) {
		new CallerTcpWithChart();
	}

}