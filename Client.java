import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected;
    private String ipAddress;

    public Client() {
        ipAddress = "localhost";
    }

    public Client(String ip) {
        this.ipAddress = ip;
    }

    @Override
    public void run() {
        try {
            client = new Socket(ipAddress, 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            connected = true;

            InputHandler inHandler = new InputHandler();
            Thread thread = new Thread(inHandler);
            thread.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (Exception e) {
            System.err.println("Exception in run client " + e);
            shutdown();
        }
    }

    public void shutdown() {
        connected = false;
        try {
            in.close();
            out.close();
            if (!client.isClosed())
                client.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    class InputHandler implements Runnable {

        @Override
        public void run() {
            try {
                Scanner reader = new Scanner(System.in);
                while (connected) {
                    String message = reader.nextLine();
                    if (message.equals("/quit")) {
                        out.println(message);
                        reader.close();
                        shutdown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (Exception e) {
                System.err.println(e);
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client;
        if (args.length == 0) {
            client = new Client();
        } else {
            client = new Client(args[0]);
        }
        client.run();
    }
}