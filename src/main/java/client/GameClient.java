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

                // set player ID when server assigns it
                if (message.startsWith("ASSIGN_PLAYER,")) {
                    playerId = message.split(",")[1];
                    System.out.println("Player: " + playerId);
                }

                // Lobby state update (send post-fix of string only)
                else if (message.startsWith("LOBBY_STATE: ")) {
                    if (gui != null) {
                        gui.updateLobby(message.substring("LOBBY_STATE: ".length()));
                    }
                }

                // Countdown update
                else if (message.startsWith("COUNTDOWN ")) {
                    int seconds = Integer.parseInt(message.split(" ")[1]);
                    if (gui != null) {
                        gui.updateCountdown(seconds);
                    }
                }

                // Countdown abort
                else if (message.equals("COUNTDOWN_ABORTED")) {
                    if (gui != null) {
                        gui.abortCountdown();
                    }
                }

                // Game start
                else if (message.equals("GAME_STARTED")) {
                    if (gui != null) {
                        gui.startGame();
                    }
                }

                // update GUI when player moves
                if (gui != null) {
                    gui.updateMaze(message);
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

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
