package com.mycompany.app;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GETClientTest {

    @Test
    void testParseServerInfo_validInput() {
        String serverInfo = "http://localhost:4567";
        String[] result = GETClient.parseServerInfo(serverInfo);
        assertNotNull(result);
        assertEquals("localhost", result[0]);
        assertEquals("4567", result[1]);
    }

    @Test
    void testParseServerInfo_invalidInput() {
        String serverInfo = "invalidInput";
        String[] result = GETClient.parseServerInfo(serverInfo);
        assertNull(result);
    }

    @Test
    void testParseServerJson_validJson() {
        StringBuilder jsonResponse = new StringBuilder();
        jsonResponse.append("{\"temperature\": 25, \"humidity\": 60}");
        
        // Simulate parsing JSON response
        GETClient client = new GETClient();
        client.parseServerJson(jsonResponse);
        
        // parseServerJson outputs the value directly to terminal via System.out.println,
        // please compare the outputted value against the inputted json
    }

    @Test
    void testParseReceivedClock_validInput() throws IOException {
        BufferedReader mockReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockReader.readLine()).thenReturn("Lamport-Clock: 5");

        LamportClock clock = new LamportClock();
        int receivedClock = clock.parseReceivedClock(mockReader);

        assertEquals(5, receivedClock);
    }

    @Test
    void testParseReceivedClock_invalidInput() throws IOException {
        BufferedReader mockReader = Mockito.mock(BufferedReader.class);
        Mockito.when(mockReader.readLine()).thenReturn("Invalid-Header: 5");

        LamportClock clock = new LamportClock();
        int receivedClock = clock.parseReceivedClock(mockReader);

        assertEquals(-1, receivedClock);
    }

    @Test
    void testLamportClock_updateTime() {
        LamportClock clock = new LamportClock();
        clock.updateTime(5);
        assertEquals(6, clock.getTime());
        
        clock.updateTime(3);
        assertEquals(7, clock.getTime()); // Should remain 7 since 6 > 3
    }

    @Test
    void testLamportClock_increment() {
        LamportClock clock = new LamportClock();
        clock.increment();
        assertEquals(2, clock.getTime());
    }
}
