import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


class ThreadRunnable implements Runnable {
    private final Socket clientSocket;
    private final String clientIP;
    private final String clientPort;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    static volatile boolean serverRunning = true;
    private HTTPResponse httpResponse;

    ThreadRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientIP = clientSocket.getInetAddress().getHostAddress();
        this.clientPort = Integer.toString(clientSocket.getPort());
    }

    @Override
    public void run() {
        try {
            openStreams();
            handleClientRequest();
        } catch (SocketException e) {
            handleSocketException(e);
        } catch (IOException e) {
           // System.out.println("Is socket closed? " + clientSocket.isClosed());
            handleIOException(e);
        } catch (Exception e) {
            handleOtherException(e);
        } finally {
            closeResources();
        }
    }

    private void handleClientRequest() throws IOException {
            StringBuilder clientRequestBuilder = new StringBuilder();
            String line;
            boolean isThereABody = false;
            int contentLength = 0;
            clientSocket.setSoTimeout(5000); // Timeout for header reading, 5 seconds is only for debugging, we should still determine the best value
            while (serverRunning && (line = inFromClient.readLine()) != null) {
                clientRequestBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    isThereABody = true;
                    contentLength = Integer.parseInt(line.split("Content-Length: ")[1]);
                }

                if (line.isEmpty()) {
                    if (isThereABody) {
                        // different timeout for reading the body
                        clientSocket.setSoTimeout(5000); // Timeout for header reading, 5 seconds is only for debugging, we should still determine the best value
                        appendBodyToRequest(clientRequestBuilder, contentLength);
                    }
                    processClientRequest(clientRequestBuilder.toString());
                    clientRequestBuilder.setLength(0);
                }
            }
        
    }

    private void appendBodyToRequest(StringBuilder clientRequestBuilder, int contentLength) throws IOException {
        char[] body = new char[contentLength];
            int charsRead = inFromClient.read(body, 0, contentLength);
            if (charsRead != contentLength) {
                throw new IOException("Error reading body: " + charsRead + " chars read, " + contentLength + " expected");
            }
        clientRequestBuilder.append(body);
    }

    private void processClientRequest(String clientRequest) throws IOException {
        System.out.println(clientRequest);
        HTTPRequest httpRequest = new HTTPRequest(clientRequest);
        this.httpResponse = new HTTPResponse(httpRequest);
        System.out.println(this.httpResponse.getResponse());
        sendHttpResponseToClient(this.httpResponse);
        outToClient.flush();
    }

    private void handleSocketException(SocketException e) {
        String clientEndpoint = getClientEndpoint();
        System.err.println("SocketException: " + clientEndpoint + " - " + e.getMessage());
        sendErrorResponse(e);
    }

    private void handleIOException(IOException e) {
        String clientEndpoint = getClientEndpoint();
        String exceptionName = e instanceof SocketTimeoutException ? "SocketTimeoutException" : "IOException";
        System.err.println(exceptionName + ": " + clientEndpoint + " - " + e.getMessage());
        sendErrorResponse(e);
    }

    private void handleOtherException(Exception e) {
        String clientEndpoint = getClientEndpoint();
        System.err.println(clientEndpoint + " - " + e.getMessage());
        sendErrorResponse(e);
    }

    private void sendErrorResponse(Exception e) {
        if (e instanceof SocketTimeoutException) {
            this.httpResponse = new HTTPResponse(new HTTPRequest(true));
        } else if (e.getMessage().startsWith("Error reading body")) {
            this.httpResponse = new HTTPResponse(new HTTPRequest("")); // invalid request
        } else {
            this.httpResponse = new HTTPResponse(null);
        }
        try {
            sendHttpResponseToClient(this.httpResponse);
        } catch (IOException ex) {
            String clientEndpoint = getClientEndpoint();
            System.err.println(clientEndpoint + " - " + ex.getMessage());
        }
    }

    private void openStreams() throws IOException {
        inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToClient = new DataOutputStream(clientSocket.getOutputStream());
    }

    private void closeResources() {
        closeStreams();
        closeSocket();
    }

    private void closeStreams() {
        try {
            if (inFromClient != null) {
                inFromClient.close();
            }
            if (outToClient != null) {
                outToClient.close();
            }
        } catch (IOException e) {
            String clientEndpoint = getClientEndpoint();
            System.err.println("Error closing streams for " + clientEndpoint + " - " + e.getMessage());
        }
    }

    private void closeSocket() {
        try {
            if (serverRunning) {
                String clientEndpoint = getClientEndpoint();
                System.out.println(clientEndpoint + " disconnected!");
                clientSocket.close();
            }
        } catch (IOException e) {
            String clientEndpoint = getClientEndpoint();
            System.err.println(clientEndpoint + " - " + e.getMessage());
        }
    }

    private String getClientEndpoint() {
        return this.clientIP + ":" + this.clientPort;
    }

    private void sendHttpResponseToClient(HTTPResponse httpResponse) throws IOException {
        outToClient.writeBytes(httpResponse.getResponse().toString());
    }
}

public class TCPServerMultithreaded {

    private static int PORT;
    private static final String CONFIG_FILE_PATH = "./config.ini";
    public static String ROOT;
    public static String DEFAULT_PAGE;
    public static int MAX_THREADS;


    public static void main(String[] args) throws Exception {
        ExecutorService executor = null;
        ServerSocket serverSocket = null;

        try {
            readDataFromConfigFile();
            serverSocket = new ServerSocket(PORT);
            executor = Executors.newFixedThreadPool(MAX_THREADS);
            System.out.println("Server is listening on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(
                        clientSocket.getInetAddress().getHostAddress() + ':' + clientSocket.getPort()
                                + " connected!");
                Runnable worker = new ThreadRunnable(clientSocket);
                executor.execute(worker);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            if (executor != null) {
                System.out.println("Shutting down executor...");
                executor.shutdown();
            }
            if (serverSocket != null) {
                System.out.println("Closing server socket...");
                serverSocket.close();
            }
            System.out.println("Shutting down...");
        }
    }

    private static void readDataFromConfigFile() {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(CONFIG_FILE_PATH));
            PORT = Integer.parseInt(properties.getProperty("port"));
            ROOT = properties.getProperty("root");
            DEFAULT_PAGE = properties.getProperty("defaultPage");
            MAX_THREADS = Integer.parseInt(properties.getProperty("maxThreads"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from " + CONFIG_FILE_PATH, e);
        }

    }
}