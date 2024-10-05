package com.mycompany.app;

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AggregationServer {
    private static int PORT = 4567;
    private static Socket socket = null;
    private static ServerSocket server = null;
    private static JSONObject jsonStorage = new JSONObject();
    private static boolean initStorage = true;
    private static LamportClock lamportClock = new LamportClock();
    private static String storageFilePath = "/home/tonypham/distributedSystem2024S2/my-app/src/main/java/com/mycompany/app/jsonStorage.json";
    private static PriorityQueue<contentServer> contentServerLamportQueues = new PriorityQueue<>(Comparator.comparingInt(contentServer::getLamportClock));

    public static void main(String[] args) {
        if (args.length == 1) {
            PORT = Integer.parseInt(args[0]);
        } else {
            System.out.println("Incorrect usage, only one parameter (int) is used for PORT, using default port 4567");
        }

        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started successfully...");
            jsonStorage = readJson();
            while (true) {
                // accepting socket connection
                Socket socket = server.accept();
                System.out.println("==================================");
                System.out.println("New device connected");
                System.out.println("Lamport clock after device connection: " + lamportClock.getTime());

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

    private static void GEThandler(BufferedReader in, PrintWriter out) {
        System.out.println("Detected GET request from client");
        int receivedLamport = lamportClock.parseReceivedClock(in);
        System.out.println("Received Lamport clock from client: " + receivedLamport);

        if (receivedLamport == -1) {
            System.out.println("Invalid Lamport clock from client");
            return;
        }
        lamportClock.updateTime(receivedLamport);
        System.out.println("Updated Lamport clock after GET request: " + lamportClock.getTime());

        lamportClock.increment();
        out.println("Lamport-Clock: " + lamportClock.getTime());
        if (!jsonStorage.isEmpty()) {
            System.out.println("Sending JSON to client");
            jsonStorage.remove("lamport_clock");
            out.println(jsonStorage);
        } else {
            out.println("JSON Storage is Empty");
            return;
        }
    }
    // handler function for PUT requests
    private static void PUThandler(BufferedReader in, PrintWriter out) {
        System.out.println("Detected PUT request from content server");
        int receivedLamportClock = -1;
        String receivedID = null;
        try {
            // reading http header
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
                if (line.startsWith("Lamport-Clock: ")) {
                    receivedLamportClock = Integer.parseInt(line.split(":")[1].trim());
                    lamportClock.updateTime(receivedLamportClock);
                    System.out.println("Updated Lamport clock after receiving PUT request: " + lamportClock.getTime());
                }
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

                if (jsonData.isEmpty()) {
                    out.println("204 Status code: No content received");
                    System.out.println("Content server gave no data");
                    return;
                }
                if (!jsonData.containsKey("id")) {
                    out.println("JSON rejected, no ID");
                    System.out.println("Content server JSON gave no id");
                    return;
                } else {
                    receivedID = (String) jsonData.get("id");
                    // System.out.println("ID received is: " + receivedID);
                }
                System.out.println("Received JSON from content server");

                // store the received JSON data
                storeJson(jsonData, receivedID);

                // add content server's lamport clock to priority queue
                addToQueue(new contentServer(receivedID, lamportClock.getTime()));

                // send confirmation to content server
                lamportClock.increment();
                System.out.println("Lamport clock incremented before sending confirmation: " + lamportClock.getTime());
                if (jsonStorage != null || jsonStorage == jsonData) {
                    out.println("Content stored / updated");
                } else {
                    out.println("Failed to update storage");
                }
                if (initStorage) {
                    out.println("200 Successful connection");
                    initStorage = false;
                } else {
                    out.println("201 - HTTP_CREATED");
                }
                System.out.println("Final Lamport clock after PUT request: " + lamportClock.getTime());
                out.println("Lamport-Clock: " + lamportClock.getTime());
                out.flush();
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
                // System.out.println("Initial Lamport clock in clientHandler: " +
                // lamportClock.getTime());
                lamportClock.increment();
                System.out.println("Lamport clock incremented after receiving request: " + lamportClock.getTime());
                if (req != null && req.startsWith("GET")) {
                    GEThandler(in, out);
                } else if (req != null && req.startsWith("PUT")) {
                    PUThandler(in, out);
                    displayContentServerQueue();
                } else {
                    out.println("400 Error - Invalid Request");
                }

                System.out.println("Final Lamport clock after clientHandler: " + lamportClock.getTime());

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

    @SuppressWarnings("unchecked")
    public static void storeJson(JSONObject jsonObject, String serverID) {
        File file = new File(storageFilePath);
        JSONObject jsonNest = new JSONObject();

        // Set the lamport clock in the jsonObject
        jsonObject.put("lamport_clock", lamportClock.getTime());

        // Try to read the existing JSON data from the file
        if (file.exists()) {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(file)) {
                // Parse existing JSON data
                jsonNest = (JSONObject) parser.parse(reader);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        // Add the new JSON object to the existing JSON nest
        jsonNest.put(serverID, jsonObject);

        // Write the updated JSONObject back to the file
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonNest.toJSONString());
            System.out.println("Stored JSON object to file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject readJson() {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(storageFilePath))) {
            // Parse the JSON file
            jsonObject = (JSONObject) parser.parse(reader);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("ParseException: " + e.getMessage());
        }
        return jsonObject;
    }

    public static void displayContentServerQueue() {
        System.out.println("Current Content Server Lamport Queue:");
        for (contentServer server : contentServerLamportQueues) {
            System.out.println(server);
        }
    }

    public static void addToQueue(contentServer contentServer){
        if (contentServerLamportQueues.size() >= 20){
            contentServerLamportQueues.poll();
        }

        for (contentServer cs : contentServerLamportQueues) {
            if (cs.getID().equals(contentServer.getID())) { 
                contentServerLamportQueues.remove(cs);
                System.out.println("Content Server already in Queue, updated position: " + cs);
                break;
            }
        }
        contentServerLamportQueues.offer(contentServer);
        return;
    }
}

class LamportClock {
    private int time;

    public LamportClock() {
        this.time = 1;
    }

    public void increment() {
        this.time++;
        System.out.println("Lamport clock incremented: " + this.time);
    }

    public void updateTime(int receivedTime) {
        this.time = Math.max(receivedTime, this.time) + 1;
        System.out.println("Lamport clock updated after receiving: " + receivedTime + ", new time: " + this.time);
    }

    public int getTime() {
        return this.time;
    }

    public int parseReceivedClock(BufferedReader in) {
        String lamportString = null;
        int receivedLamportClock = -1;
        try {
            lamportString = in.readLine();
            if (lamportString != null && lamportString.startsWith("Lamport-Clock: ")) {
                receivedLamportClock = Integer.parseInt(lamportString.split(":")[1].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        System.out.println("Client - Lamport-Clock: " + receivedLamportClock);
        return receivedLamportClock;
    }
}


class contentServer implements Comparable<contentServer> {
 
    String ID;
    int lamportClock;
 
    public contentServer(String ID, int lamportClock) {
        this.ID = ID;
        this.lamportClock = lamportClock;
    }
 
    public String getID() {
        return ID;
    }
 
    public int getLamportClock() {
        return lamportClock;
    }
     
    @Override
    public int compareTo(contentServer other) {
        // Compare contentServer objects based on their lamportClock
        return Integer.compare(this.lamportClock, other.lamportClock);
    }
    @Override
    public String toString() {
        return "ID: " + ID + ", Lamport Clock: " + lamportClock;
    }
}
