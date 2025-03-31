package main.java.server;
import main.java.model.Grid;
import main.java.model.Player;
import main.java.model.Square;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private static final int PORT = 12345;
    private static final Grid grid = new Grid(10);
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
        playerCounter.decrementAndGet(); // Decrement counter when a player disconnects
        ClientHandler.broadcastLobbyState();
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

                // Assign unique player ID and color
                String playerId = "P" + playerCounter.getAndIncrement();
                int startX, startY;
                switch (playerId) {
                    case "P1":
                        startX = 0;
                        startY = 0;
                        break;
                    case "P2":
                        startX = grid.getSize() - 1;
                        startY = 0;
                        break;
                    case "P3":
                        startX = 0;
                        startY = grid.getSize() - 1;
                        break;
                    case "P4":
                        startX = grid.getSize() - 1;
                        startY = grid.getSize() - 1;
                        break;
                    default:
                        // Additional players spawn near the center
                        startX = (grid.getSize() - 1) / 2;
                        startY = (grid.getSize() - 1) / 2;
                }
                String playerColor = switch (playerId) {
                    case "P1" -> "RED";
                    case "P2" -> "GREEN";
                    case "P3" -> "BLUE";
                    default -> "YELLOW";
                };

                player = new Player(playerId, startX, startY, playerColor);

                synchronized (players) {
                    players.put(playerId, player);
                    broadcastLobbyState();
                }

                grid.getSquare(startX, startY).tryLock(player);
                broadcast("PLAYER_JOINED " + playerId + " 0 0");
                broadcastLobbyState();

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

        // Broadcast lobby's players and their readiness (so client's UI can update accordingly)
        private static void broadcastLobbyState() {
            synchronized (players) {
                String lobbyState = ("LOBBY_STATE: ");
                for (Player player : players.values()) {
                    lobbyState += player.getId() + "," + (player.getReady() ? "READY" : "NOT_READY") + ";";
                }
                broadcast(lobbyState);
            }
        }

        // Check if all players are ready
        private static boolean allPlayersReady() {
            synchronized (players) {
                for (Player player : players.values()) {
                    if (!player.getReady()) {
                        return false;
                    }
                }
            }
            return true;
        }

        // If all is ready, start countdown (can be cancelled if someone unready)
        private static void startGameCountdown() {
            broadcast("GAME_STARTING");
    
            for (int i = 3; i > 0; i--) {
                synchronized (players) {
                    if (!allPlayersReady()) {
                        broadcast("COUNTDOWN_ABORTED");
                        broadcastLobbyState();
                        return;
                    }
                }
    
                broadcast("COUNTDOWN" + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            broadcast("GAME_STARTED"); // Notify clients to transition to the game
            // todo: Logic for transitioning into the actual game
        }

        // Handle messages from the clients
        private void handleClientMessage(String message) {
            String[] parts = message.split(" ");
            switch (parts[0]) {
                case "MOVE":
                    int newX = Integer.parseInt(parts[1]);
                    int newY = Integer.parseInt(parts[2]);

                    Square square = grid.getSquare(newX, newY);

                    if(square.tryLock(player)) {
                        player.setX(newX);
                        player.setY(newY);
                    }

                    if (player.move(newX, newY, grid)) {
                        broadcast("PLAYER_MOVED " + player.getId() + " " + newX + " " + newY);
                    } else {
                        sendMessage("INVALID MOVE");
                    }
                    break;
                    
                case "READY":
                    player.toggleReady();
                    broadcastLobbyState();

                    if (allPlayersReady()) {
                        startGameCountdown();
                    }
                    break;

                case "UNREADY":
                    player.toggleReady();
                    broadcastLobbyState();
                    break;

                default:
                    sendMessage("UNKNOWN COMMAND");
                    break;
            }
        }

        public static void determineWinner() {
            Map<Player, Integer> scoreMap = new HashMap<>();

            // count squares owned by each player
            for(int i = 0; i < grid.getSize(); i++) {
                for(int j = 0; j < grid.getSize(); j++) {
                    Player owner = grid.getSquare(i, j).getOwner();
                    if(owner != null) {
                        scoreMap.put(owner, scoreMap.getOrDefault(owner, 0) + 1);
                    }
                }
            }

            // find player with the most squares
            Player winner = null;
            int maxScore = 0;
            for (Map.Entry<Player, Integer> entry : scoreMap.entrySet()) {
                if(entry.getValue() > maxScore) {
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

        private void cleanup() {
            try {
                if (player != null) {
                    grid.getSquare(player.getX(), player.getY()).unlock();
                    synchronized (players) {
                        players.remove(player.getId());
                        broadcastLobbyState();
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
