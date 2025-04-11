package main.java.server;
import main.java.model.Grid;
import main.java.model.Player;
import main.java.model.Square;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// classes for running checks on game's status (timer run out/all squared owned)
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents the game server that manages the game state and client interactions.
 *
 * The GameServer class follows the Observer design pattern, where multiple ClientHandler objects
 * (acting as observers) observe the game server and receive updates when the game state changes.
 * The server manages the grid, player state, and overall game flow. It broadcasts state changes to
 * all registered observers, ensuring that clients remain in sync with the server's actions (e.g.,
 * player movements, game status updates).
 *
 * The GameServer also manages player assignments, ensuring that players are added or removed as necessary,
 * and that the game starts only when all players are ready.
 */

public class GameServer implements GameServerInterface {
    private static final int PORT = 12345;
    private static final Grid grid = new Grid(10);
    private static final int GAME_DURATION_SECONDS = 60;
    private static final Set<ClientHandler> clients = new HashSet<>();
    private static final Map<String, Player> players = new HashMap<>();
    private static final AtomicInteger playerCounter = new AtomicInteger(1);
    private static final int MAX_PLAYERS = 4;

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> task_checkAllSquaresClaimed = null;

    private List<Observer> observers = new ArrayList<>();

    /**
     * Registers a new observer to receive updates from the GameServer.
     *
     * This method adds a new observer (e.g., a ClientHandler) to the list of observers
     * so that it can receive game state updates. Observers are notified when significant
     * changes occur in the game, such as player actions or game status updates.
     *
     * @param observer The observer to be added.
     */
    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    /**
     * Removes an observer from the list of registered observers.
     *
     * This method unregisters an observer (e.g., a ClientHandler) so that it no longer
     * receives updates from the GameServer. This is typically called when a client disconnects.
     *
     * @param observer The observer to be removed.
     */
    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers of a game state update.
     *
     * This method sends a message to all observers to notify them of a change in the game state.
     * Each observer will handle the update accordingly, ensuring that the client remains in sync with
     * the server's current state.
     *
     * @param message The message to be broadcast to all observers.
     */
    @Override
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * This method sends a message to all registered observers, typically used to notify
     * players about events like player movements, game status updates, or other game events.
     *
     * This method is synchronized to ensure thread-safety when broadcasting messages to multiple
     * clients simultaneously.
     *
     * @param message The message to be broadcast to all observers.
     */
    @Override
    public synchronized void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Starts the server and listens for incoming client connections.
     * Once a client is connected, a new ClientHandler thread is created for communication.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Maze Game Server started on port " + PORT);

            // Instantiate GameServer
            GameServer gameServer = new GameServer();

            // Continuously accept new client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Pass the GameServer instance as an observer
                ClientHandler clientHandler = new ClientHandler(gameServer, clientSocket);

                synchronized (clients) {
                    clients.add(clientHandler);
                }

