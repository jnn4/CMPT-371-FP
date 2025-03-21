package main.server;

import java.io.*;
import java.net.*;

public class PlayerHandler implements Runnable {  // Implements Runnable to allow PlayerHandler to be executed on a separate thread
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public PlayerHandler(Socket socket) {
        this.socket = socket;
        try {
            // Initialize the output stream to send data to the client
            // Create a PrintWriter with auto-flush enabled
            out = new PrintWriter(socket.getOutputStream(), true);
            // Initialize the input stream to receive data from the client
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    // The run method is executed when this thread starts
    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from player: " + inputLine);

                // Handle different requests asynchronously by creating a new thread
                String finalInputLine = inputLine;
                new Thread(() -> handleRequest(finalInputLine)).start();  // Start a new thread to handle the player's request
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to handle various requests from the player
    private void handleRequest(String inputLine) {
        if (inputLine.equals("JOIN")) {
            String playerId = "Player" + socket.getPort();  // Generate a player ID based on the socket's port
            GameServer.addPlayer(playerId, this);
            out.println("Welcome, " + playerId + "!");
            GameServer.broadcastMessage(playerId + " has joined the game.");  // Broadcast a message to all players that the new player has joined
        } else if (inputLine.equals("GET_GAME_STATE")) {
            String gameState = GameServer.getGameState();
            out.println(gameState);  // Send the game state to the player
        }
    }

}
