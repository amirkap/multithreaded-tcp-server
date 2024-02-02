import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ChatRunnable implements Runnable {
    static List<ChatRunnable> connectedClients = new ArrayList<>();
    private Socket clientSocket;
    private String clientIP;
    private String clientPort;
    static volatile boolean serverRunning = true;

    ChatRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientIP = clientSocket.getInetAddress().getHostAddress();
        this.clientPort = Integer.toString(clientSocket.getPort());
    }

    public Socket getClientSocket() {
        return this.clientSocket;
    }

    private static synchronized void addUser(ChatRunnable client) {
        connectedClients.add(client);
    }

    private static synchronized void removeUser(ChatRunnable client) {
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
                // If the server is shutting down, shutdown hook will close the socket
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
        for (ChatRunnable client : connectedClients) {
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

    private static final int PORT = 9922;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        ExecutorService executor = Executors.newCachedThreadPool(); // Dynamic number of threads
        System.out.println("Server is listening on port " + PORT + "...");

        // This thread will be executed when the server is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ChatRunnable.serverRunning = false;
            System.out.println(System.getProperty("line.separator") + "Shutting down server...");
            notifyClientsAboutShutdown();
        }));

        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(
                        clientSocket.getInetAddress().getHostAddress() + ':' + clientSocket.getPort()
                                + " connected!");
                Runnable worker = new ChatRunnable(clientSocket);

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
        for (ChatRunnable clientThread : ChatRunnable.connectedClients) {
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
}