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
            handleClientRequest();
        } catch (SocketException e) {
            handleSocketException(e);
        } catch (IOException e) {
            handleIOException(e);
        } catch (Exception e) {
            handleOtherException(e);
        } finally {
            closeSocket();
        }
    }

    private void handleClientRequest() throws IOException {
        try (
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            StringBuilder clientRequestBuilder = new StringBuilder();
            String line;
            boolean isThereABody = false;
            int contentLength = 0;

            while (serverRunning && (line = inFromClient.readLine()) != null) {
                clientRequestBuilder.append(line).append("\r\n");
                if (line.startsWith("Content-Length:")) {
                    isThereABody = true;
                    contentLength = Integer.parseInt(line.split("Content-Length: ")[1]);
                }

                if (line.isEmpty()) {
                    if (isThereABody) {
                        appendBodyToRequest(clientRequestBuilder, inFromClient, contentLength);
                    }
                    processClientRequest(clientRequestBuilder.toString(), outToClient);
                    clientRequestBuilder.setLength(0);
                }
            }
        }
    }

    private static void appendBodyToRequest(StringBuilder clientRequestBuilder, BufferedReader inFromClient, int contentLength) {
        char[] body = new char[contentLength];
        try {
            inFromClient.read(body, 0, contentLength);
        } catch (IOException e) {
            System.err.println("Error reading body: " + e.getMessage());
        }
        clientRequestBuilder.append(body);
    }

    private void processClientRequest(String clientRequest, DataOutputStream outToClient) throws IOException {
        System.out.println(clientRequest);
        HTTPRequest httpRequest = new HTTPRequest(clientRequest);
        this.httpResponse = new HTTPResponse(httpRequest);
        System.out.println(this.httpResponse.getResponse());
        sendHttpResponseToClient(this.httpResponse, outToClient);
        outToClient.flush();
        outToClient.close();
    }

    private void handleSocketException(SocketException e) {
        String clientEndpoint = getClientEndpoint();
        System.err.println("SocketException: " + clientEndpoint + " - " + e.getMessage());
    }

    private void handleIOException(IOException e) {
        String clientEndpoint = getClientEndpoint();
        System.err.println("IOException: " + clientEndpoint + " - " + e.getMessage());
        handleResponseError();
    }

    private void handleOtherException(Exception e) {
        String clientEndpoint = getClientEndpoint();
        System.err.println(clientEndpoint + " - " + e.getMessage());
        handleResponseError();
    }

    private void handleResponseError() {
        this.httpResponse = new HTTPResponse(null);
        try {
            sendHttpResponseToClient(this.httpResponse, new DataOutputStream(clientSocket.getOutputStream()));
        } catch (IOException ex) {
            String clientEndpoint = getClientEndpoint();
            System.err.println(clientEndpoint + " - " + ex.getMessage());
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

    private void sendHttpResponseToClient(HTTPResponse httpResponse, DataOutputStream outToClient) throws IOException {
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