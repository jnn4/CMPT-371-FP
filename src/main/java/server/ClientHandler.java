package main.java.server;

import main.java.model.Player;

import java.io.*;
import java.net.Socket;

/**
 * Handles communication with an individual game client.
 * This class is responsible for assigning players, processing client commands,
 * sending and receiving messages, and broadcasting game state updates.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private Player player;

    /**
     * Constructor for creating a new ClientHandler.
     *
     * @param socket The socket connection to the client.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Sends a message to the client.
     *
     * @param message The message to be sent to the client.
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * The main execution method for the ClientHandler thread.
     * Listens for incoming messages from the client, processes them, and responds accordingly.
     * Handles client connection, player assignment, game state updates, and message processing.
     */
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Check if the server has space for more players
            if (GameServer.getPlayerCount() > GameServer.getMaxPlayers()) {
                sendMessage("SERVER_FULL");
                socket.close();
                return;
            }

            // Assign a player ID and position, then initialize the player
            String playerId = "P" + GameServer.getNextPlayerId();
            int[] startPos = getCornerPosition(playerId);
            String playerColor = getCornerColor(playerId);
            this.player = new Player(playerId, startPos[0], startPos[1], playerColor);

            // Add player to the game and lock the initial position
            GameServer.addPlayer(player);
            GameServer.getGrid().getSquare(startPos[0], startPos[1]).tryLock(player);

            // Send player assignment and broadcast player join
            sendMessage("ASSIGN_PLAYER," + playerId + "," + startPos[0] + "," + startPos[1] + "," + playerColor);
            GameServer.broadcast("PLAYER_JOINED," + playerId + "," + startPos[0] + "," + startPos[1] + "," + playerColor);

            // Continuously read and process messages from the client
            String message;
            while ((message = in.readLine()) != null) {
                handleClientMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + (player != null ? player.getId() : "unknown"));
        } finally {
            cleanup();
        }
    }

    /**
     * Determines the starting position for a player based on their ID.
     * The positions correspond to the four corners of the grid.
     *
     * @param playerId The ID of the player (e.g., P1, P2).
     * @return An array of two integers, [x, y], representing the starting position.
     */
    private int[] getCornerPosition(String playerId) {
        int maxPos = GameServer.getGrid().getSize() - 1;
        return switch (playerId) {
            case "P1" -> new int[]{0, 0};
            case "P2" -> new int[]{maxPos, 0};
            case "P3" -> new int[]{0, maxPos};
            case "P4" -> new int[]{maxPos, maxPos};
            default -> throw new IllegalStateException("Unexpected player ID: " + playerId);
        };
    }

    /**
     * Determines the color for a player based on their ID.
     *
     * @param playerId The ID of the player (e.g., P1, P2).
     * @return A string representing the player's color in hexadecimal format.
     */
    private String getCornerColor(String playerId) {
        return switch (playerId) {
            case "P1" -> "#FF0000";
            case "P2" -> "#00FF00";
            case "P3" -> "#0000FF";
            case "P4" -> "#FFFF00";
            default -> throw new IllegalStateException("Unexpected player ID: " + playerId);
        };
    }

    /**
     * Handles messages sent by the client and takes appropriate actions.
     *
     * @param message The message received from the client.
     */
    private void handleClientMessage(String message) {
        String[] parts = message.split(" ");
        switch (parts[0]) {
            case "MOVE":
                int newX = Integer.parseInt(parts[1]);
                int newY = Integer.parseInt(parts[2]);
                if (GameServer.movePlayer(player.getId(), newX, newY)) {
                    GameServer.broadcast("PLAYER_MOVED," + player.getId() + "," + newX + "," + newY + "," + player.getColor());
                } else {
                    sendMessage("INVALID MOVE");
                }
                break;

            case "INIT_STATE":
                broadcastLobbyState();
                break;

            case "READY":
                player.toggleReady();
                broadcastLobbyState();
                if (allPlayersReady()) startGameCountdown();
                break;

            case "UNREADY":
                player.toggleReady();
                broadcastLobbyState();
                break;

            default:
                sendMessage("UNKNOWN COMMAND");
        }
    }

    /**
     * Broadcasts the current lobby state to all clients, including each player's readiness status.
     */
    static void broadcastLobbyState() {
        StringBuilder lobbyState = new StringBuilder("LOBBY_STATE,");
        for (Player player : GameServer.getPlayers().values()) {
            lobbyState.append(player.getId())
                    .append(",")
                    .append(player.getReady() ? "READY" : "NOT_READY")
                    .append(";");
        }
        GameServer.broadcast(lobbyState.toString());
    }

    /**
     * Checks if all players in the game are marked as "ready".
     *
     * @return true if all players are ready, false otherwise.
     */
    private static boolean allPlayersReady() {
        for (Player player : GameServer.getPlayers().values()) {
            if (!player.getReady()) return false;
        }
        return true;
    }

    /**
     * Starts the game countdown, notifying all players every second, and begins the game when all players are ready.
     */
    private static void startGameCountdown() {
        for (int i = 3; i > 0; i--) {
            if (!allPlayersReady()) {
                GameServer.broadcast("COUNTDOWN_ABORTED");
                broadcastLobbyState();
                return;
            }
            GameServer.broadcast("COUNTDOWN," + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        GameServer.broadcast("GAME_STARTED");
    }

    /**
     * Cleans up resources when the client disconnects, including releasing locks, updating game state,
     * broadcasting the player's departure, and closing the socket connection.
     */
    private void cleanup() {
        try {
            if (player != null) {
                GameServer.getGrid().getSquare(player.getX(), player.getY()).releaseLock();
                GameServer.removePlayer(player.getId());
                broadcastLobbyState();
                GameServer.broadcast("PLAYER_LEFT," + player.getId());
            }

            GameServer.removeClient(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}