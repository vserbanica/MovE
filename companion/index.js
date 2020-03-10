/*
 * Entry point for the companion app
 */
console.log("Companion code started");

import * as messaging from "messaging";


//INFO: Listen for the onopen event 
messaging.peerSocket.onopen = function() {
  console.log("Companion message socket opens!");
  
  //INFO: Send a message
  if (messaging.peerSocket.readyState === messaging.peerSocket.OPEN) {
    messaging.peerSocket.send("Test Message2");
  }
  else {
    console.log("Error, readyState != OPEN");
  }
}


