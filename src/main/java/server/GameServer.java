package main.java.server;
import main.java.model.Grid;
import main.java.model.Player;
import main.java.model.Square;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The GameServer class manages the multiplayer game server.
 * It handles client connections, broadcasts game messages, player movements, and determines the winner based on player activity in the game grid.
 * The server listens for incoming connections and handles multiple players concurrently.
 */
public class GameServer {
    private static final int PORT = 12345;
    private static final Grid grid = new Grid(10);
    private static final Set<ClientHandler> clients = new HashSet<>();
    private static final Map<String, Player> players = new HashMap<>();
    private static final AtomicInteger playerCounter = new AtomicInteger(1);
    private static final int MAX_PLAYERS = 4;

    /**
     * The entry point for the server.
     * Starts the server and listens for incoming client connections.
     * Once a client is connected, a new ClientHandler thread is created for communication.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Maze Game Server started on port " + PORT);

            // Continuously accept new client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                synchronized (clients) {
                    clients.add(clientHandler);
                }

                new Thread(clientHandler).start();
            }
        } catch (BindException e) {
            System.err.println("Error: Port " + PORT + " is already in use. Please use a different port.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: An unexpected I/O error occurred.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message the message to be sent to all clients.
     */
    public static synchronized void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Removes a client from the list of connected clients.
     *
     * @param client the client to be removed.
     */
    public static synchronized void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        playerCounter.decrementAndGet(); // Decrement counter when a player disconnects
        ClientHandler.broadcastLobbyState();
    }

    // Game logic methods

    /**
     * Moves a player to a new position on the grid.
     *
     * @param playerId the ID of the player.
     * @param newX the new X-coordinate on the grid.
     * @param newY the new Y-coordinate on the grid.
     * @return true if the move is successful, false otherwise.
     */
    public static synchronized boolean movePlayer(String playerId, int newX, int newY) {
        Player player = players.get(playerId);
        if (player == null) {
            return false;
        }

        Square square = grid.getSquare(newX, newY);

        if (square.tryLock(player)) {
            player.setX(newX);
            player.setY(newY);
        }

        return player.move(newX, newY, grid);
    }

    /**
     * Determines the winner of the game by counting the squares owned by each player.
     * The player with the most squares is declared the winner.
     */
    public static synchronized void determineWinner() {
        Map<Player, Integer> scoreMap = new HashMap<>();

        // Count squares owned by each player
        for (int i = 0; i < grid.getSize(); i++) {
            for (int j = 0; j < grid.getSize(); j++) {
                Player owner = grid.getSquare(i, j).getOwner();
                if (owner != null) {
                    scoreMap.put(owner, scoreMap.getOrDefault(owner, 0) + 1);
                }
            }
        }

        // Find player with the most squares
        Player winner = null;
        int maxScore = 0;
        for (Map.Entry<Player, Integer> entry : scoreMap.entrySet()) {
            if (entry.getValue() > maxScore) {
                winner = entry.getKey();
                maxScore = entry.getValue();
            }
        }

        if (winner != null) {
            String message = "Winner: " + winner.getId() + " with " + maxScore + " squares!";
            System.out.println(message);
            broadcast("GAME_OVER," + winner.getId() + "," + maxScore);
        }
    }

    /**
     * Adds a new player to the game.
     *
     * @param player the player to be added.
     */
    public static synchronized void addPlayer(Player player) {
        players.put(player.getId(), player);
        grid.getSquare(player.getX(), player.getY()).tryLock(player);
    }

    /**
     * Removes a player from the game.
     *
     * @param playerId the ID of the player to be removed.
     */
    public static synchronized void removePlayer(String playerId) {
        Player player = players.remove(playerId);
        if (player != null) {
            grid.getSquare(player.getX(), player.getY()).releaseLock();
        }
    }

    public static Grid getGrid() {return grid;}
    public static int getPlayerCount() {return players.size();}
    public static int getMaxPlayers() {return MAX_PLAYERS;}
    public static int getNextPlayerId() {return playerCounter.getAndIncrement();}
    public static Map<String, Player> getPlayers() {return players;}
}
