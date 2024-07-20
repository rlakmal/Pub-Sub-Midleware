import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientApp {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Valid Input: java ClientApp <server_ip> <port> <role>");
            System.exit(1);
        }

        String serverIp = args[0];
        int port = Integer.parseInt(args[1]);
        String role = args[2].toUpperCase();

        if (!role.equals("PUBLISHER") && !role.equals("SUBSCRIBER")) {
            System.out.println("Role is invalid. Available roles: PUBLISHER, SUBSCRIBER");
            System.exit(1);
        }

        try (Socket socket = new Socket(serverIp, port);
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to server at " + serverIp + ":" + port);

            Thread receiverThread = new Thread(() -> receiveMessages(in));
            receiverThread.start();

            out.println(role);
            handleUserInput(consoleReader, out, role);

            receiverThread.join();

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Receiver thread interrupted: " + e.getMessage());
        }
    }

    private static void receiveMessages(BufferedReader in) {
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println("Server response: " + serverResponse);
            }
        } catch (IOException e) {
            System.out.println("Error receiving server response: " + e.getMessage());
        }
    }

    private static void handleUserInput(BufferedReader consoleReader, PrintWriter out, String role) throws IOException {
        String userInput;
        while ((userInput = consoleReader.readLine()) != null) {
            out.println(userInput);
            if (userInput.equals("terminate")) {
                break;
            }
        }
    }
}
