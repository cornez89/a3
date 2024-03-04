/** Web server program
 *
 *  @author Zacharey Cornell
 *          Joshua Taylor-Hill
 *
 *  @version CS 391 - Fall 2024 - A3
 **/

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class A3
{
    static ServerSocket serverSocket = null;  // listening socket
    static int portNumber = 5555;             // port on which server listens
    static Socket clientSocket = null;        // socket to a client
    
    /* Start the server then repeatedly wait for a connection request, accept,
       and start a new thread to service the request
     */
    public static void main(String args[])
    {
        try {
            serverSocket = new ServerSocket(portNumber);
            clientSocket = serverSocket.accept();
            new Thread(new WebServer(clientSocket)).run();
        } catch (IOException ioe) {
            System.out.print("Failed to set up Server Socket");
        }

    }// main method
}// A3 class

class WebServer implements Runnable
{
    static int numConnections = 0;           // number of ongoing connections
    Socket clientSocket = null;              // socket to client    
    BufferedReader in = null;                // input stream from client
    DataOutputStream out = null;             // output stream to client

    /* Store a reference to the client socket, update and display the
       number of connected clients, and open I/O streams
    **/
    WebServer(Socket clientSocket)
    {
        try {
            openStreams(clientSocket);
        } catch (IOException ioe) {
            System.out.println("Failed to set up client Socket");
        }

    }// constructor

    /* Each WebServer thread processes one HTTP GET request and
       then closes the connection
    **/
    public void run()
    {
        processRequest();
        close();
    }// run method

    /* Parse the request then send the appropriate HTTP response
       making sure to handle all of the use cases listed in the A3
       handout, namely codes 200, 404, 418, 405, and 503 responses.
    **/
    void processRequest()
    {
        String[] request;
            String getString;
            String protocol;
            String filePath;            
            Scanner parser; 
            byte[] file;
            final String RESPONSE_418 = "coffee";
            final String RESPONSE_503 = "tea/coffee";
        try {
            request = parseRequest();  //full reques
            getString = request[0];      //GET line
            parser = new Scanner(getString);
//Seperate the file path and the protocol
            if(!parser.next().equals("GET")) {
                filePath = parser.next().substring(1);          //chop out the first "/"
                protocol = parser.next();
            } else {
                throw new Exception("GET command required.");
            }
        //Complete the request                
            // filter out /coffee and /tea/coffee
            switch (filePath) {
                case RESPONSE_418:
                    writeCannedResponse(protocol, 418, RESPONSE_503);
                    break;
                case RESPONSE_503:
                    writeCannedResponse(protocol, 503, RESPONSE_503);
                    break;
                default: 
                    try {
                        file = loadFile(new File(getString));
                        write200Response(protocol, file, filePath);
                    } catch (FileNotFoundException fnef) {
                        write404Response(protocol, filePath);
                    }        
            }
        } catch (IOException ioe) {
            System.out.println("Problem parseing requesting");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }// processRequest method

    /* Read the HTTP request from the input stream line by line up to
       and including the empty line between the header and the
       body. Send to the console every line read (except the last,
       empty line). Then extract from the first line the HTTP command,
       the path to the requested file, and the protocol description string and
       return these three strings in an array.
    **/
    String[] parseRequest() throws IOException
    {
        ArrayList<String> lines = new ArrayList<String>();
        String[] parsedRequest;
        String line = ""; 
        boolean endOfFile = false;
        final String END_MARKER = "body:"; 
        while( in.ready() && !endOfFile ) {
            line = in.readLine();
            if (line.length() > 4 && line.substring(0,4).equals(END_MARKER)) {
                endOfFile = true;
            } else {
                lines.add(line);
                System.out.println(line);
            }
        }
        parsedRequest = new String[lines.size()];
        for(int i = 0; i < lines.size(); i++) {
            parsedRequest[i] = lines.get(i);
        }
        return parsedRequest;
    }// parseRequest method

    /* Given a File object for a file that we know is stored on the
       server, return the contents of the file as a byte array
    **/
    byte[] loadFile(File file) throws IOException
    {
        return Files.readAllBytes(file.toPath());
    }// loadFile method

    /* Given an HTTP protocol description string, a byte array, and a file
       name, send back to the client a 200 HTTP response whose body is the
       input byte array. The file name is used to determine the type of
       Web resource that is being returned. The set of required header
       fields and file types is spelled out in the A3 handout.
    **/
    void write200Response(String protocol, byte[] body, String pathToFile)
    {
//Make header
        int contentLength = body.length;
        String response = protocol + " 200 Document Follows\n";
        response += "Content-Length: " + contentLength + "\n\n"; 
//Add body
        response += body;
//send
        try {
            out.writeUTF(response);
        } catch (IOException ioe) {
            System.out.println("Problem writing 200 Response");
        }
    }// write200Response method

    /* Given an HTTP protocol description string and a path that does not refer
       to any of the existing files on the server, return to the client a 404 
       HTTP response whose body is a dynamically created page whose content
       is spelled out in the A3 handout. The only HTTP header to be included
       in the response is "Content-Type".
    **/
    void write404Response(String protocol, String pathToFile)
    { 
        String response = protocol + " 404 Not found\n";
        response += "Content-Type: " + pathToFile + "\n\n"; 
//Add body
        response += "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Page not found</title></head><body><h1>HTTP Error 404 Not Found</h1><h2>The file <span style=\"color: red\">/" + pathToFile + "</span> does not exist on this server.</h2></body></html>";
        try {
            out.writeUTF(response);
        } catch (IOException ioe) {
            System.out.println("Problem writing 404 Response");
        }
    }// write404Response method

    /* Given an HTTP protocol description string, a byte array, and a file
       name, send back to the client a 200 HTTP response whose body is the
       input byte array. The file name is used to determine the type of
       Web resource that is being returned. The only HTTP header to be included
       in the response is "Content-Type".
    **/
    void writeCannedResponse(String protocol, int code, String description)
    {
        switch(code) {
            case 200://how do we get the file information?
                description
                write200Response(protocol,, description);
            case 400:
                write404Response(protocol, description);
            case 418:
            case 503:
            
        }

    }// writeCannedResponse method

    /* open the necessary I/O streams and initialize the in and out
       variables; this method does not catch any IO exceptions.
    **/    
    void openStreams(Socket clientSocket) throws IOException
    {
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new DataOutputStream(clientSocket.getOutputStream());

    }// openStreams method

    /* close all open I/O streams and sockets; also update and display the
       number of connected clients.
    **/
    void close()
    {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException ioe) {
            System.out.println("Issue closing socket");
        }

    }// close method

}// WebServer class
