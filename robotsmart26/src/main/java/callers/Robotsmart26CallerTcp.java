package callers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import alice.tuprolog.Struct;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

/**
 * Robotsmart26CallerTcp - TCP Client for Basic Robot Communication
 * 
 * 
 * @author Unibo Robotsmart26 Team
 * @version 2026
 */
public class Robotsmart26CallerTcp  {
    /** Robot commands utility for accessing predefined robot operations */
    private Robotsmart26Cmds robot = new Robotsmart26Cmds();

    private Interaction conn;

    /**
     * Establishes a TCP connection to the robot service.
     * Creates a client connection using the ConnectionFactory with TCP protocol.
     * 
     * @param host The hostname or IP address of the robot service
     * @param port The port number as a string
     * @return An Interaction object for communication, or null if connection fails
     */
    protected Interaction connectToService(String host, String port) {
		try {			 
  				CommUtils.outcyan("connectService Hostname: " + host);
				CommUtils.outcyan("connectService Port:     " + port);
 				Interaction connSupport = 
 						ConnectionFactory.createClientSupport23(ProtocolType.tcp, host, port);
 				return connSupport;
 		} catch (Exception e) {
			CommUtils.outred("ERROR:" + e.getMessage());
			return null;
		}
	}
    
    public void doSetDir() throws Exception   {
		conn = connectToService("localhost", "8020");
		if( conn != null ) {
			
			CommUtils.outcyan("connectService doSetDir");
			conn.forward(Robotsmart26Cmds.setHome.toString());
 
			 
			String robotstate = getRobotState();
			CommUtils.outmagenta("connectService robotstate before=" + robotstate);
			
 
//			CommUtils.outcyan("cmdl:" + Robotsmart26Cmds.cmdl);
//			conn.forward(Robotsmart26Cmds.cmdl.toString());  // Turn left
 			
			CommUtils.outcyan("setDirectionLeft:" + Robotsmart26Cmds.setDirectionLeft);
			String reply = conn.request(Robotsmart26Cmds.setDirectionLeft.toString());  // Mind
			CommUtils.outcyan("setDirectionLeft ANSWER:" + reply);

			robotstate = getRobotState();
			CommUtils.outmagenta("connectService robotstate after=" + robotstate);
		}
    	
    }
    
    /** Request getrobotstate; restituisce {X, Y, DIR} da robotstate(pos(X,Y),DIR). 
     * @throws Exception */
    private  String getRobotState() throws Exception   {
        IApplMessage req = CommUtils.buildRequest("callertcp", "getrobotstate", "getrobotstate(test)", "robotmnemo");
        String reply = conn.request(req.toString());
        //CommUtils.outblack("getRobotState reply:" + reply);
        return reply;
    }
	
	/**
     * Sends a sequence of basic movement commands to the robot.
     * Demonstrates how to use the Interaction interface to control the robot
     * with simple movement commands (left, right, forward).
     * 
     * @throws Exception if communication fails
     */
	public void doSomeCmd() throws Exception   {
		String answer= "";
		conn = connectToService("localhost", "8020");
		if( conn != null ) {
			CommUtils.outcyan("connectService doSomeCmd");
			conn.forward(Robotsmart26Cmds.setHome.toString());
			//conn.forward(Robotsmart26Cmds.resesetplanbuildelay.toString()); 
			//conn.forward(Robotsmart26Cmds.restoreplanbuildelay.toString());
			// Send basic movement commands
/*
			CommUtils.outcyan("cmdl:" + Robotsmart26Cmds.cmdl);
			conn.forward(Robotsmart26Cmds.cmdl.toString());  // Turn left
			CommUtils.delay(1000);
			conn.forward(Robotsmart26Cmds.cmdr.toString());  // Turn right
			CommUtils.delay(1000); 
			 
			CommUtils.outcyan("connectService doSomePlan");
			answer = conn.request(Robotsmart26Cmds.doplan.toString().replace("PLAN", "lwwwwww"));  // 
			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);
			CommUtils.delay(1000);
*/
			
			CommUtils.outcyan("connectService goto cell 1/y,4/x ");
		 
			answer = conn.request(Robotsmart26Cmds.move14.toString());  // 
			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);

			String reply = conn.request(Robotsmart26Cmds.setDirectionLeft.toString());  // Mind
			CommUtils.outcyan("setDirectionLeft ANSWER:" + reply);

			String robotstate = getRobotState();
			CommUtils.outmagenta("connectService robotstate after=" + robotstate);

			CommUtils.delay(1000);

//			answer = conn.request(Robotsmart26Cmds.doplan.toString().replace("PLAN", "llwwwwwwl"));  // 
//			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);
//			CommUtils.delay(1000);

			CommUtils.outcyan("doSomeCmd BYE");
		}
        System.exit(0);
	}
	
	/*
	 * Fatto da Claude
	 */
	public static IApplMessage requestOrFail(Interaction conn, IApplMessage req, int toutMs)
	        throws Exception {
	    IApplMessage answer = conn.request(req, toutMs);      // lancia "request timeout"
	    if (answer.msgId().equals("requestfailed"))
	        //throw new QakRequestFailedException(answer.msgContent());
	    	throw new Exception(answer.msgContent());
	    return answer;
	}
	
 
    /**
     * Executes a predefined path-following command sequence.
     * Uses the tf25 method from RobotCmds to demonstrate higher-level
     * robot control with path planning and execution.
     * 
     * @throws Exception if communication fails
     */
  
	/**
     * Main entry point for the TCP caller application.
     * Creates an instance and executes robot commands.
     * 
     * Available options:
     * - doSomeCmd(): Basic movement commands
     * - doJob(): Predefined path execution (tf25)
     * 
     * @param args Command line arguments (not used)
     * @throws Exception if any operation fails
     */
	public static void main(String[] args) throws Exception  {
 		Robotsmart26CallerTcp caller = new Robotsmart26CallerTcp();
		//caller.doSomeCmd();  // basic movement demo
 		caller.doSetDir();  // set direction demo
	}
}
