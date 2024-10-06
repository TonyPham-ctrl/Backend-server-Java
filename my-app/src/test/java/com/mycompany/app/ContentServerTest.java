package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.*;

// TESTING FILE FOR ContentServer.java
public class ContentServerTest {
    
    private ContentServer contentServer;

    //set up for unit test
    @BeforeEach
    public void setUp() {
        contentServer = new ContentServer();
    }

    // testin lamport clock incrementing
    @Test
    public void testLamportClockIncrement() {
        LamportClockContent lamportClock = new LamportClockContent();
        int initialTime = lamportClock.getTime();
        lamportClock.increment();
        assertEquals(initialTime + 1, lamportClock.getTime(), "Lamport clock should increment by 1");
    }

    //testing updating lamport clock against received lamport clock
    @Test
    public void testUpdateTime() {
        LamportClockContent lamportClock = new LamportClockContent();
        lamportClock.updateTime(5);
        assertEquals(6, lamportClock.getTime(), "Lamport clock should update to the maximum received time + 1");
    }

    // testing file reader method
    @Test
    public void testFileReaderLoop() {
        String filePath = "/home/tonypham/distributedSystem2024S2/my-app/src/test/java/com/mycompany/app/testFile1.txt";  // Provide a test file path
        JSONObject jsonFile = new JSONObject();
        ContentServer.fileReaderLoop(filePath, jsonFile);
        // validate that jsonfile is populated
        assertTrue(jsonFile.size() > 0);
    }


    // testing file reader method against invalid entry
    @Test
    public void testFileReaderInvalidEntry() throws IOException {
        Path tempFile = Files.createTempFile("testFileInvalid", ".txt");
        Files.write(tempFile, List.of("invalidEntryWithoutColon"));
        JSONObject jsonFile = new JSONObject();
        ContentServer.fileReaderLoop(tempFile.toString(), jsonFile);
        // Assert that the jsonFile remains empty due to invalid input
        assertTrue(jsonFile.isEmpty(), "Should not add any entries for invalid input");
        Files.delete(tempFile);
    }


    //testing sending a PUT request
    @Test
    public void testPUTreq() {
        // mocking socket behaviour with mockito
        try {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("HTTP/1.1 200 OK\nLamport-Clock: 3\n".getBytes()));
            when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
            // Replace the actual socket connection in PUTreq with the mocked one
            contentServer.PUTreq();
        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
    }

    //clean up after unit test
    @AfterEach
    public void tearDown() {
        contentServer = null;
    }
}
