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
    private HTTPRequest httpRequest;

    ThreadRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientIP = clientSocket.getInetAddress().getHostAddress();
        this.clientPort = Integer.toString(clientSocket.getPort());
    }

    public Socket getClientSocket() {
        return this.clientSocket;
    }

    @Override
    public void run() {
        try (
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            outToClient.writeBytes("Welcome to our http server! " + System.lineSeparator());

            StringBuilder clientRequestBuilder = new StringBuilder();
            String line;
            int consecutiveEmptyLines = 0;

            while (serverRunning && (line = inFromClient.readLine()) != null) {
                clientRequestBuilder.append(line).append("\r\n");

                if (line.isEmpty()) {
                    consecutiveEmptyLines++;

                    if (consecutiveEmptyLines == 1) {
                        String clientRequest = clientRequestBuilder.toString();
                        System.out.println(clientRequest);
                        try {
                            this.httpRequest = new HTTPRequest(clientRequest);
                            System.out.println(this.httpRequest);
                        } catch (IllegalArgumentException e) {
                            outToClient.writeBytes("HTTP/1.1 400 Bad Request" + System.lineSeparator());
                            outToClient.writeBytes("Content-Type: text/html" + System.lineSeparator());
                            outToClient.writeBytes("Content-Length: 0" + System.lineSeparator());
                            outToClient.writeBytes(System.lineSeparator());
                            outToClient.writeBytes(System.lineSeparator());
                            continue;
                        }

                        clientRequestBuilder.setLength(0);
                        consecutiveEmptyLines = 0;
                    }
                } else {
                    consecutiveEmptyLines = 0;
                }
            }

        } catch (IOException e) {
            String clientEndpoint = this.clientIP + ":" + this.clientPort;
            System.err.println(clientEndpoint + " - " + e.getMessage());
        } finally {
            try {
                if (serverRunning) {
                    String clientEndpoint = this.clientIP + ":" + this.clientPort;
                    System.out.println(clientEndpoint + " disconnected!");
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}

public class TCPServerMultithreaded {

    private static int PORT;
    private static final String CONFIG_FILE_PATH = "./config.ini";
    public static String ROOT;
    public static String DEFAULT_PAGE;
    public static int MAX_THREADS;


    public static void main(String[] args) throws Exception {
        readDataFromConfigFile();
        ServerSocket serverSocket = new ServerSocket(PORT);
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("Server is listening on port " + PORT + "...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadRunnable.serverRunning = false;
            System.out.println(System.lineSeparator() + "Shutting down server...");
        }));

        try {
            while (!serverSocket.isClosed()) {
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
            executor.shutdown();
            serverSocket.close();
            System.out.println("Server is closed");
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