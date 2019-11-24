package com.example.gaitanalyzer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class WebSocketEcho extends WebSocketListener implements Runnable {
    private final Broker broker2;
    private String ip;
    private String userID;
    private Broker broker;
    private String name;
    private final String TAG = "WebSocketEcho";
    int messagesWebSocket = 0;
    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString();

    public WebSocketEcho(String name, Broker broker, Broker broker2, String userID,  String ip)
    {
        this.name = name;
        this.broker = broker;
        this.broker2 = broker2;
        this.userID = userID;
        this.ip = ip;
    }

    @Override
    public void run() {
        Log.d(TAG, "websocket RUN method called");
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0,  TimeUnit.MILLISECONDS)
                .build();

        String socketURL = "ws://gait-poc.herokuapp.com/gait";
        if(ip.length() != 0){
//            System.out.println("IP ADDRESS WAS NOT EMPTY: " + ip.length());
            socketURL = "ws://" + ip + "/gait";
        }
        Request request = new Request.Builder()
//                .url("ws://echo.websocket.org")
//                .url("ws://10.0.0.133:8000/gait")
                .url(socketURL)
                .build();
        client.newWebSocket(request, this);

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
//        client.dispatcher().executorService().shutdown();
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        // When the channel is opened
        /**
         * {
         *   "event": "connect",
         *   "data": {
         *     "userid" : 123,
         *     "uuid": "bBNhR0kYNXYoE0q6"
         *   }
         * }
         */
        /**
        {
            "event": "gait",
                "data": {
            "userid" : 123,
            "uuid":"bBNhR0kYNXYoE0q6",
                    "gait": ["timestamp, x, y, z", "timestamp1, x1, y1, z1" ]
        }
        }
         **/
        Log.d(TAG, "websocket onOpen");

        JSONObject connectedJSON = new JSONObject();
        JSONObject obj3 = new JSONObject();
//        JSONArray connectedJSONArr = new JSONArray();
//        connectedJSONArr.put(connectedJSON);
        try {
            obj3.put("userid", this.userID);
            obj3.put("uuid", randomUUIDString);
            connectedJSON.put("event","connected");
            connectedJSON.put("data", obj3);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, connectedJSON.toString());

        messagesWebSocket++;
        webSocket.send(connectedJSON.toString());
        echoText(broker, webSocket, randomUUIDString);
    }

    @Override public void onMessage(WebSocket webSocket, String text) {
        // When a message is received
        try {
            broker2.put("MESSAGE: " + text);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
        try {
            broker2.put("MESSAGE: " + bytes.hex());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(1000, null);
        try {
            broker2.put("CLOSE: " + code + " " + reason);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
    }

//    public static void main(String... args) {
//        new WebSocketEcho().run();
//    }

    void echoText(Broker broker, WebSocket webSocket, String uuid) {
        class OneShotTask implements Runnable {
            Broker broker;
            WebSocket webSocket;
            String randomUUIDString;
            int messagesSent = messagesWebSocket;
            OneShotTask(Broker broker, WebSocket webSocket, String uuid) {
                this.broker = broker;
                this.webSocket = webSocket;
                this.randomUUIDString = uuid;
            }
            public void run(){
                String data;
                try
                {
                    while (broker.continueProducing)
                    {
//                sb.append("[");
                        JSONArray array = new JSONArray();
                        JSONObject obj = new JSONObject();
                        JSONObject obj2 = new JSONObject();
                        obj2.put("userid", userID);
                        obj2.put("uuid", randomUUIDString);
                        obj.put("event", "gait");
                        long start = System.currentTimeMillis();
                        long end = start + 250;
                        while(true) {
                            //do your code
                            //
                            data = broker.get();
//                    sb.append(data + ", ");
                            array.put(data);
//                System.out.println("Consumer " + this.name + " processed data from broker: " + data);
//                    Log.d(TAG, "Consumer3 " + this.name + " processed data from broker: " + data);
                            if(data == null || System.currentTimeMillis() > end) {
                                break;
                            }
                        }
//                        for(int i = 0; i < 10; i++){ // 40 Hz = 25 ms, for 250ms interval ~ 10 tuples

//                        }
                        obj2.put("gait", array);
                        obj.put("data", obj2);
                        Log.d(TAG, "Consumer3 " +  " processed data from broker: " + obj.toString());
//                sb.append("]");
                        messagesSent++;
                        webSocket.send(obj.toString());
                        messagesWebSocket = messagesSent;

//                sb.setLength(0); // flush
                        Thread.sleep(250);
                    }


//            System.out.println("Comsumer " + this.name + " finished its job; terminating.");
                    Log.d(TAG, "Comsumer " + " finished its job; terminating.");

                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                /**
                 * {
                 *   "event": "connect",
                 *   "data": {
                 *     "userid" : 123,
                 *     "uuid": "bBNhR0kYNXYoE0q6"
                 *   }
                 * }
                 */
                Log.d(TAG, "websocket onClosing");

                JSONObject stopJSON = new JSONObject();
                JSONObject obj = new JSONObject();
                try {
                    stopJSON.put("event","stop");
                    obj.put("userid", userID);
                    obj.put("uuid", randomUUIDString);
                    stopJSON.put("data", obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, stopJSON.toString());

                messagesWebSocket++;
                webSocket.send(stopJSON.toString());
//        webSocket.close(1000, "Goodbye, World!");
            }
        }
        Thread t = new Thread(new OneShotTask(broker, webSocket, uuid));
        t.start();
    }
}

