package com.mycompany.app;

import java.io.*;
import java.net.*;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GETClient {

    private static int PORT = 4567;
    private static String serverName = "localhost";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Too few arguments, <serverName:host> <stationID (optional)>");
            return;
        } else if (args.length > 2) {
            System.out.println("Too many arguments, <serverName:host> <stationID (optional)>");
            return;
        }
        String serverInfo = args[0];
        @SuppressWarnings("unused")
        String stationID = args.length == 2 ? args[1] : "";

        // parsing server information
        String[] serverAddr = parseServerInfo(serverInfo);

        PORT = Integer.parseInt(serverAddr[1]);
        serverName = serverAddr[0];

        try (Socket socket = new Socket(serverName, PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send GET request
            out.println("GET /weatherData");

            // Read response
            String res;
            StringBuilder response = new StringBuilder();
            while ((res = in.readLine()) != null) {
                response.append(res);
            }

            // print the raw response for debugging
            // System.out.println("Raw response: " + response.toString());

            if (response.length() == 0) {
                System.out.println("Received an empty response from the server.");
                return;
            }

            // parse and print JSON data
            System.out.println("JSON data from Server --> ");
            parseServerJson(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseServerJson(StringBuilder res) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonData = (JSONObject) parser.parse(res.toString());

            // Print each key-value pair
            for (Object key : jsonData.keySet()) {
                Object value = jsonData.get(key);
                System.out.println(key + ": " + value);
            }
        } catch (ParseException e) {
            System.out.println("Error parsing JSON: " + e.getMessage());
        }
    }

    private static String[] parseServerInfo(String serverInfo) {
        String[] parts;

        if (serverInfo.startsWith("http://")) {
            parts = serverInfo.substring(7).split(":");
        } else if (serverInfo.contains(":")) {
            parts = serverInfo.split(":");
        } else {
            return null;
        }
        return parts;
    }
}
