/*
outarea.js
*/
    
    function addItem(item){
		var list = document.getElementById("msgslist")	
	    const li = document.createElement('li');
        li.appendChild(document.createTextNode(item))
		list.appendChild(li);			 
    }
	
	function clearOutArea(){
		//alert("clearOutArea")
		const lista = document.getElementById('msgslist');  
		lista.innerHTML = ''; // Rimuovi tutti i nodi figli
	}