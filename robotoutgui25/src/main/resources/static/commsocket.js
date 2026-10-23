/*
commsocket.js
*/

var socketToGui = null;
var serviceUrl  = null;
var opened      = false

//Chiamata al caricamento
function discoverServiceUrl(){
	fetch('/getserverip')	
	 .then(response => {
		if (!response.ok) {
			alert("Errore nella richiesta al server")		
			throw new Error('Errore nella richiesta: ' + response.status)	
		}
		return response.json()
	 })
	 .then(config => {
	   serviceUrl = `${config.host}`
	   console.log("commsocket serviceUrl="+serviceUrl)
	   createSocketForGui(serviceUrl);
	 })
	 .catch(error => console.log('Errore:', error));
}

function createSocketForGui(serviceUrl){
    //CREAZIONE DELLA CONNESSIONE WEBSOCKET DELLA GUI
	    wsAddr = "ws://localhost:8085/wsupdates"//.replace("localhost",serviceUrl)
		console.log("wsAddr="+wsAddr)
   		socketToGui = new WebSocket(wsAddr);  
        console.log("socketToGui="+socketToGui.url)
	//FUNZIONI DI CALLBACK 	
		socketToGui.onopen = function(event) {
		  console.log("socketToGui ONOPEN event="+event)
		  opened = true;
		};
		socketToGui.onmessage = function(event) {  
		//event generato da WSServer broadcastToWebSocket
			//console.log("--- socketToGui ONMESSAGE "+event.data + " " + serviceUrl)
			handleWsMessage(event) 
		};
		socketToGui.onclose = function(event) {
		  // Handle connection close
		  console.log("--- socketToGui CLOSEEEE ", "code=", event.code, " reason=", event.reason)
		};		 
		socketToGui.addEventListener("error", (event) => {
		  console.log("socketToGui error: ", event);
		  
		});
}

function handleWsMessage(event){
    const message = event.data;
	try{
	
/*
	if (message === "enable") {
	    enableButtons();
	} else if (message === "disable") {
	    disableButtons();
	}
*/
    //console.log("commsocket handleWsMessage " + message)     
        if (message.startsWith("cell(")) {
           // Estrae la posizione e il colore
            const [_, x, y, v] = message.match(/cell\((\d+),(\d+),([^)]+)\)/);
            updateCellColor(parseInt(x), parseInt(y), parseInt(v));   //in iomap.js
        }else if (message.startsWith("cell-")) {
			const [_, x, y ] = message.match(/cell-(\d+)-(\d+)/);
			console.log("x=" + x + " y="+ y)
		    switchCellColor(parseInt(y), parseInt(x));   //in iomap.js
		} else if (message.startsWith("clear")) {
			//clearOutArea() //in outarea.js  
			clearAllCells() //in iomap.js
        }else {
            addItem(message); //in outarea.js
        }
	}catch(e){
		console.log("ERROR " + e)
	}
}



function sendCmdToServer(cmd) {
	 console.log("sendCmdToServer:" + cmd + " opened=" + opened)
	 if( opened )
     socketToGui.send(cmd);
}


 
discoverServiceUrl()