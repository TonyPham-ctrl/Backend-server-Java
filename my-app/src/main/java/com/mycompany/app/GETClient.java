package com.mycompany.app;

import java.io.*;
import java.net.*;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.Scanner;

public class GETClient {

    public static int PORT = 4567;
    public static String serverName = "localhost";
    public static LamportClockClient lamportClock = new LamportClockClient();
    public static boolean exit = false;
    public static String stationID = null;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Too few arguments, <serverName:host> <stationID> (optional)>");
            return;
        } else if (args.length > 2) {
            System.out.println("Too many arguments, <serverName:host> <stationID> (optional)>");
            return;
        }
        String serverInfo = args[0];
        if (args.length == 2) {
            stationID = args[1];
        }
        // parsing server information
        String[] serverAddr = parseServerInfo(serverInfo);
        PORT = Integer.parseInt(serverAddr[1]);
        serverName = serverAddr[0];

        
        try (Scanner scanner = new Scanner(System.in)) {
            while (!exit) {
                StringBuilder jsonResponse = new StringBuilder();
                GETreq(jsonResponse);
                parseServerJson(jsonResponse);
                terminalQuery(scanner);
            }
        }
    }

    public static void terminalQuery(Scanner scaner) {
        System.out
                .println("\n type (yes) for update, (no) to stop, (lamport) to show lamport");
        String userInput = scaner.nextLine().trim().toLowerCase();
        if (userInput.equals("no")) {
            exit = true;
        } else if (userInput.equals("lamport")) {
            System.out.println(lamportClock.getTime());
            terminalQuery(scaner);
        }
    }

    public static void GETreq(StringBuilder jsonResponse) {
        // connecting to server socket
        try (Socket socket = new Socket(serverName, PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send GET request
            lamportClock.increment();
            out.println("GET /weather.json HTTP/1.1");
            out.println("Lamport-Clock: " + lamportClock.getTime());
            if (stationID != null){
                out.println("StationID: " + stationID);
            }
            out.println();
            out.flush();

            // Read response
            int serverLamportClock = lamportClock.parseReceivedClock(in);
            if (serverLamportClock == -1) {
                System.out.println("Unable to parse server's lamport");
            } else {
                System.out.println("Server: Server's lamport clock: " + serverLamportClock);
                lamportClock.updateTime(serverLamportClock);
            }

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                jsonResponse.append(line);
            }

            if (jsonResponse.length() == 0) {
                System.out.println("Received an empty response from the server.");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void parseServerJson(StringBuilder res) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonData = (JSONObject) parser.parse(res.toString());

            // Print each key-value pair
            for (Object key : jsonData.keySet()) {
                Object value = jsonData.get(key);
                System.out.println(key + ": " + value);
            }
        } catch (ParseException e) {
            System.out.println("Client - Error parsing JSON: " + e.getMessage());
            System.out.println(res);
        }
    }

    public static String[] parseServerInfo(String serverInfo) {
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

class LamportClockClient {
    public int time;

    public LamportClockClient() {
        this.time = 1;
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

    public int parseReceivedClock(BufferedReader in) {
        String lamportString = null;
        int receivedLamportClock = -1;
        try {
            lamportString = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        if (lamportString != null && lamportString.startsWith("Lamport-Clock: ")) {
            receivedLamportClock = Integer.parseInt(lamportString.split(":")[1].trim());
        }
        return receivedLamportClock;
    }
}