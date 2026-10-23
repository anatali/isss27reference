package unibo.actorgridgui.ws; 
import java.util.HashSet;
import java.util.Set;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import jakarta.websocket.server.ServerEndpoint;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.utils.CommUtils;

/*

 */

//@Component //Ci pensa WebSocketConfiguration
@ServerEndpoint("/wsupdates")
public class WSActorGuiMqtt extends AbstractWebSocketHandler{   
	
	private static WSActorGuiMqtt myself        = null;
	private MqttInteraction mqttConn;
	private final String name                    = "wsactorguimqtt";
	private Set<WebSocketSession> clients        = new HashSet<>();
	private boolean firstConnection              = true;
	private WebSocketSession owner;
	
	
	
	public static WSActorGuiMqtt getInstance() {
		//CommUtils.outblue( name + " | getInstance " + myself );
		if (myself == null) {
			myself = new WSActorGuiMqtt();
		}
		return myself;
	}  
	
	public WSActorGuiMqtt() {
  		//useMqttInteraction();
    }
	
	protected void useMqttInteraction() {
		//CommUtils.outblue(name + " | useMqttInteraction  "  );
		String broker = System.getenv("MQTTBROKER_URL");
		if( broker == null ) {
			//String lcoalAddr = CommUtils.getServerLocalIp();
			broker = "tcp://localhost:1883";   //in locale outof docker
			CommUtils.outgreen( name + " |  outside docker " + broker  );			
		} 
		else {
			broker = "tcp://HOST:1883".replace("HOST", broker);
			CommUtils.outgreen( "wsactorguimqtt | useMqttInteraction in docker to " + broker ); 	
		}					

		
		mqttConn = new MqttInteraction(  name, broker, "guiin", "actorapplin" );
 		
		new Thread() {
			public void run() { //activateReceive(); NECESSARIO SE Life non usa WS
				try {
					CommUtils.outblue(name + " | wsactorguimqtt activate receiver ..."  );
					while (true) {
	 					String msg = "";
						msg = mqttConn.receiveMsg();
 						CommUtils.outblue(name + " | wsactorguimqtt receives via mqtt: " + msg);
						elabMsgMqtt( msg );
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();		
 	
	}

 	
	@Override //from AbstractWebSocketHandler
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);		
		//CommUtils.outblue(name + " | WebSocket: " + session.getId() + " connected!");
		if( firstConnection ) {
			owner           = session;
			firstConnection = false;
		}
        clients.add(session);
	}
 	 
	/*
	 * Invio comandi alle pagine HTML connesse
	 */
    //Usato da MqttConnCallback e dal receiver su MqttInteraction
	public void broadcastToWebSocket(String message) {
 		//CommUtils.outyellow(name + " broadcastToWebSocket " + message);
        for (WebSocketSession client : clients) {
            if (client.isOpen()) {
                try {
                    client.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    CommUtils.outred(name + "ERROR: COULD NOT SEND TEXT THROUGH WEBSOCKET TO " + client.getId() + "!");
                }
            }
        } 
    }	
	
	/*
	 * Gestione comandi provenienti dalla GUI
	 */ 	
	@Override //since AbstractWebSocketHandler
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
     	//CommUtils.outgreen(name + " | receives: " + message.getPayload() );		
		String cmd = message.getPayload();
		elabMsg(cmd, session);				
	}

	/*
	 * Msg rcevuti via MQTT
	 */
	protected void elabMsgMqtt( String cmd ) throws Exception {
		if( cmd.startsWith("cell(") ){ //IL comando arriva dalla applicazione remota
			broadcastToWebSocket(cmd);
			return;
	    }		
		if( cmd.startsWith("clear") ){ //IL comando arriva dalla GUI ma ottimizzo
			CommUtils.outmagenta(name + " | receives: " + cmd   );	
			//ESEGUO IL COMANDO
			//broadcastToWebSocket(cmd);
			//mqttConn.forward(cmd);
			return;
	    }		
 	}

	/*
	 * Msg rcevuti via WS
	 */
	protected void elabMsg(String cmd, WebSocketSession session) throws Exception {
		if( cmd.startsWith("cell(") ){ //IL comnado arriva dalla applicazione remota
			broadcastToWebSocket(cmd);
			return;
	    }		
		if( cmd.startsWith("cell-") ){ //IL comnado arriva dalla GUI
			broadcastToWebSocket(cmd);
//			if(mqttConn!=null) mqttConn.forward(cmd);     //Just to test if TestClientMqtt receives ...
			return;
	    }		 
		else if( cmd.startsWith("clear") ){ //IL comado arriva dalla GUI ma ottimizzo
			CommUtils.outmagenta(name + " | receives CLEAR " + cmd   );	
 			broadcastToWebSocket(cmd);
//			if(mqttConn!=null) mqttConn.forward(cmd);
			return;
	    }		
 
        //Il comando va verso l'applicazione remota
		if( mqttConn != null ) {
			CommUtils.outgreen(name + " | handleTextMessage forward on mqttConn: " + cmd) ;	
			mqttConn.forward(cmd);  
			if( cmd.equals("exit")) System.exit(0);
	    } 
		
	}
}
