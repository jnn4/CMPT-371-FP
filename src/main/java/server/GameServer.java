package main.java.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private static final int PORT = 12345;
    private static ConcurrentHashMap<String, PlayerHandler> players = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Game Server Running" + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while(true) {
                Socket clientSocket = serverSocket.accept();
                PlayerHandler player = new PlayerHandler(clientSocket);
                new Thread(player).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addPlayer(String id, PlayerHandler player) {
        players.put(id, player);
    }

    public static void removePlayer(String id) {
        players.remove(id);
    }

    public static void broadcastMessage(String message) {
        for (PlayerHandler player : players.values()) {
            player.sendMessage(message);
        }
    }
}