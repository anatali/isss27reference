/*
buttoninput.js
*/

document.getElementById('clear').addEventListener('click', function(event) {
  //alert(event)
  event.preventDefault() // Impedisce l'invio del modulo
  event.stopPropagation() 
  sendCmdToServer('clear')
});



function handleOwnerChkbox(){
	const noowner = document.getElementById('owner');
	if (noowner.checked) {
		sendCmdToServer("owneroff")
	}else{
		sendCmdToServer("owneron")
	}	
}	


function enableButtons() {
    document.getElementById("start").disabled = false;
    document.getElementById("stop").disabled = false;
	document.getElementById("clear").disabled = false;
	document.getElementById("exit").disabled = false;
    console.log("Pulsanti abilitati");
}

function disableButtons() {
    document.getElementById("start").disabled = true;
    document.getElementById("stop").disabled = true;
	document.getElementById("clear").disabled = true;
	document.getElementById("exit").disabled = true;
    console.log("Pulsanti disabilitati");
}
 
///C:/Didattica2025/qak25/conwaygui/src/main/resources/templates/guipage.html