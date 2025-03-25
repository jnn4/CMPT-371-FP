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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        GameClient client = new GameClient("localhost", 12345);
        GameGUI gui = new GameGUI(client);
        client.setGUI(gui);
    }


    public void setGUI(GameGUI gui) {
        this.gui = gui;  // Connect GUI with Client
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
                if(message.startsWith("ASSIGN_PLAYER,")) {
                    playerId = message.split(",")[1];
                    System.out.println("Player: " + playerId);
                }

                // update GUI when player moves
                if(gui != null) {
                    gui.updateMaze(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
