package com.mycompany.app;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import org.json.simple.*;

// code for content server

public class ContentServer {
    private static int PORT = 4567;
    private static String serverName = "localhost";
    private static LamportClockContent lamportClock = new LamportClockContent();
    private static JSONObject jsonFile = new JSONObject();


    // main method that handles parameter input and file reading loop
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Too few arguments, <serverName:host> <filepath> ");
            return;
        } else if (args.length > 2) {
            System.out.println("Too many arguments, <serverName:host> <filepath>");
            return;
        }
        String[] serverInfo = args[0].split(":");
        PORT = Integer.parseInt(serverInfo[1]);
        serverName = serverInfo[0];
        // file path is where you want the content server to read a .txt file from and creating a JSONObject from it
        String filePath = args[1];

        // reading from file path and populating JSON object
        fileReaderLoop(filePath, jsonFile);
        try (Scanner scanner = new Scanner(System.in)) {
            boolean exit = false;
            while (!exit) {
                lamportClock.increment();
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

    // loop that read file from file path
    public static void fileReaderLoop(String filePath, JSONObject jsonFile) { // Update method
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileReader(line, jsonFile);
            }
        } catch (IOException error) {
            System.out.println("Error reading the file: " + error.getMessage());
        }
    }

    

    // sending put request to aggregation server
    public static void PUTreq() {
        try (Socket socket = new Socket(serverName, PORT);
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // preparing put request header
            String jsonData = jsonFile.toJSONString();
            writer.println("PUT /weather.json HTTP/1.1");
            writer.println("User-Agent: ATOMClient/1/0");
            writer.println("Content-Type: application/json");
            writer.println("Content-Length: " + jsonData.length());
            writer.println("Lamport-Clock: " + lamportClock.getTime());
            writer.println();
            writer.println(jsonData); // Send the JSON body
            writer.flush();

            // read server response
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                System.out.println("Server response: " + responseLine);

            // Check for Lamport-Clock in response
            if (responseLine.startsWith("Lamport-Clock: ")) {
                int receivedLamportClock = Integer.parseInt(responseLine.split(":")[1].trim());
                lamportClock.updateTime(receivedLamportClock);  // Update ContentServer's Lamport Clock
                System.out.println("Updated ContentServer's Lamport Clock: " + lamportClock.getTime());
            }
            }
            System.out.println();
        } catch (IOException e) {
            System.out.println("Cannot connect to server or send data: " + e.getMessage());
        }
    }

    // method to read file and creating JSONObject
    @SuppressWarnings("unchecked")
    public static void fileReader(String line, JSONObject jsonFile) {
        int colonIndex = line.indexOf(":");
        if (colonIndex != -1) {
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1, line.length()).trim();
            jsonFile.put(key, value);
        } else {
            System.out.println("Invalid entry, no colon");
        }
    }
}

// class to create lamport clock
class LamportClockContent {
    private int time;

    public LamportClockContent() {
        this.time = 1;
    }

    public void increment() {
        this.time++;
    }

    // updating content server time against received lamport clock with logic max(this.time, receiveTime) + 1
    public void updateTime(int receivedTime) {
        this.time = Math.max(receivedTime, this.time) + 1;
    }

    public int getTime() {
        return this.time;
    }

    // parsing received clock within Aggregation server response
    public int parseReceivedClock(BufferedReader in) {
        String lamportString = null;
        int receivedLamportClock = -1;
        try {
            lamportString = in.readLine();
            if (lamportString != null && lamportString.startsWith("Server response: Lamport-Clock: ")) {
                receivedLamportClock = Integer.parseInt(lamportString.split(":")[2].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        System.out.println("Client - Lamport-Clock: " + receivedLamportClock);
        return receivedLamportClock;
    }
}