                new Thread(clientHandler).start();
            }
        } catch (BindException e) {
            System.err.println("Error: Port " + PORT + " is already in use. Please use a different port.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: An unexpected I/O error occurred.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Removes a client from the list of connected clients.
     *
     * This method ensures thread-safety when removing a client from the list of connected clients
     * and updates the player counter accordingly. It also notifies all observers about the player's disconnection.
     *
     * @param client the client to be removed.
     */
    @Override
    public synchronized void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        playerCounter.decrementAndGet(); // Decrement counter when a player disconnects

        // Notify all observers about the state change (like player disconnection)
        notifyObservers("Player " + client.getPlayerId() + " has disconnected.");
    }

    /**
     * Moves a player to a new position on the grid.
     *
     * This method attempts to move a player to the specified coordinates on the grid.
     * If the move is successful, the player's position is updated, and the grid square is locked.
     * The game state is updated accordingly, and observers are notified of the change.
     *
     * @param playerId the ID of the player.
     * @param newX the new X-coordinate on the grid.
     * @param newY the new Y-coordinate on the grid.
     * @return true if the move is successful, false otherwise.
     */
    @Override
    public synchronized boolean movePlayer(String playerId, int newX, int newY) {
        Player player = players.get(playerId);
        if (player == null) {
            return false;
        }

        Square square = grid.getSquare(newX, newY);

        if (square.tryLock(player)) {
            player.setX(newX);
            player.setY(newY);
        }

        return player.move(newX, newY, grid);
    }

    /**
     * Determines the winner of the game by counting the squares owned by each player.
     * The player with the most squares is declared the winner.
     *
     * The winner's information is broadcasted to all connected clients once the game ends.
     */
    @Override
    public synchronized void determineWinner() {
        // Cancel the scheduled check if the game has ended
        if (task_checkAllSquaresClaimed != null) {
            task_checkAllSquaresClaimed.cancel(true);
        }

        Map<Player, Integer> scoreMap = new HashMap<>();

        // Count squares owned by each player
        for (int i = 0; i < grid.getSize(); i++) {
            for (int j = 0; j < grid.getSize(); j++) {
                Player owner = grid.getSquare(i, j).getOwner();
                if (owner != null) {
                    scoreMap.put(owner, scoreMap.getOrDefault(owner, 0) + 1);
                }
            }
        }

        // Convert map to list for sorting (descending order)
        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scoreMap.entrySet());
        sortedScores.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        // Winner = first entry in sorted list
        Player winner = null;
        int maxScore = 0;

        if (!sortedScores.isEmpty()) {
            winner = sortedScores.get(0).getKey();
            maxScore = sortedScores.get(0).getValue();
        }

        String scoresData = "";
        for (Map.Entry<Player, Integer> entry : sortedScores) {
            Player player = entry.getKey();
            int score = entry.getValue();
            scoresData += player.getId() + ":" + score + ";";
        }
    
        if (winner != null) {
            String message = "Winner: " + winner.getId() + " with " + maxScore + " squares!";
            System.out.println(message);
            broadcast("GAME_OVER," + winner.getId() + "," + maxScore + "," + scoresData);
        }
    }

    /**
     * Starts the game timer that counts down from GAME_DURATION_SECONDS.
     * When the timer expires, determineWinner() is called to end the game.
     */
    @Override
    public synchronized void startGameTimer() {
        scheduler.schedule(() -> {
            System.out.println("Game time expired! Determining winner...");
            determineWinner();
        }, GAME_DURATION_SECONDS, TimeUnit.SECONDS);

        // Schedule a task to periodically check if all squares are claimed
        task_checkAllSquaresClaimed = scheduler.scheduleAtFixedRate(() -> {
            checkAllSquaresClaimed();
        }, 5, 1, TimeUnit.SECONDS);
    }

    /**
     * Checks if all squares on the grid have been claimed by players.
     * This method iterates through the grid and checks if all squares have an owner.
     *
     * If all squares are claimed, it calls determineWinner() to end the game early.
     */
    @Override
    public void checkAllSquaresClaimed() {
        boolean allClaimed = true;
        for (int i = 0; i < grid.getSize(); i++) {
            for (int j = 0; j < grid.getSize(); j++) {
                Square square = grid.getSquare(i, j);
                if (!square.isWall() && square.getOwner() == null) {
                    allClaimed = false;
                    break;
                }
            }
        }

        if (allClaimed) {
            System.out.println("All squares claimed! Determining winner...");
            determineWinner();
        }
    }

    /**
     * Adds a player to the game and notifies all observers about the new player.
     *
     * This method assigns a player to the game and broadcasts the player's joining
     * status to all connected clients.
     *
     * @param player The player to be added to the game.
     */
    @Override
    public synchronized void addPlayer(Player player) {
        players.put(player.getId(), player);
        grid.getSquare(player.getX(), player.getY()).tryLock(player);
    }

    /**
     * Removes a player from the game and releases the lock on the player's square.
     *
     * This method removes the player from the game, releases the lock on the square
     * that the player occupied, and broadcasts the player's departure to all connected clients.
     *
     * @param playerId The ID of the player to be removed.
     */
    @Override
    public synchronized void removePlayer(String playerId) {
        Player player = players.remove(playerId);
        if (player != null) {
            grid.getSquare(player.getX(), player.getY()).releaseLock();
        }
    }

    @Override
    public int getPlayerCount() {return players.size();}
    @Override
    public int getMaxPlayers() {return MAX_PLAYERS;}
    @Override
    public int getNextPlayerId() {return playerCounter.getAndIncrement();}
    @Override
    public Grid getGrid() {return grid;}
    @Override
    public Map<String, Player> getPlayers() {return players;}
}
