import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.format.DateTimeFormatter;

public class Server implements Runnable {
    private List<ConnectionHandler> connections;
    private ServerSocket server;
    private Socket client;
    private boolean acceptConnections;
    private ExecutorService pool;

    public Server() {
        acceptConnections = true;
        connections = new ArrayList<>();

    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (acceptConnections) {
                client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println(e);
            shutdown();
        }
    }

    public void shutdown() {
        acceptConnections = false;
        try {
            if (!server.isClosed()) {
                server.close();
            }
            pool.shutdown();
        } catch (Exception e) {
            System.err.println("unable to close server due to " + e);
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader inp;
        private PrintWriter out;
        private String nickName;
        private DateTimeFormatter dateTime;
        private LocalDateTime curTime;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                inp = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please Enter a nick name: ");
                nickName = inp.readLine();
                dateTime = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
                curTime = LocalDateTime.now();
                System.out.println(nickName + " is connected at " + dateTime.format(curTime));
                broadCaste(nickName + " has joined chat!");
                String message;
                while ((message = inp.readLine()) != null) {
                    String msg = message.toLowerCase();
                    if (msg.startsWith("/name")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadCaste(nickName + " renamed themselves as " + messageSplit[1]);
                            System.out.println(nickName + " renamed themselves as " + messageSplit[1]);
                            nickName = messageSplit[1];
                            out.println("Successfully changed your nick name as " + nickName);
                        } else {
                            out.println("No nickname has been provided");
                        }
                    } else if (msg.startsWith("/quit")) {
                        broadCaste(nickName + " has left the chat");
                        shutdown();
                    } else {
                        broadCaste(nickName + " : " + message, this.client);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void broadCaste(String message) {
            for (ConnectionHandler ch : connections) {
                if (ch != null) {
                    ch.sendMessage(message);
                }
            }
        }

        public void broadCaste(String message, Socket sender) {
            for (ConnectionHandler ch : connections) {
                if (ch != null && !ch.client.equals(sender)) {
                    ch.sendMessage(message);
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                inp.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (Exception e) {
                System.err.println("unable to close due to " + e);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}