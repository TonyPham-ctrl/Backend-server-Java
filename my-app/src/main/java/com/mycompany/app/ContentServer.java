package com.mycompany.app;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import org.json.simple.*;

public class ContentServer {
    private static int PORT = 4567;
    private static String serverName = "localhost";
    private static LamportClock lamportClock = new LamportClock();
    private static JSONObject jsonFile = new JSONObject();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Too few arguments, <serverName:host> <stationID (optional)>");
            return;
        } else if (args.length > 2) {
            System.out.println("Too many arguments, <serverName:host> <stationID (optional)>");
            return;
        }
        String[] serverInfo = args[0].split(":");
        PORT = Integer.parseInt(serverInfo[1]);
        serverName = serverInfo[0];
        String filePath = args[1];

        // reading from file path and populating JSON object
        fileReaderLoop(filePath);
        try (Scanner scanner = new Scanner(System.in)) {
            boolean exit = false;
            while (!exit) {
                PUTreq();
                System.out
                        .println("\n type (yes) to send another json, (no) to stop");
                String userInput = scanner.nextLine().trim().toLowerCase();
                if (userInput.equals("exit")) {
                    exit = true;
                }
            }
        }
    }

    private static void fileReaderLoop(String filePath){
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileReader(line, jsonFile);
            }
        } catch (IOException error) {
            System.out.println("Error reading the file: " + error.getMessage());
            return;
        }
    }

    private static void PUTreq() {
        try (Socket socket = new Socket(serverName, PORT);
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // sending put request
            String jsonData = jsonFile.toJSONString();
            writer.println("PUT /weather.json HTTP/1.1");
            writer.println("User-Agent: ATOMClient/1/0");
            writer.println("Content-Type: application/json");
            writer.println("Content-Length: " + jsonData.length());
            writer.println();
            writer.println(jsonData); // Send the JSON body
            writer.flush();

            // read server response
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                if (responseLine.startsWith("Lamport-Clock :")){

                }
                System.out.println();
                System.out.println("Server response: " + responseLine);
            }
        } catch (IOException e) {
            System.out.println("Cannot connect to server or send data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void fileReader(String line, JSONObject jsonFile) {
        int colonIndex = line.indexOf(":");
        if (colonIndex != -1) {
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1, line.length()).trim();

            // System.out.println( key + " | " + value);
            jsonFile.put(key, value);
        } else {
            System.out.println("Invalid entry, no colon");
        }
    }
}

class LamportClock {
    private int time;

    public LamportClock() {
        this.time = 0;
    }

    public void increment() {
        this.time++;
    }

    public void updateTime(int receivedTime) {
        this.time = Math.max(receivedTime, this.time) + 1;
    }

    public int getTime() {
        return this.time;
    }
}