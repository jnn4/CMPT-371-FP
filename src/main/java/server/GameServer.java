package main.java.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 3;
    private List<PlayerHandler> players = new ArrayList<>();

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public void StartServer() {
        System.out.println("Game server started on PORT: " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while(players.size() < MAX_PLAYERS) {
                Socket playerSocket = serverSocket.accept();
                System.out.println("Player connected: " + playerSocket.getInetAddress());
                if (players.size() >= MAX_PLAYERS) {
                    System.out.println("Requested game is full! Closing connection...");
                    playerSocket.close();
                } else {
                    PlayerHandler playerHandler = new PlayerHandler(playerSocket, this);
                    players.add(playerHandler);
                    new Thread(playerHandler).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcastMessage(String message) {
        for (PlayerHandler player : players) {
            player.sendMessage(message);
        }
    }
}