package main.server;

import java.io.*;
import java.net.*;

public class PlayerHandler implements Runnable{
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;

    public PlayerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            playerId = "Player-" + socket.getPort();
            GameServer.addPlayer(playerId, this);
            sendMessage("Welcome " + playerId);

            String message;
            while((message = in.readLine()) != null) {
                System.out.println(playerId + "moved " + message);
                GameServer.broadcastMessage(playerId + "moved " + message);
            }
        } catch (IOException e) {
            System.out.println("Player " + playerId + " disconnected.");
        } finally {
            GameServer.removePlayer(playerId);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
