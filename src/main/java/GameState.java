package main.backend.springboot.src.main.java.CMPT371Project.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;

public class GameState {
    private final Map<String, Player> players; // Maps player IDs to player objects
    private final List<List<Square>> board; // Game board (2D list)
    private final Lock[][] locks; // Lock array for concurrency control

    public GameState(int rows, int cols) {
        players = new HashMap<>();
        board = new ArrayList<>();
        locks = new Lock[rows][cols];
        // Initialize board and locks
        for (int i = 0; i < rows; i++) {
            board.add(new ArrayList<>());
            for (int j = 0; j < cols; j++) {
                board.get(i).add(new Square(i, j)); // Initialize squares
                locks[i][j] = new ReentrantLock(); // Create a lock for each square
            }
        }
    }

    // Methods to get and modify player positions, squares, etc.
    public void movePlayer(String playerId, int row, int col) {
        // Game logic to move the player
    }

    public boolean tryLockSquare(int row, int col) {
        return locks[row][col].tryLock();
    }

    public void unlockSquare(int row, int col) {
        locks[row][col].unlock();
    }

    public void updatePlayerPosition(String playerId, int row, int col) {
        // Update player position logic
    }
}
