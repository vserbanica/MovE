package com.example.owncameraapp.fitbitserver;
import com.example.owncameraapp.tflitemodel.MLResult;
import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

public class FitBitSocketServer extends WebSocketServer {
    private static final String TAG = "FitBitSocketServer";
    private WebSocket mSocket = null;

    /**
     * Constructor
     *
     * @param address InetSocketAddress that must have a host and a port.
     */
    public FitBitSocketServer(InetSocketAddress address) {
        super(address);
        this.setReuseAddr(true);
        Log.d(TAG, "Ctor IN");
    }

    /**
     * When connection with a client opens
     *
     * @param conn WebSocket connection
     * @param handshake unused param
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        mSocket = conn;

        //conn.send("Welcome to the server!"); //This method sends a message to the new client
        Log.d(TAG, "new connection to " + mSocket.getRemoteSocketAddress());
    }

    /**
     * When connection with a client closes
     *
     * @param conn WebSocket connection
     * @param code info on why connection has closed
     * @param reason info on why connection has closed
     * @param remote unused param
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if(conn != null) {
            Log.d(TAG, "closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        }
        else {
            Log.d(TAG, "closed FitBitSocketServer with exit code " + code + " additional info: " + reason);
        }
        if(mSocket != null) {
            mSocket = null;
        }
    }

    /**
     * When receiving a message from client
     *
     * @param conn WebSocket connection
     * @param message the actual message
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, "received String from "	+ conn.getRemoteSocketAddress());
    }

    /**
     * On occurance of an error regarding connection between server and client
     *
     * @param conn WebSocket connection
     * @param ex error exception
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        if( conn != null ) {
            Log.e(TAG, "an error occurred on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
        }
        else {
            Log.e(TAG, "an error occurred on connection: " + ex);
        }
        if(mSocket != null) {
            mSocket = null;
        }
    }

    /**
     * Called automatically when we run the server
     */
    @Override
    public void onStart() {
        Log.d(TAG, "Server started!");
        setConnectionLostTimeout(100);
    }

    /**
     * Send a message to the client. It's used in MainActivity.
     *
     * @param result Result from ML process
     * @param title the name of the detected object
     */
    public void sendMessage(MLResult result, String title) throws JSONException {
        if(mSocket != null) {
            JSONObject json = new JSONObject();
            json.put("MLValue",result.toInt());
            json.put("Object",title);
            sendMessage(json);
        }
        else {
            Log.e(TAG, "sendMessage: no client registered!");
        }
    }

    /**
     * Send a message to the client. It's used internally in this class.
     *
     * @param message the json message (converted to string) that is received by the client
     */
    private void sendMessage(JSONObject message) {
        Log.i(TAG, "sendMessage JSONObject: " + message.toString());
        mSocket.send(message.toString());
    }

    public void endConnection() {
        if(mSocket != null) {
            Log.d(TAG, "endConnection");
            mSocket.close();
            mSocket = null;
        }
    }
}