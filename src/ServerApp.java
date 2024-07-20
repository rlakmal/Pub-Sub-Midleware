import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerApp {
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Valid Input: java ServerApp <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client has connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean isPublisher;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                setupStreams();
                processClient();
            } catch (IOException e) {
                System.out.println("ClientHandler error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void setupStreams() throws IOException {
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
            String role = this.in.readLine();
            this.isPublisher = role.equalsIgnoreCase("PUBLISHER");
        }

        private void processClient() throws IOException {
            String inputLine;
            while ((inputLine = this.in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);
                if (this.isPublisher) {
                    broadcastMessagesToSubs(inputLine);
                }
                if (inputLine.equals("terminate")) {
                    break;
                }
            }
        }

        private void broadcastMessagesToSubs(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (!client.isPublisher) {
                        synchronized (client.out) {
                            client.out.println("Broadcast: " + message);
                        }
                    }
                }
            }
        }

        private void cleanup() {
            try {
                System.out.println("Client disconnected: " + this.clientSocket.getInetAddress());
                clients.remove(this);
                if (this.out != null) {
                    this.out.close();
                }
                if (this.in != null) {
                    this.in.close();
                }
                if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                    this.clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error during cleanup: " + e.getMessage());
            }
        }
    }
}
