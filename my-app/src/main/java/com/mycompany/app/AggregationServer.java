package com.mycompany.app;

import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.mycompany.app.AggregationServer.clientHandler;

@SuppressWarnings("unused")
public class AggregationServer {

    private static int PORT = 4567;
    private static Socket socket = null;
    private static ServerSocket server = null;
    private static JSONObject jsonStorage = new JSONObject();
    private static boolean initStorage = true;

    public static void main(String[] args) {
        if (args.length == 1) {
            PORT = Integer.parseInt(args[0]);
        } else {
            System.out.println("Incorrect usage, only one parameter (int) is used for PORT, using default port 4567");
        }

        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started successfully...");
            while (true) {
                // accepting socket connection
                Socket socket = server.accept();
                System.out.println("New device connected");

                // handling client connection in new thread
                new clientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void GEThandler(PrintWriter out) {
        System.out.print("Detected GET request from client");
        if (!jsonStorage.isEmpty()){
            out.println(jsonStorage);
        } else {
            System.out.println("Storage is empty");
            return;
        }
    }

    //handler function for PUT requests
    private static void PUThandler(BufferedReader in, PrintWriter out) {
        System.out.println("Detected PUT request from content server");
    
        try {

            // reading http header
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
            }
            
            // reading body -> json data
            StringBuilder jsonString = new StringBuilder();
            while (in.ready()) {
                jsonString.append((char) in.read());
            }
    
            // parsing json
            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonData = (JSONObject) parser.parse(jsonString.toString());

                if (jsonData.isEmpty()){
                    out.println("204 Status code: No content received");
                    System.out.println("Content server gave no data");
                    return;
                }

                System.out.println("Received JSON from content server");
                
                if (initStorage){
                    out.println("200 Successful connection");
                    initStorage = false;
                } else {
                    out.println("201 - HTTP_CREATED");
                }

                // store the received JSON data
                jsonStorage = jsonData;

                // send confirmation to content server
                if (jsonStorage != null || jsonStorage == jsonData) {
                    out.println("Content stored / updated");
                } else {
                    out.println("Failed to update storage");
                }
            } catch (ParseException e) {
                System.out.println("Error parsing JSON: " + e.getMessage());
                out.println("500 Internal Server Error - Failed to parse JSON.");
            }
    
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
    

    static class clientHandler extends Thread {
        private Socket clientSocket;

        public clientHandler(Socket socket) {
            clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String req = in.readLine();
                if (req != null && req.startsWith("GET")) {
                    GEThandler(out);
                } else if (req != null && req.startsWith("PUT")) {
                    PUThandler(in, out);
                } else {
                    out.println("400 Error - Invalid Request");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

}
