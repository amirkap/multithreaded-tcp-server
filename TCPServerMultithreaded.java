import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


class ThreadRunnable implements Runnable {
    static List<ThreadRunnable> connectedClients = new ArrayList<>();
    private Socket clientSocket;
    private String clientIP;
    private String clientPort;
    static volatile boolean serverRunning = true;

    ThreadRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientIP = clientSocket.getInetAddress().getHostAddress();
        this.clientPort = Integer.toString(clientSocket.getPort());
    }

    public Socket getClientSocket() {
        return this.clientSocket;
    }

    private static synchronized void addUser(ThreadRunnable client) {
        connectedClients.add(client);
    }

    private static synchronized void removeUser(ThreadRunnable client) {
        connectedClients.remove(client);
    }

    private static synchronized int getUserCount() {
        return connectedClients.size();
    }

    private static synchronized String getUserCountMessage() {
        return String.format("There are %d users connected.", getUserCount());
    }

    private String getJoinMessage() {
        return String.format("[%s] joined", this.clientIP);
    }

    private String getFormattedClientMsg(String message) {
        return String.format("(%s:%s): %s",
                this.clientIP, this.clientPort, message);
    }

    @Override
    public void run() {
        try (
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            outToClient.writeBytes("Welcome to RUNI Computer Networks 2024 chat server! " + getUserCountMessage() + System.getProperty("line.separator"));
            addUser(this);
            String joinMessage = getJoinMessage();
            sendToOtherClients(joinMessage);
            String clientSentence;
            while (serverRunning && (clientSentence = inFromClient.readLine()) != null) {
                String formattedMessage = getFormattedClientMsg(clientSentence);
                sendToOtherClients(formattedMessage);
            }
        } catch (IOException e) {
            String clientEndpoint = this.clientIP + ":" + this.clientPort;
            System.err.println(clientEndpoint + " - " + e.getMessage());
        } finally {
            try {
                if (serverRunning) {
                    String clientEndpoint = this.clientIP + ":" + this.clientPort;
                    System.out.println(clientEndpoint + " disconnected!");
                    String leaveMessage = String.format("%s has left the chat.", clientEndpoint);
                    sendToOtherClients(leaveMessage);
                    clientSocket.close();
                    removeUser(this);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void sendToOtherClients(String message) {
        for (ThreadRunnable client : connectedClients) {
            if (client != this) {
                try {
                    DataOutputStream outToOtherClient = new DataOutputStream(client.clientSocket.getOutputStream());
                    outToOtherClient.writeBytes(message + System.getProperty("line.separator"));
                    outToOtherClient.flush();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
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
            System.out.println(System.getProperty("line.separator") + "Shutting down server...");
            notifyClientsAboutShutdown();
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

    private static void notifyClientsAboutShutdown() {
        String shutdownMessage = "Server was shut down, you are no longer connected.";
        for (ThreadRunnable clientThread : ThreadRunnable.connectedClients) {
            try {
                Socket clientSocket = clientThread.getClientSocket();
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
                outToClient.writeBytes(shutdownMessage + System.getProperty("line.separator"));
                outToClient.flush();
                outToClient.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
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