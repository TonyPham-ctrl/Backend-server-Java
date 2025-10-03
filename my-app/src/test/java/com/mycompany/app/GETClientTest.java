package com.mycompany.app;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

//TEST FILE FOR GETCClient.java

class GETClientTest {

    //testing a mock json response using mockito
    @Test
    void testReceiveJsonResponse() throws IOException {
        // Mock BufferedReader to simulate server sending a JSON response
        String jsonResponse = "{\"temperature\": 25, \"humidity\": 60}";
        BufferedReader mockReader = new BufferedReader(new StringReader(jsonResponse));
        GETClient client = new GETClient();
        StringBuilder receivedJson = new StringBuilder();
        String line;
        while ((line = mockReader.readLine()) != null) {
            receivedJson.append(line);
        }
        client.parseServerJson(receivedJson);
        assertTrue(receivedJson.toString().contains("\"temperature\": 25"));
        assertTrue(receivedJson.toString().contains("\"humidity\": 60"));
    }

    // testing the method responsible for parsing user's input of the server's information
    @Test
    void testParseServerInfo_validInput() {
        String serverInfo = "http://localhost:4567";
        String[] result = GETClient.parseServerInfo(serverInfo);
        assertNotNull(result);
        assertEquals("localhost", result[0]);
        assertEquals("4567", result[1]);
    }

    // testing the method responsible for parsing user's input of the server's information
    @Test
    void testParseServerInfo_invalidInput() {
        String serverInfo = "invalidInput";
        String[] result = GETClient.parseServerInfo(serverInfo);
        assertNull(result);
    }

    // testing method that parses the received lamport clock from aggregation server
    @Test
    void testParseReceivedClock_validInput() throws IOException {
        BufferedReader mockReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockReader.readLine()).thenReturn("Lamport-Clock: 5");

        LamportClock clock = new LamportClock();
        int receivedClock = clock.parseReceivedClock(mockReader);

        assertEquals(5, receivedClock);
    }

    // testing method that parses the received lamport clock from aggregation server
    @Test
    void testParseReceivedClock_invalidInput() throws IOException {
        BufferedReader mockReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockReader.readLine()).thenReturn("Invalid-Header: 5");

        LamportClock clock = new LamportClock();
        int receivedClock = clock.parseReceivedClock(mockReader);

        assertEquals(-1, receivedClock);
    }

    // testing method that is responsible for updating lamport clock time
    @Test
    void testLamportClock_updateTime() {
        LamportClock clock = new LamportClock();
        clock.updateTime(5);
        assertEquals(6, clock.getTime());
        
        clock.updateTime(3);
        assertEquals(7, clock.getTime()); // Should remain 7 since 6 > 3
    }

    // testing method that is responsible for incrementing lamport clock
    @Test
    void testLamportClock_increment() {
        LamportClock clock = new LamportClock();
        clock.increment();
        assertEquals(2, clock.getTime());
    }
}
