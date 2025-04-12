package main.java.client;

import javax.swing.*;
import java.io.*;
import java.net.*;

/**
 * The GameClient class handles the network communication between the client and server
 * for the multiplayer maze game. It manages connection establishment, message sending,
 * and server message processing.
 */
public class GameClient {
    // Network components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // GUI reference and player state
    private GameGUI gui;
    private String playerId;

    /**
     * Sets the GUI reference and marks it as ready for updates.
     * @param gui The GameGUI instance to associate with this client
     */
    public void setGUI(GameGUI gui) {
        this.gui = gui;
        System.out.println("GUI reference set");
    }

    /**
     * Main entry point for the client application.
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Initialize GUI first with null client (temporarily)
        GameGUI gui = new GameGUI(null);

        // Create client instance with GUI reference
        GameClient client = new GameClient("localhost", 12345, gui);

        // Complete bidirectional connection
        gui.setClient(client);
        client.setGUI(gui);

        System.out.println("Initialization complete");
    }

    /**
     * Constructs a new GameClient and establishes server connection.
     * @param serverAddress The server hostname/IP address
     * @param port The server port number
     * @param gui The associated GameGUI instance
     */
    public GameClient(String serverAddress, int port, GameGUI gui) {
        this.gui = gui;
        if (this.gui != null) {
        }

        try {
            // Establish network connection
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start message listener thread
            new Thread(this::listenForMessages).start();

        } catch (ConnectException e) {
            System.err.println("ERROR: Unable to connect to the server at " +
                    serverAddress + ":" + port);
            System.err.println("Please make sure the server is running and try again.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: I/O exception occurred while connecting to the server.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Sends a generic message to the server.
     * @param message The message to send
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Sends a move command to the server.
     * @param newX The target X coordinate
     * @param newY The target Y coordinate
     */
    public void sendMove(int newX, int newY) {
        out.println("MOVE," + newX + "," + newY);
    }

    /**
     * Listens for incoming messages from the server and processes them.
     * Runs in a separate thread to avoid blocking the main application.
     */
    private void listenForMessages() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println("Server: " + message);
                processServerMessage(message);
            }
        } catch (SocketException e) {
            handleDisconnection("Connection to the server was lost.");
        } catch (IOException e) {
            handleDisconnection("I/O exception occurred while reading from the server.");
        }
    }

    /**
     * Processes a single message received from the server.
     * @param message The raw message from the server
     */
    private void processServerMessage(String message) {
        String[] parts = message.split(",");
        String command = parts[0];

        // Handle different server commands
        switch (command) {
            case "ASSIGN_PLAYER":
                handlePlayerAssignment(parts);
                break;

            case "LOBBY_STATE":
                if (gui != null) {
                    gui.updateLobby(message.substring("LOBBY_STATE,".length()));
                }
                break;

            case "COUNTDOWN":
                handleCountdown(parts);
                break;

            case "COUNTDOWN_ABORTED":
                if (gui != null) {
                    gui.abortCountdown();
                }
                break;

            case "GAME_STARTED":
                SwingUtilities.invokeLater(() -> {
                    if (gui != null && playerId == null) {
                        System.err.println("Game started but player ID not assigned!");
                        return;
                    }
                    gui.startGame();
                });
                break;

            case "PLAYER_JOINED":
                handlePlayerJoined(message, parts);
                break;

            case "PLAYER_MOVED":
                if (gui != null) {
                    gui.updateMaze(message.substring("PLAYER_MOVED,".length()));
                }
                break;

            case "PLAYER_LEFT":
                if (gui != null) {
                    gui.removePlayer(message.substring("PLAYER_LEFT,".length()));
                }
                break;

            case "MOVE_CONFIRMED":
                if (gui != null && parts.length >= 4) {
                    String confirmedPlayerId = parts[1];
                    int newX = Integer.parseInt(parts[2]);
                    int newY = Integer.parseInt(parts[3]);
                    gui.onMoveConfirmed(confirmedPlayerId, newX, newY);
                }
                break;

            case "INVALID_MOVE":
                System.err.println("Invalid move detected");
                break;
    
            case "UNKNOWN_COMMAND":
                System.err.println("Server rejected last command");
                break;
            
            case "SERVER_FULL":
                System.err.println("ERROR: Server is full. Please try again later.");
                break;

            case "GAME_OVER":
                if (parts.length >= 3) {
                    String winnerID = parts[1];
                    int winnerScore = Integer.parseInt(parts[2]);
                    String allScores = parts[3];
                    
                    if (gui != null) {
                        gui.showGameOver(winnerID, winnerScore, allScores);
                    }
                }
                break;

            default:
                System.err.println("Unknown command from server: " + command);
                break;
        }
    }

    /**
     * Handles player assignment message from server.
     * @param parts The parsed message parts
     */
    private void handlePlayerAssignment(String[] parts) {
        playerId = parts[1];
        int x = Integer.parseInt(parts[2]);
        int y = Integer.parseInt(parts[3]);
        String color = parts[4];

        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.setLocalPlayer(playerId, x, y, color);
            }
        });
    }

    /**
     * Handles countdown messages from server.
     * @param parts The parsed message parts
     */
    private void handleCountdown(String[] parts) {
        try {
            int seconds = Integer.parseInt(parts[1]);
            if (gui != null) {
                gui.updateCountdown(seconds);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid countdown value received from server.");
        }
    }

    /**
     * Handles player join notifications from server.
     * @param message The raw message
     * @param parts The parsed message parts
     */
    private void handlePlayerJoined(String message, String[] parts) {
        // Skip our own join message
        if (!parts[1].equals(playerId)) {
            String finalMessage = message;
            SwingUtilities.invokeLater(() -> {
                if (gui != null) {
                    gui.addPlayer(finalMessage.substring("PLAYER_JOINED,".length()));
                }
            });
        }
    }

    /**
     * Handles server disconnection events.
     * @param errorMessage The disconnection reason
     */
    private void handleDisconnection(String errorMessage) {
        System.err.println("ERROR: " + errorMessage);
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                JOptionPane.showMessageDialog(gui,
                        "Connection to server lost: " + errorMessage,
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        System.exit(1);
    }
}