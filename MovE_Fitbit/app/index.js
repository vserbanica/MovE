/*
 * Entry point for the watch app
 */
console.log("[APP]: Watch code started");

import { vibration } from "haptics";
import * as messaging from "messaging";
import document from "document";

//Same enum as in Android App
const ServerResultEnum = { 
    RESULT_GREEN_TRAFFIC_LIGHT: 0,
    RESULT_RED_TRAFFIC_LIGHT: 1,
    RESULT_OBSTACLE_AVOID_LEFT: 2,
    RESULT_OBSTACLE_AVOID_RIGHT: 3,
    RESULT_OBSTACLE_UNAVOIDABLE: 4,
    RESULT_UNDEFINED: 5
}

//Labels
let mixedtext = document.getElementById("mixedtext");
mixedtext.text = "The Header";
let body = mixedtext.getElementById("copy");
body.text = "This is the body text";


//INFO: Listen for the onopen event from companion
messaging.peerSocket.onopen = function() {
  //INFO: Ready to send or receive messages
  console.log("[APP]: Device message socket opens!");
}

//INFO: Listen for the onmessage event from companion
messaging.peerSocket.onmessage = function(evt) {
 console.log(JSON.stringify(evt.data));
 var jsonObj = JSON.parse(evt.data);
  
 switch(jsonObj.MLValue) {
  case ServerResultEnum.RESULT_GREEN_TRAFFIC_LIGHT: {
      vibration.start("ping");
      mixedtext.text = "Green";
      body.text = jsonObj.Object;
      break;
    }
  case ServerResultEnum.RESULT_RED_TRAFFIC_LIGHT: {
      vibration.start("ring");
      setTimeout(function(){ vibration.stop(); }, 2000);
      mixedtext.text = "Red";
      body.text = jsonObj.Object;
      break;
    }
  case ServerResultEnum.RESULT_OBSTACLE_AVOID_LEFT: {
      vibration.start("confirmation");
      vibration.start("confirmation");
      mixedtext.text = "Avoid Left";
      body.text = jsonObj.Object;
      break;
    }
  case ServerResultEnum.RESULT_OBSTACLE_AVOID_RIGHT: {
      vibration.start("nudge");
      vibration.start("nudge");
      mixedtext.text = "Avoid Right";
      body.text = jsonObj.Object;
      break;
    }
  case ServerResultEnum.RESULT_OBSTACLE_UNAVOIDABLE: {
      vibration.start("alert");
      setTimeout(function(){ vibration.stop(); }, 2000);
      mixedtext.text = "Unavoidable";
      body.text = jsonObj.Object;
      break;
    }
  case ServerResultEnum.RESULT_UNDEFINED: {
      mixedtext.text = "Undefined";
      body.text = "";
      console.log("Undefined data!");
      break;
    }  
  default: {
      mixedtext.text = "Undefined";
      body.text = "";
      console.log("Undefined data!");
      break;
    }
  }
}

//INFO: Listen for the onerror event from companion
messaging.peerSocket.onerror = function(err) {
  console.log("[APP]: ERROR");
}

//INFO: Debugg variable button for testing
let mybutton = document.getElementById("mybutton");

//INFO: Debugg button event click
mybutton.onactivate = function(evt) {
  console.log("[APP]: Clicked!");
  
  console.log(ServerResultEnum.RESULT_OBSTACLE_AVOID_LEFT);
  vibration.start("alert"); 
  mixedtext.text = "alert";
  setTimeout(function(){ vibration.stop(); }, 2000);
}



