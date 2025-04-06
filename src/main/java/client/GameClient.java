package main.java.client;

import java.io.*;
import java.net.*;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GameGUI gui; // Reference to update the UI
    private String playerId;

    public GameClient(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a listener thread for server messages
            new Thread(this::listenForMessages).start();

        } catch (ConnectException e) {
            System.err.println("ERROR: Unable to connect to the server at " + serverAddress + ":" + port);
            System.err.println("Please make sure the server is running and try again.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: I/O exception occurred while connecting to the server.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        GameClient client = new GameClient("localhost", 12345);
        GameGUI gui = new GameGUI(client);
        client.setGUI(gui);
        client.sendMessage("INIT_STATE");
    }


    public void setGUI(GameGUI gui) {
        this.gui = gui;  // Connect GUI with Client
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendMove(int newX, int newY) {
        if (playerId != null) {
            out.println("MOVE," + playerId + "," + newX + "," + newY);
        }
    }

    private void listenForMessages() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println("Server: " + message);

                String[] parts = message.split(",");
                String command = parts[0];

                switch (command) {
                    case "ASSIGN_PLAYER":
                        playerId = parts[1];
                        int startX = Integer.parseInt(parts[2]);
                        int startY = Integer.parseInt(parts[3]);
                        int endX = Integer.parseInt(parts[4]);
                        String playerColor = parts[5];
                        // create local player at assigned position
                        if(gui != null) {
                            gui.setLocalPlayer(playerId, startX, startY, playerColor);
                        }
                        System.out.println("Player: " + playerId + " at position (" + startX + ", " + startY + ")");
                        break;
                    case "LOBBY_STATE":
                        if (gui != null) {
                            gui.updateLobby(message.substring("LOBBY_STATE,".length()));
                        }
                        break;
                    case "COUNTDOWN":
                        try {
                            int seconds = Integer.parseInt(parts[1]);
                            if (gui != null) {
                                gui.updateCountdown(seconds);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid countdown value received from server.");
                        }
                        break;
                    case "COUNTDOWN_ABORTED":
                        if (gui != null) {
                            gui.abortCountdown();
                        }
                        break;
                    case "GAME_STARTED":
                        if (gui != null) {
                            gui.startGame();
                        }
                        break;
                    case "PLAYER_JOINED":
                        if (gui != null) {
                            gui.addPlayer(message.substring("PLAYER_JOINED,".length()));
                        }
                        break;
                    case "PLAYER_MOVED":
                        String[] playerParts = message.substring("PLAYER_MOVED,".length()).split(",");
                        int newX = Integer.parseInt(playerParts[0]);
                        int newY = Integer.parseInt(playerParts[1]);
                        String playerId = parts[2];
                        gui.movePlayer(playerId, newX, newY);
                        break;
                    case "PLAYER_LEFT":
                        if (gui != null) {
                            gui.removePlayer(message.substring("PLAYER_LEFT,".length()));
                        }
                        break;
                    case "GRID_STATE":
                        String gridStateData = message.substring("GRID_STATE".length());
                        gui.initializeBoard(gridStateData);
                        break;
                    case "SQUARE_CLAIMED":
                        String[] squareParts = message.substring("SQUARE_CLAIMED,".length()).split(",");
                        if (squareParts.length >= 3) {
                            int x = Integer.parseInt(squareParts[0]);
                            int y = Integer.parseInt(squareParts[1]);
                            String playerid = squareParts[2];
                            if (gui != null) {
                                gui.updateSquareOwnership(x, y, playerid);
                            }
                        }
                        break;
                    default:
                        System.err.println("Unknown command from server: " + command);
                        break;
                }
            }
        } catch (SocketException e) {
            System.err.println("ERROR: Connection to the server was lost.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: I/O exception occurred while reading from the server.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // NOT USED ANYWHERE
    // public void close() {
    //     try {
    //         if (socket != null) socket.close();
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }
}
