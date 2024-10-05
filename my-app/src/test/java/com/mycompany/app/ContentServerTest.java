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

public class ContentServerTest {
    
    private ContentServer contentServer;

    @BeforeEach
    public void setUp() {
        contentServer = new ContentServer();
    }

    @Test
    public void testLamportClockIncrement() {
        LamportClockContent lamportClock = new LamportClockContent();
        int initialTime = lamportClock.getTime();
        lamportClock.increment();
        assertEquals(initialTime + 1, lamportClock.getTime(), "Lamport clock should increment by 1");
    }

    @Test
    public void testUpdateTime() {
        LamportClockContent lamportClock = new LamportClockContent();
        lamportClock.updateTime(5);
        assertEquals(6, lamportClock.getTime(), "Lamport clock should update to the maximum received time + 1");
    }

    @Test
    public void testFileReaderLoop() {
        String filePath = "/home/tonypham/distributedSystem2024S2/my-app/src/test/java/com/mycompany/app/testFile1.txt";  // Provide a test file path
        JSONObject jsonFile = new JSONObject();
        
        // Now call fileReaderLoop with both arguments
        ContentServer.fileReaderLoop(filePath, jsonFile);

        // Validate if jsonFile is populated correctly
        assertTrue(jsonFile.size() > 0);
    }

    // admitedly this test case testFileReaderInvalidEntry() is AI-generated
    @Test
    public void testFileReaderInvalidEntry() throws IOException {
        // Create a temporary file with invalid content
        Path tempFile = Files.createTempFile("testFileInvalid", ".txt");
        Files.write(tempFile, List.of("invalidEntryWithoutColon"));
        JSONObject jsonFile = new JSONObject();
        ContentServer.fileReaderLoop(tempFile.toString(), jsonFile);

        // Assert that the jsonFile remains empty due to invalid input
        assertTrue(jsonFile.isEmpty(), "Should not add any entries for invalid input");
        Files.delete(tempFile);
    }

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

    @AfterEach
    public void tearDown() {
        contentServer = null;
    }
}
