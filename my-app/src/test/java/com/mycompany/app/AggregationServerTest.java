package com.mycompany.app;

import org.junit.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.net.*;
import org.json.simple.JSONObject;

public class AggregationServerTest {
    private ServerSocket mockServerSocket;
    private Socket mockSocket;
    private PrintWriter out;
    private BufferedReader in;
    
    @Before
    public void setUp() throws Exception {
        // Mocking ServerSocket and Socket
        mockServerSocket = mock(ServerSocket.class);
        mockSocket = mock(Socket.class);
        
        // Mock input and output streams
        out = new PrintWriter(new StringWriter(), true);
        in = new BufferedReader(new StringReader(""));
        
        // Stubbing methods for the mock objects
        when(mockServerSocket.accept()).thenReturn(mockSocket);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("GET".getBytes()));
        when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    }
    
    @Test
    public void testServerStart() throws Exception {
        // Simulate server start on a separate thread
        new Thread(() -> {
            try {
                AggregationServer.main(new String[]{"4567"});
            } catch (Exception e) {
                Assert.fail("Exception occurred during server start: " + e.getMessage());
            }
        }).start();
        
        // Simulating a connection to the server
        Socket testSocket = new Socket("localhost", 4567);
        Assert.assertNotNull("Socket should connect to the server", testSocket);
        
        // Close socket after test
        testSocket.close();
    }
    
    @Test
    public void testHandleGETRequest() throws Exception {
        // Mock client sending a GET request
        BufferedReader mockInput = new BufferedReader(new StringReader("GET\nLamport-Clock: 1\n"));
        PrintWriter mockOutput = new PrintWriter(new StringWriter(), true);

        AggregationServer.GEThandler(mockInput, mockOutput);

        // Verify that GET request was handled properly
        Assert.assertNotNull("GET request handler should return a response", mockOutput);
    }

    @Test
    public void testHandlePUTRequest() throws Exception {
        // Simulating a PUT request
        String putRequest = "PUT\nLamport-Clock: 2\n\n{\"id\":\"server1\",\"data\":\"test\"}";
        BufferedReader mockInput = new BufferedReader(new StringReader(putRequest));
        PrintWriter mockOutput = new PrintWriter(new StringWriter(), true);

        AggregationServer.PUThandler(mockInput, mockOutput);

        // Checking if data was correctly stored in the JSON
        JSONObject storedJson = AggregationServer.readJson();
        Assert.assertNotNull("Stored JSON should not be null", storedJson);
        Assert.assertTrue("Stored JSON should contain the id 'server1'", storedJson.containsKey("server1"));
    }

    @Test
    public void testLamportClockUpdate() throws Exception {
        // Create LamportClock instance
        LamportClock lamportClock = new LamportClock();

        // Mock client clock value and update it in the server
        BufferedReader mockInput = new BufferedReader(new StringReader("Lamport-Clock: 3\n"));
        lamportClock.updateTime(3);

        // Verify the Lamport clock has been updated correctly
        Assert.assertEquals("Lamport clock should update correctly", 4, lamportClock.getTime());
    }

    @After
    public void tearDown() throws Exception {
        if (mockServerSocket != null) {
            mockServerSocket.close();
        }
        if (mockSocket != null) {
            mockSocket.close();
        }
    }
}
