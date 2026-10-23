package unibo.actorgridgui.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.socket.WebSocketSession;
import jakarta.servlet.http.HttpServletRequest;
import unibo.basicomm23.utils.CommUtils;
 
 
@Controller
public class GridGuiController {

//	@Value("${spring.application.name}")
//	String appName;
//	
//	@Value("${server.port}")
//	String serverport;

 	
//    private static String curOwnerName = "none";
//    private static String ownerName    = "unibodisi";
	
 	public static boolean serverInDocker  = false;
	public static String serverIP    = "";
	public static String callerIP    = "";
 	private boolean started = false;
	
//	public GridGuiController() {
// 	}
	
//	public static boolean checkIfOwner(   ) {
// 		CommUtils.outgreen("GridGuiController | checkIfOwner curOwnerName= " + curOwnerName  );
// 		return curOwnerName.equals(ownerName);
//		//return callerIP.equals(serverIP) || callerIP.equals("0:0:0:0:0:0:0:1") || callerIP.startsWith("172") ;
//	}
	
  
	@GetMapping("/")
	public String homePage(Model model, HttpServletRequest request) {
		CommUtils.outmagenta("GridGuiController homePage");
		//ModelAndView modelAndView = new ModelAndView();
	    //modelAndView.setViewName("guipage"); 
//		callerIP = getClientIp(request);
	    return "guipage";  
	}

 	
	//CHiamata dalla pagina HTML per conoscere l'ip del server
	@RequestMapping("/getserverip")
	@ResponseBody
	public String getserverip(HttpServletRequest request) {
		try {
		CommUtils.outyellow("GridGuiController doing getserverip"  );
		callerIP = request.getRemoteAddr();
		CommUtils.outmagenta("GridGuiController getserverip callerIP="+ callerIP );
		
		String mypubip = getMyPublicip();   //Just to check ....
		System.out.println("GridGuiController getserverip: mypubip=" + mypubip);
		
		serverIP    = CommUtils.getEnvvarValue("HOST_IP"); // in docker-compose
		if( serverIP != null ) {
			serverInDocker = true;
			String s = "{\"host\":\"P\"}".replace("P",serverIP );
			System.out.println("GridGuiController con HOST_IP serverIP:" + s);		      
			return s;				
		}
		serverIP = getServerLocalIp();
		//System.out.println("GridGuiController getserverip: serverIP=" + serverIP);
		if( serverIP == null ) { //non ho la rete ...
			String s = "{\"host\":\"localhost\"}";
			System.out.println("GridGuiController senza la rete serverIP:" + s);		      
			return s;				
		}
 		else { 
  			String s = "{\"host\":\"P\"}".replace("P",serverIP );
			System.out.println("GridGuiController con la rete serverIP:" + s);		      
			return s;				
 		}
		}catch(Exception e) {
			CommUtils.outred("getserverip ERROR:"+e.getMessage());
			return "{\"host\":\"localhost\"}";
		}
	}
 
    public String getClientIp(HttpServletRequest request) {
        CommUtils.outgreen("GridGuiController | getClientIp request=" 
        		+ request.getLocalAddr() +  " remote=" + request.getRemoteAddr());
        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        if (remoteAddr == null || "".equals(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }
        CommUtils.outgreen("GridGuiController | getClientIp callerAddr=" + remoteAddr);
        return  remoteAddr;
    }

    private static String getClientIp(WebSocketSession session) {
    	try {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
	        if (attr != null) {
	            HttpServletRequest request = attr.getRequest();
	            String remoteAddr = request.getHeader("X-FORWARDED-FOR");
	            if (remoteAddr == null || "".equals(remoteAddr)) {
	                remoteAddr = request.getRemoteAddr();
	            }
	            
	            return remoteAddr;
	        }
	        return "Indirizzo IP non disponibile";
		} catch (Exception e) {
			System.out.println("Errore nell'ottenere l'indirizzo IP: " + e.getMessage());
			return "getClientIp-session error";
		}        
    }
    
	protected  String getMyPublicip(){
		try {
			// URL di un servizio che restituisce l'indirizzo IP pubblico
			String serviceUrl = "https://checkip.amazonaws.com";

			// Creazione della connessione HTTP
			URL url = new URL(serviceUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			// Lettura della risposta
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();   
			
			String localip = getServerLocalIp();
			
			// Stampa dell'indirizzo IP pubblico
			String myip = response.toString().trim();
			System.out.println("Il tuo indirizzo IP pubblico è: " +  myip);
		    //return  myip;
		    return localip;
		} catch (Exception e) {
			System.out.println("Errore nell'ottenere l'indirizzo IP: " + e.getMessage());
			return null;
		}
	}
	
	protected String getServerLocalIp() {		
        try {
            Enumeration<NetworkInterface> interfacce = NetworkInterface.getNetworkInterfaces();
            while (interfacce.hasMoreElements()) {
                NetworkInterface interfaccia = interfacce.nextElement();
                Enumeration<InetAddress> indirizzi = interfaccia.getInetAddresses();
                while (indirizzi.hasMoreElements()) {
                    InetAddress indirizzo = indirizzi.nextElement();
                    if (!indirizzo.isLoopbackAddress()) { // Esclude l'indirizzo loopback (127.0.0.1)
                        //System.out.println(interfaccia.getDisplayName() + ": " + indirizzo.getHostAddress());                        
                        if( indirizzo.getHostAddress().startsWith("192.168")) {
                        	//System.out.println("GridGuiController ==== " + indirizzo.getHostAddress());
                        	return indirizzo.getHostAddress();
                        }
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            System.err.println("Errore durante la ricerca degli indirizzi IP: " + e.getMessage());
            return null;
        }			
 	
	}
	 
}
