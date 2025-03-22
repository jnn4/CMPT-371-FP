package main.java.server;
import main.java.model.Maze;
import main.java.model.Player;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private static Maze maze = new Maze(10);
    private static Set<ClientHandler> clients = new HashSet<>();
    private static Map<String, Player> players = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Maze Game Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // **Broadcast messages to all clients**
    public static synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // **Remove disconnected clients**
    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Player player;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // assign unique player ID and start position
                String playerId = "P" + (players.size() + 1);
                player = new Player(playerId, 0, 0, "RED");
                players.put(playerId, player);

                maze.getSquare(0, 0).lock(player);
                broadcast("PLAYER_JOINED " + playerId + " 0 0");

                String message;
                while ((message = in.readLine()) != null) {
                    handleClientMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
            } finally {
                removeClient(this);
                if (player != null) {
                    maze.getSquare(player.getX(), player.getY()).unlock();
                    players.remove(player.getId());
                    broadcast("PLAYER_LEFT " + player.getId());
                }
            }
        }

        private void handleClientMessage(String message) {
            String[] parts = message.split(" ");
            if(parts[0].equals("MOVE")) {
                int newX = Integer.parseInt(parts[1]);
                int newY = Integer.parseInt(parts[2]);

                if (player.move(newX, newY, maze)) {
                    broadcast("PLAYER_MOVED " + newX + " " + newY);
                } else {
                    sendMessage("INVALID MOVE");
                }
            }
        }
    }
}
