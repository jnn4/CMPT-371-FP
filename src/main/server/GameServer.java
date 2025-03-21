package main.server;

import java.io.*;
import java.net.*;  // For Socket, ServerSocket
import java.util.concurrent.ConcurrentHashMap;  // Thread-safe map class
import java.util.HashMap;  // HashMap for storing key-value pairs
import java.util.ArrayList;

public class GameServer {
    private static final int PORT = 12345;
    // A thread-safe map for storing players by their IDs
    private static ConcurrentHashMap<String, PlayerHandler> players = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Game Server Running on port " + PORT);
        // Listen for incoming client connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while(true) {  // Loop to keep accepting client connections
                Socket clientSocket = serverSocket.accept();
                PlayerHandler player = new PlayerHandler(clientSocket);
                new Thread(player).start();  // Start a new thread for each player (PlayerHandler is executed in a separate thread)
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

    public static String getGameState() {
        HashMap<String, Object> gameState = new HashMap<>();

        // Create a list of player names (keys of the players map)
        ArrayList<String> playerNames = new ArrayList<>(players.keySet());  // Convert player IDs to a list of player names
        gameState.put("players", playerNames);

        return "Game State: " + gameState.toString();
    }
}
