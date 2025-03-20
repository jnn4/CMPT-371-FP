package main.java;

import java.io.*;
import java.net.Socket;

public class PlayerHandler implements Runnable{
    private Socket socket;
    private GameServer server;
    private PrintWriter out;

    public PlayerHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
            out = new PrintWriter(socket.getOutputStream(), true);
            sendMessage("Welcome to [our amazing game name]!");

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Player says: " + message);
                server.broadcastMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
