package main.java.server;

import main.java.model.Grid;
import main.java.model.Player;

import java.util.Map;

/**
 * Defines the core game server operations for a multiplayer maze game.
 * This interface represents the contract between the game server and its clients,
 * including player management, game state updates, and observer pattern implementation.
 *
 * The interface combines server administration methods with observer pattern
 * support for push-based notifications.
 */
public interface GameServerInterface {
    // Observer methods
    void addObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers(String message);
    
    // Player Management and Communication
    void broadcast(String message);
    void addPlayer(Player player);
    void removePlayer(String playerId);
    boolean movePlayer(String playerId, int newX, int newY);
    void removeClient(ClientHandler client);
    void determineWinner();

    // Game State
    int getPlayerCount();
    int getMaxPlayers();
    int getNextPlayerId();
    Grid getGrid();
    Map<String, Player> getPlayers();   
    void startGameTimer();
    void checkAllSquaresClaimed();
}
