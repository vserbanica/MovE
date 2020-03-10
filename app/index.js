/*
 * Entry point for the watch app
 */
console.log("Watch code started");

import { vibration } from "haptics";
import * as messaging from "messaging";
import document from "document";


//INFO: variable button to test the haptic sensor
let mybutton = document.getElementById("mybutton");

//INFO: previous button event click
mybutton.onactivate = function(evt) {
  console.log("Clicked!");
  vibration.start("ping"); //INFO: can have different types of vibrations
}

//INFO: Listen for the onopen event 
messaging.peerSocket.onopen = function() {
  //INFO: Ready to send or receive messages
  console.log("Device message socket opens!");
}

//INFO: Listen for the onerror event
messaging.peerSocket.onerror = function(err) {
  console.log("ERROR");
}

//INFO: Listen for the onmessage event
messaging.peerSocket.onmessage = function(evt) {
  console.log(JSON.stringify(evt.data));
}

