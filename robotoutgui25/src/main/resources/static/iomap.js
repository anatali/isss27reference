/*
iomap.js
*/
const ncells       = 10
const mapContainer = document.getElementById("map");  
mapContainer.innerHTML = '';
    

//25 is the same used in mapstyle.css
function createMapRep(){ 
	
 console.log("createMapRep")
 for( let i=0; i<ncells; i++ ){
     for( let j=0; j<ncells; j++ ){
        const cellElement = document.createElement("div");
        cellElement.classList.add("cell");
        cellElement.classList.add("green");
        cellElement.id = `cell-${i}-${j}`;
        cellElement.addEventListener('click', function() { 
            sendCmdToServer(""+cellElement.id);  //cell-i-j in commsocket.js
            //alert(""+cellElement.id );
        });
        mapContainer.appendChild(cellElement);
        //console.log("created cell" );
}//for
}//for
}//createMapRep


function clearAllCells(){ 
	//createMapRep()  //NO: crea un nuovo div
	
	console.log("clearAllCells")
 for( let i=0; i<ncells; i++ ){
     for( let j=0; j<ncells; j++ ){
		const cell = document.getElementById(`cell-${i}-${j}`);
		//console.log("clear " + `cell-${i}-${j}`)
		 /*
		if( cell.classList.contains("red") ){
		  cell.classList.remove("red");
		}
		else if( cell.classList.contains("green") ){
		  cell.classList.remove("green");
		}
		 */
		cell.className =  "";
		cell.classList.add("green");
	}//for
 }//for
 
}//clearAllCells

    function updateCellColor(newX, newY,color) {
	if( newX == 0 && newY == 0 ) console.log("updateCellColor " + color)
       const newRobotCell = document.getElementById(`cell-${newY}-${newX}`);

	   newRobotCell.className =  ""; //Clear

      if (color == 1) {
        newRobotCell.classList.add("red");
      }else if (color == 0) {
        newRobotCell.classList.add("green");
      }
	  else if( color == 2){ 	    
	    newRobotCell.classList.add("acqua");
	  }
	  else if( color == 3){ 	    
	    newRobotCell.classList.add("yellow");
	  }
	  else if( color == 4){ 	    
	    newRobotCell.classList.add("blue");
	  }
	  else if( color == 5){ 	    
	    newRobotCell.classList.add("cyan");
	  }
	  else if( color == 6){ 	    
	    newRobotCell.classList.add("black");
	  }
	  else if( color == 7){ 	    
	    newRobotCell.classList.add("magenta");
	  }
	  else if( color == 8){ 	    
	    newRobotCell.classList.add("white");
	  }
  else if( color == 9){ 	    
    newRobotCell.classList.add("sud");
  }
  else if( color == 10){ 	    
    newRobotCell.classList.add("est");
  }
  else if( color == 11){ 	    
    newRobotCell.classList.add("west");
  }
  else if( color == 12){ 	    
    newRobotCell.classList.add("nord");
  }
  else if( color == 13){ 	    
    newRobotCell.classList.add("target");
  }
    }

	/*
	* Vecchia funzione eliminabile
	*/
	function switchCellColor(newX, newY ) {
	   const newRobotCell = document.getElementById(`cell-${newY}-${newX}`);
	  //console.log("iomap switchCellColor BEFORE " + newX + " " + newY + " " + newRobotCell.classList.contains("green"))
	  if( newRobotCell.classList.contains("red") ){
	    newRobotCell.classList.remove("red");
		newRobotCell.classList.add("green");
	  }
	  else if( newRobotCell.classList.contains("green") ){
	    newRobotCell.classList.remove("green");
		newRobotCell.classList.add("red");
	  }
	 //console.log("iomap switchCellColor AFTER " + newX + " " + newY + " " + newRobotCell.classList.contains("green"))
	}	   
    
createMapRep()
//clearAllCells()
