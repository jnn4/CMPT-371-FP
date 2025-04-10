package main.java.server;

import main.java.model.Player;

import java.io.*;
import java.net.Socket;

/**
 * Handles communication with an individual game client.
 * This class is responsible for assigning players, processing client commands,
 * sending and receiving messages, and broadcasting game state updates.
 *
 * The ClientHandler also implements the Observer interface to receive updates from the GameServer.
 * This decouples the ClientHandler's responsibilities from the GameServer's core logic,
 * allowing for more flexible and maintainable code. The ClientHandler listens for changes in the game
 * state and reacts accordingly by sending updates to the client.
 */
public class ClientHandler implements Runnable, Observer {
    private final GameServerInterface gameServer;
    private final Socket socket;
    private PrintWriter out;
    private Player player;

    /**
     * This method is called when the observed GameServer sends an update.
     * It handles the game server's broadcast messages and sends them to the client.
     *
     * The ClientHandler listens for updates from the GameServer and ensures that the client is kept in sync
     * with the game state (e.g., game status, player actions, etc.).
     *
     * @param message The message received from the game server to be sent to the client.
     */
    @Override
    public void update(String message) {
        sendMessage(message);
    }

    /**
     * Constructor for creating a new ClientHandler.
     *
     * This constructor initializes the ClientHandler with a connection to the game server and the client socket.
     * It also registers the ClientHandler as an observer of the game server to receive updates regarding
     * game state changes (e.g., player movements, game status updates, etc.).
     *
     * @param gameServer The game server instance that the ClientHandler will observe for game state changes.
     * @param socket The socket connection to the client.
     */
    public ClientHandler(GameServerInterface gameServer, Socket socket) {
        this.gameServer = gameServer;
        this.socket = socket;
        gameServer.addObserver(this);  // Register itself as an observer
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
            if (gameServer.getPlayerCount() > gameServer.getMaxPlayers()) {
                sendMessage("SERVER_FULL");
                socket.close();
                return;
            }

            // Assign a player ID and position, then initialize the player
            String playerId = "P" + gameServer.getNextPlayerId();
            int[] startPos = getCornerPosition(playerId);
            String playerColor = getCornerColor(playerId);
            this.player = new Player(playerId, startPos[0], startPos[1], playerColor);

            // Add player to the game and lock the initial position
            gameServer.addPlayer(player);
            gameServer.getGrid().getSquare(startPos[0], startPos[1]).tryLock(player);

            // Send player assignment and broadcast player join
            sendMessage("ASSIGN_PLAYER," + playerId + "," + startPos[0] + "," + startPos[1] + "," + playerColor);
            gameServer.broadcast("PLAYER_JOINED," + playerId + "," + startPos[0] + "," + startPos[1] + "," + playerColor);

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
        int maxPos = gameServer.getGrid().getSize() - 1;
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
        String[] parts = message.split(",");
        switch (parts[0]) {
            case "MOVE":
                int newX = Integer.parseInt(parts[1]);
                int newY = Integer.parseInt(parts[2]);
                if (gameServer.movePlayer(player.getId(), newX, newY)) {
                    gameServer.broadcast("PLAYER_MOVED," + player.getId() + "," + newX + "," + newY + "," + player.getColor());
                    sendMessage("MOVE_CONFIRMED," + player.getId() + "," + newX + "," + newY);
                } else {
                    sendMessage("INVALID_MOVE");
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
                sendMessage("UNKNOWN_COMMAND");
        }
    }

    /**
     * Broadcasts the current lobby state to all clients, including each player's readiness status.
     */
    void broadcastLobbyState() {
        StringBuilder lobbyState = new StringBuilder("LOBBY_STATE,");
        for (Player player : gameServer.getPlayers().values()) {
            lobbyState.append(player.getId())
                    .append(",")
                    .append(player.getReady() ? "READY" : "NOT_READY")
                    .append(";");
        }
        gameServer.broadcast(lobbyState.toString());
    }

    /**
     * Checks if all players in the game are marked as "ready".
     *
     * @return true if all players are ready, false otherwise.
     */
    private boolean allPlayersReady() {
        for (Player player : gameServer.getPlayers().values()) {
            if (!player.getReady()) return false;
        }
        return true;
    }

    /**
     * Starts the game countdown, notifying all players every second, and begins the game when all players are ready.
     */
    private void startGameCountdown() {
        for (int i = 3; i > 0; i--) {
            if (!allPlayersReady()) {
                gameServer.broadcast("COUNTDOWN_ABORTED");
                broadcastLobbyState();
                return;
            }
            gameServer.broadcast("COUNTDOWN," + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        gameServer.broadcast("GAME_STARTED");
    }

    /**
     * Cleans up resources when the client disconnects, including releasing locks, updating game state,
     * broadcasting the player's departure, and closing the socket connection.
     *
     * This method also unregisters the ClientHandler as an observer of the GameServer to stop receiving
     * updates once the client disconnects.
     */
    private void cleanup() {
        try {
            if (player != null) {
                gameServer.removeObserver(this);
                gameServer.getGrid().getSquare(player.getX(), player.getY()).releaseLock();
                gameServer.removePlayer(player.getId());
                broadcastLobbyState();
                gameServer.broadcast("PLAYER_LEFT," + player.getId());
            }

            gameServer.removeClient(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerId() {
        return player.getId();
    }
}
