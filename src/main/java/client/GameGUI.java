package main.java.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameGUI extends JFrame {
    private static final int GRID_SIZE = 10;
    private JLabel[][] grid;
    private int playerX = 0, playerY = 0; // Initial position
    private GameClient client;

    public GameGUI(GameClient client) {
        this.client = client;
        setTitle("Multiplayer Maze Game");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        grid = new JLabel[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                grid[i][j] = new JLabel(" ", SwingConstants.CENTER);
                grid[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                add(grid[i][j]);
            }
        }

        updatePlayerPosition();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int newX = playerX, newY = playerY;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: newY--; break;    // Move up (decrease Y)
                    case KeyEvent.VK_DOWN: newY++; break;  // Move down (increase Y)
                    case KeyEvent.VK_LEFT: newX--; break;  // Move left (decrease X)
                    case KeyEvent.VK_RIGHT: newX++; break; // Move right (increase X)
                }

                if (isValidMove(newX, newY)) {
                    playerX = newX;
                    playerY = newY;
                    client.sendMove(playerX, playerY);
                    updatePlayerPosition();
                }
            }
        });

        setFocusable(true);
        setVisible(true);
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    public void updateMaze(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(",");
            String command = parts[0];

            if(command.equals("PLAYER_MOVED")) {
                String playerId = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);

                grid[x][y].setBackground(Color.RED);
            }
        });
    }

    private void updatePlayerPosition() {
        grid[playerX][playerY].setText("P");
        grid[playerX][playerY].setBackground(Color.GREEN);
    }
}
