package main.java.server;
import main.java.model.Maze;
import main.java.model.Player;
import main.java.model.Square;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private static final int PORT = 12345;
    private static final Maze maze = new Maze(10);
    private static final Set<ClientHandler> clients = new HashSet<>();
    private static final Map<String, Player> players = new HashMap<>();
    private static final AtomicInteger playerCounter = new AtomicInteger(1); // Ensures unique IDs

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Maze Game Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                synchronized (clients) {
                    clients.add(clientHandler);
                }

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public static synchronized void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
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
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Assign unique player ID
                String playerId = "P" + playerCounter.getAndIncrement();
                player = new Player(playerId, 0, 0, "RED");

                synchronized (players) {
                    players.put(playerId, player);
                }

                maze.getSquare(0, 0).tryLock(player);
                broadcast("PLAYER_JOINED " + playerId + " 0 0");

                String message;
                while ((message = in.readLine()) != null) {
                    handleClientMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + player.getId());
            } finally {
                cleanup();
            }
        }

        private void handleClientMessage(String message) {
            String[] parts = message.split(" ");
            if (parts[0].equals("MOVE")) {
                int newX = Integer.parseInt(parts[1]);
                int newY = Integer.parseInt(parts[2]);

                Square square = maze.getSquare(newX, newY);

                if(square.tryLock(player)) {
                    player.setX(newX);
                    player.setY(newY);
                }

                if (player.move(newX, newY, maze)) {
                    broadcast("PLAYER_MOVED " + player.getId() + " " + newX + " " + newY);
                } else {
                    sendMessage("INVALID MOVE");
                }
            }
        }

        private void cleanup() {
            try {
                if (player != null) {
                    maze.getSquare(player.getX(), player.getY()).unlock();
                    synchronized (players) {
                        players.remove(player.getId());
                    }
                    broadcast("PLAYER_LEFT " + player.getId());
                }

                synchronized (clients) {
                    clients.remove(this);
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
