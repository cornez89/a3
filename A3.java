/** Web server program
 *
 *  @author Joshua Hill, Zachary Cornez
 *
 *  @version CS 391 - Fall 2024 - A3
 **/

import java.io.*;
import java.net.*;
import java.nio.file.Files;

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
        try 
        {
           serverSocket = new ServerSocket(portNumber);
           System.out.println("%% Server started: " + serverSocket);
  
           while (true)
           {
                System.out.println("%% Waiting for client...");
                clientSocket = serverSocket.accept();

               (new Thread( new WebServer(clientSocket))).start();
           }
       }
       catch (IllegalThreadStateException itse)
       {
            System.out.println("Thread error in A3.main(): " + itse.getMessage());
       }
       catch (IllegalArgumentException iae)
       {
            System.out.println("Argument error in A3.main(): " + iae.getMessage());
       }
       catch (SecurityException se)
       {
            System.out.println("Security error in A3.main(): " + se.getMessage());
       }
       catch (SocketTimeoutException ste)
       {
            System.out.println("Socket timeout error in A3.main(): " + ste.getMessage());
       }
       catch (java.nio.channels.IllegalBlockingModeException ibme)
       {
            System.out.println("Blocking error in A3.main(): " + ibme.getMessage());
       }
       catch (IOException ioe)
       {
            System.out.println("IO error in A3.main(): " + ioe.getMessage());
       }
       catch (Exception e)
       {
            System.out.println("Error in A3.main(): " + e.getMessage());
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
        this.clientSocket = clientSocket;

        try
        {               
            openStreams(clientSocket);
            System.out.println("%% New connection established: " + clientSocket);
            System.out.printf("%%%% [# of connected clients: %d]\n", ++numConnections);
        } 
        catch (IOException ioe)
        {
            System.out.println("IO error in openStreams(): " + ioe.getMessage());
        }
        catch (Exception e)
        {
            System.out.println("Error in Webserver class constructor: " + e.getMessage());
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
        final String REQUEST_GET = "GET";
        final String REQUEST_418 = "/coffee";
        final String REQUEST_503 = "/tea/coffee";
        final String RESPONSE_405 = "Method not allowed";
        final String REPSONSE_418 = "I'm a teapot";
        final String REPSONSE_503 = "Coffee is temporarily unavailable";
        final int CODE_405 = 405;
        final int CODE_418 = 418;
        final int CODE_503 = 503;

        try 
        {
            String requestLine[] = parseRequest();

            if(!requestLine[0].equals(REQUEST_GET))
            {
                writeCannedResponse(requestLine[2], CODE_405, RESPONSE_405);
            } 
            else
            {
                switch (requestLine[1]) 
                {
                    case REQUEST_418:
                        writeCannedResponse(requestLine[2], CODE_418, REPSONSE_418);
                        break;

                    case REQUEST_503:
                        writeCannedResponse(requestLine[2], CODE_503, REPSONSE_503);
                        break;
                
                    default:
                        File file = new File(System.getProperty("user.dir") + requestLine[1]);

                        if(file.exists())
                        {
                            write200Response(requestLine[2], loadFile(file), requestLine[1]);
                        }
                        else
                        {
                            write404Response(requestLine[2], requestLine[1]);
                        }
                        break;
                }
            }
        } 
        catch (IllegalArgumentException iae)
        {
            System.out.println("Illegal argument error in processRequest(): " + iae.getMessage());
        }
        catch (SecurityException se)
        {
            System.out.println("Security error in processRequest(): " + se.getMessage());
        }
        catch (NullPointerException npe)
        {
            System.out.println("Null pointer error in processRequest(): " + npe.getMessage());
        }
        catch (IOException ioe)
        {
            System.out.println("IO error in processRequest(): " + ioe.getMessage());
        }
        catch (Exception e)
        {
            System.out.println("Error in processRequest(): " + e.getMessage());
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
        final String CONSOLE_REQUEST_START = "\n*** request ***\n";
        final String CONSOLE_SPACING = "     %s\n";

        String line = in.readLine();
        String requestLine[] = line.split("\\s");

        System.out.printf(CONSOLE_REQUEST_START);
        while(!line.equals(""))
        {
            System.out.printf(CONSOLE_SPACING, line);
            line = in.readLine();
        }

        return requestLine;
    }// parseRequest method

    /* Given a File object for a file that we know is stored on the
       server, return the contents of the file as a byte array
    **/
    byte[] loadFile(File file)
    {
        byte htmlBytes[] = new byte[0];
    
        try 
        {
            htmlBytes = Files.readAllBytes(file.toPath());
        } 
        catch (java.nio.file.InvalidPathException ipe)
        {
            System.out.println("File path error in loadFile(): " + ipe.getMessage());
        }
        catch (IOException ioe)
        {
            System.out.println("IO error in loadFile(): " + ioe.getMessage());
        }
        catch (OutOfMemoryError oome)
        {
            System.out.println("Memory error in loadFile(): " + oome.getMessage());
        }
        catch (SecurityException se)
        {
            System.out.println("Security error in loadFile(): " + se.getMessage());
        }
        catch (Exception e)
        {
            System.out.println("Error in loadFile(): " + e.getMessage());
        }

        return htmlBytes;
    }// loadFile method

    /* Given an HTTP protocol description string, a byte array, and a file
       name, send back to the client a 200 HTTP response whose body is the
       input byte array. The file name is used to determine the type of
       Web resource that is being returned. The set of required header
       fields and file types is spelled out in the A3 handout.
    **/
    void write200Response(String protocol, byte[] body, String pathToFile)
    {
        final String CONSOLE_RESPONSE_START = "\n*** Response ***\n";
        final String FILE_CONTENTS = "<file contents not shown>";
        final String CONSOLE_SPACING = "     %s\n";
        final String SEPARATOR = "\r\n";

        String responseStatus = String.format("%s 200 Document Follows\r", protocol);
        String responseContentLength = String.format("Content-Length: %d\r\n", body.length);

        try 
        {
            out.write(responseStatus.getBytes());
            out.write(responseContentLength.getBytes());
            out.write(SEPARATOR.getBytes());
            out.write(body);

            System.out.printf(CONSOLE_RESPONSE_START);
            System.out.printf(CONSOLE_SPACING, responseStatus);
            System.out.printf(CONSOLE_SPACING, responseContentLength);
            System.out.printf(CONSOLE_SPACING, FILE_CONTENTS);
            System.out.println();
        } 
        catch (IOException ioe) 
        {
            System.out.println("IO error in write200Response(): " + ioe.getMessage());
        }
        catch (Exception e) 
        {
            System.out.println("Error in write200Response(): " + e.getMessage());
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
        final String CONSOLE_RESPONSE_START = "\n*** Response ***\n";
        final String CONSOLE_SPACING = "     %s\n";
        final String RESPONSE_CONTENT_TYPE = "ContentType: text/html\r\n";
        final String SEPARATOR = "\r\n";

        String html404 = String.format("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Page not found</title></head><body><h1>HTTP Error 404 Not Found</h1><h2>The file <span style=\"color: red\">%s</span> does not exist on this server.</h2></html></body>", pathToFile);
        String responseStatus = String.format("%s Not found\r", protocol);

        try 
        {
            out.write(responseStatus.getBytes());
            out.write(RESPONSE_CONTENT_TYPE.getBytes());
            out.write(SEPARATOR.getBytes());
            out.write(html404.getBytes());

            System.out.printf(CONSOLE_RESPONSE_START);
            System.out.printf(CONSOLE_SPACING, responseStatus);
            System.out.printf(CONSOLE_SPACING, RESPONSE_CONTENT_TYPE);
            System.out.printf(CONSOLE_SPACING, html404.toString());
            System.out.println();
        } 
        catch (IOException ioe) 
        {
            System.out.println("IO error in write404Response(): " + ioe.getMessage());
        }
        catch (Exception e) 
        {
            System.out.println("Error in write404Response(): " + e.getMessage());
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
        final String CONSOLE_RESPONSE_START = "\n*** Response ***\n";
        final String CONSOLE_SPACING = "     %s\n";
        final String RESPONSE_CONTENT_TYPE = "ContentType: text/html\r\n";
        final String SEPARATOR = "\r\n";
        final int CODE_405 = 405;
        final int CODE_418 = 418;
        final int CODE_503 = 503;
        final String FILE_405_PATH = "html\\405.html";
        final String FILE_418_PATH = "html\\418.html";
        final String FILE_503_PATH = "html\\503.html";

        String responseFilePath = "";
        String contentsNotShown = "";
        String responseStatus = String.format("%s %d %s\r", protocol, code, description);

        switch (code) 
        {
            case CODE_405:
                responseFilePath = FILE_405_PATH;
                break;
        
            case CODE_418:
                responseFilePath = FILE_418_PATH;
                break;

            case CODE_503:
                responseFilePath = FILE_503_PATH;
                break;
        }

        contentsNotShown = String.format("<contents of %s not shown>\n", responseFilePath);

        try 
        {
            out.write(responseStatus.getBytes());
            out.write(RESPONSE_CONTENT_TYPE.getBytes());
            out.write(SEPARATOR.getBytes());

            byte[] htmlBytes = loadFile(new File(responseFilePath));
            out.write(htmlBytes);
            
            System.out.printf(CONSOLE_RESPONSE_START);
            System.out.printf(CONSOLE_SPACING, responseStatus);
            System.out.printf(CONSOLE_SPACING, RESPONSE_CONTENT_TYPE);
            System.out.printf(CONSOLE_SPACING, contentsNotShown);
        } 
        catch (NullPointerException npe) 
        {
            System.out.println("Null pointer error in writeCannedResponse: " + npe.getMessage());
        } 
        catch (IOException ioe) 
        {
            System.out.println("IO error in writeCannedResponse: " + ioe.getMessage());
        } 
        catch (Exception e) 
        {
            System.out.println("Error in writeCannedResponse(): " + e.getMessage());
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
        try
        {
            System.out.println("%% Connection Released: " + clientSocket);
            System.out.printf("%%%% [# of connected clients: %d]\n", --numConnections);

            if (in != null)           { in.close();           } 
            if (out != null)          { out.close();          } 
            if (clientSocket != null) { clientSocket.close(); }
        } 
        catch (IOException ioe)
        {
            System.err.println("IO error in close(): " + ioe.getMessage());
        }   
        catch (Exception e)
        {
            System.err.println("Error in close(): " + e.getMessage());
        }  
    }// close method

}// WebServer class
