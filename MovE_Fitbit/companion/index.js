/*
 * Entry point for the companion app
 */
console.log("[COMP]: Companion code started");

import * as messaging from "messaging";
//import Fetch from "../modules/companion/fetch.js";  //unused

//Used for connection with the AndroidApp via WebSockets
const wsUri = "ws://127.0.0.1:8887";
const TryServerConnectionTime = 6000;
let isServerConnected = false;
let websocket;
let serverIntervalId;

//----------------------------------------------------------------------
//WATCH CONNECTIONS

//INFO: Listen for the onopen event 
messaging.peerSocket.onopen = function() {
  console.log("[COMP]: Connection with Watch socket open!");
  console.log("[COMP]: start Timer");
  
  //INFO: start a recurent task that tryes the connection to the server until it connects
  //INFO: for the case when we first start the WatchApp and the server from the PhoneApp is not running 
  isServerConnected = false;
  serverIntervalId = setInterval(startWebSocket, TryServerConnectionTime);
}

//INFO: Listen for the onerror event
messaging.peerSocket.onerror = function(err) {
  console.log("[COMP]: Error from watch");
}

//INFO: Listen for the onmessage event
messaging.peerSocket.onmessage = function(evt) {
  console.log("[COMP]: Meessage from watch");
}

//----------------------------------------------------------------------
//ANDROID CONNECTIONS

function onOpen(evt) {
  console.log("CONNECTED");
  if(isServerConnected == false) {
    console.log("[COMP]: stop Timer");
    isServerConnected = true;
    
    clearInterval(serverIntervalId);
  }
}

function onClose(evt) {
  console.log("DISCONNECTED");
  if(isServerConnected == true) {
    console.log("[COMP]: start Timer");
    isServerConnected = false;
    
    if(websocket != null) {
      stopWebSocket();
    }
    serverIntervalId = setInterval(startWebSocket, TryServerConnectionTime);
  }
  
}

function onMessage(evt) {
  //INFO: Forward message to Watch
  if (messaging.peerSocket.readyState === messaging.peerSocket.OPEN) {
    console.log("[COMP]: Forward message to WATCH!");
    messaging.peerSocket.send(evt.data);
  }
  else {
    console.log("[COMP]: Error, readyState != OPEN");
  }
}

function onError(evt) {
  console.error(`ERROR: ${evt.data}`);
}

function doSend(message) {
  console.log(`SEND: ${message}`);
  websocket.send(message);
}

/** Function to make the connections with the server. */
function startWebSocket() {
  console.log('Try start WebSocket');
  websocket = new WebSocket(wsUri);
  websocket.addEventListener("open", onOpen);
  websocket.addEventListener("close", onClose);  
  websocket.addEventListener("message", onMessage);
  websocket.addEventListener("error", onError);
}

/** Function to remove the connections with the server */
function stopWebSocket() {
  console.log('Try stop WebSocket');
  websocket.removeEventListener("open", onOpen);
  websocket.removeEventListener("close", onClose);
  websocket.removeEventListener("message", onMessage);
  websocket.removeEventListener("error", onError);
  websocket = null;
}

//----------------------------------------------------------------------