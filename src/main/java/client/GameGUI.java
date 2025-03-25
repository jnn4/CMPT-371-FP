package main.java.client;

import main.java.model.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class GameGUI extends JFrame {
    private static final int GRID_SIZE = 10;
    private JLabel[][] grid;
    private Player localPlayer;
    private final GameClient client;
    private final Map<String, Player> players = new HashMap<>();
    private final Map<String, Color> trailColors = new HashMap<>();

    public GameGUI(GameClient client) {
        this.client = client;
        setTitle("Multiplayer Maze Game");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        grid = new JLabel[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                grid[row][col] = new JLabel(" ", SwingConstants.CENTER);
                grid[row][col].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                grid[row][col].setOpaque(true);
                add(grid[row][col]);
            }
        }

        // Create local player
        this.localPlayer = new Player("you", 0, 0, "#00FF00");
        players.put(localPlayer.getId(), localPlayer);
        trailColors.put(localPlayer.getId(), calculateTrailColor(Color.decode(localPlayer.getColor())));

        updatePlayerPosition(localPlayer);

        setupKeyBindings();

        setFocusable(true);
        setVisible(true);
        requestFocusInWindow();
    }

    private void setupKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "moveUp");
        im.put(KeyStroke.getKeyStroke("DOWN"), "moveDown");
        im.put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");

        am.put("moveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptMove(localPlayer.getX(), localPlayer.getY() - 1);
            }
        });
        am.put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptMove(localPlayer.getX(), localPlayer.getY() + 1);
            }
        });
        am.put("moveLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptMove(localPlayer.getX() - 1, localPlayer.getY());
            }
        });
        am.put("moveRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptMove(localPlayer.getX() + 1, localPlayer.getY());
            }
        });
    }

    private void attemptMove(int newX, int newY) {
        if (isValidMove(newX, newY)) {
            int oldX = localPlayer.getX();
            int oldY = localPlayer.getY();
            localPlayer.setX(newX);
            localPlayer.setY(newY);

            updateTrail(oldX, oldY, localPlayer.getId());
            updatePlayerPosition(localPlayer);

            client.sendMove(newX, newY); // Send move to server
        }
    }

    private void updateTrail(int x, int y, String playerId) {
        grid[y][x].setText(""); // <- FIXED
        grid[y][x].setBackground(trailColors.get(playerId)); // <- FIXED
    }

    private void updatePlayerPosition(Player player) {
        int x = player.getX();
        int y = player.getY();
        grid[y][x].setText("P"); // <- FIXED
        grid[y][x].setBackground(Color.decode(player.getColor())); // <- FIXED
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    public void updateMaze(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(",");
            String command = parts[0];

            if (command.equals("PLAYER_MOVED")) {
                String playerId = parts[1];
                int newX = Integer.parseInt(parts[2]);
                int newY = Integer.parseInt(parts[3]);
                String color = parts[4];

                Player p = players.get(playerId);
                if (p == null) {
                    p = new Player(playerId, newX, newY, color);
                    players.put(playerId, p);
                    trailColors.put(playerId, calculateTrailColor(Color.decode(color)));
                } else {
                    int prevX = p.getX();
                    int prevY = p.getY();
                    updateTrail(prevX, prevY, playerId);
                    p.setX(newX);
                    p.setY(newY);
                }

                updatePlayerPosition(p);
            }
        });
    }

    private Color calculateTrailColor(Color base) {
        return new Color(
                Math.max(base.getRed() - 50, 0),
                Math.max(base.getGreen() - 50, 0),
                Math.max(base.getBlue() - 50, 0)
        );
    }

    public GameClient getClient() {
        return client;
    }
}
