package main.java.client;

import main.java.model.Player;
import main.java.model.Square;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class GameGUI extends JFrame {
    private static final int WINDOW_SIZE = 400;
    private static final int GRID_SIZE = 10;
    // Lobby UI
    private JPanel lobbyPanel;
    private JLabel countdownLabel;
    private DefaultListModel<String> playerListModel;
    private JList<String> playerList;
    private JButton readyButton;

    // Game UI
    private JLabel[][] gridLabels; // For displaying the game grid
    private Square[][] gridSquares; // For game logic (locking squares)
    private Player localPlayer;
    private final GameClient client;
    private final Map<String, Player> players = new HashMap<>();
    private final Map<String, Color> trailColors = new HashMap<>();

    private Color parseColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "BLUE" -> Color.BLUE;
            case "YELLOW" -> Color.YELLOW;
            default -> Color.GRAY;  // Default if color name doesn't match
        };
    }

    // Constructor
    public GameGUI(GameClient client) {
        this.client = client;
        setTitle("Multiplayer Maze Game");
        setSize(WINDOW_SIZE, WINDOW_SIZE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        // Lobby setup
        setupLobbyPanel();

        getContentPane().add(lobbyPanel, BorderLayout.CENTER);
        lobbyPanel.setVisible(true);

        // Game setup
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

    private void setupLobbyPanel() {
        lobbyPanel = new JPanel(new BorderLayout());
        lobbyPanel.setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

        // Countdown label at the top
        countdownLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
        lobbyPanel.add(countdownLabel, BorderLayout.PAGE_START);

        // Player list in the center
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setVisibleRowCount(4);
        playerList.setFixedCellHeight(20);

        JScrollPane playerListScrollPane = new JScrollPane(playerList);
        playerListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        playerListScrollPane.setPreferredSize(new Dimension(200, 150));
        lobbyPanel.add(playerListScrollPane, BorderLayout.CENTER);

        // Ready button at the bottom
        readyButton = new JButton("READY");
        readyButton.addActionListener(_ -> toggleReadyState());
        lobbyPanel.add(readyButton, BorderLayout.PAGE_END);
    }

    // ----- Lobby methods -----
    private void toggleReadyState() {
        if (readyButton.getText().equals("READY")) {
            client.sendMessage("READY");
            readyButton.setText("UNREADY");
            System.out.println("Player is ready");
        } else {
            client.sendMessage("UNREADY");
            readyButton.setText("READY");
        }
    }

    public void updateLobby(String message) {
        SwingUtilities.invokeLater(() -> {
            // Update player list
            playerListModel.clear();
            String[] players = message.split(";");
            for (String playerInfo : players) {
                if (!playerInfo.isEmpty() && playerInfo.contains(",")) {
                    String[] parts = playerInfo.split(",");
                    String playerId = parts[0];
                    String readiness = parts[1];
                    playerListModel.addElement(playerId + " - " + readiness);
                }
            }
            lobbyPanel.revalidate();
            lobbyPanel.repaint();
        });
    }

    public void updateCountdown(int seconds) {
        SwingUtilities.invokeLater (() -> {
            countdownLabel.setText("Game starting in " + seconds);
        });
    }

    public void abortCountdown() {
        SwingUtilities.invokeLater(() -> {
            countdownLabel.setText("Countdown aborted. Waiting for all players to be ready.");
        });
    }

    // ----- Game methods -----
    public void startGame() {
        getContentPane().removeAll();
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));
        gridLabels = new JLabel[GRID_SIZE][GRID_SIZE];
        gridSquares = new Square[GRID_SIZE][GRID_SIZE]; // Initialize game logic grid

        for (int col = 0; col < GRID_SIZE; col++) {
            for (int row = 0; row < GRID_SIZE; row++) {
                // Initialize the JLabel for the grid
                gridLabels[col][row] = new JLabel(" ", SwingConstants.CENTER);
                gridLabels[col][row].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                gridLabels[col][row].setOpaque(true);

                // Initialize the Square for the game logic, passing the JLabel to it
                gridSquares[col][row] = new Square(gridLabels[col][row]);

                // Add the JLabel to the panel to display the grid
                add(gridLabels[col][row]);
            }
        }

        // Render original positions for all players
        for (Player player : players.values()) {
            updatePlayerPosition(player);
        }

        revalidate();
        repaint();
    }

    // ----- Local player methods -----
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
            // Try to lock the square for the local player
            Square targetSquare = gridSquares[newY][newX];

            // Attempt to lock the square and move
            if (targetSquare.tryLock(localPlayer)) {
                // Send the move to the server for validation and update
                client.sendMove(newX, newY); // Send move to server for validation

                // If the server confirms the move, then update the grid on the client
                int oldX = localPlayer.getX();
                int oldY = localPlayer.getY();
                localPlayer.setX(newX);
                localPlayer.setY(newY);

                // Release the old square lock after moving
                gridSquares[oldY][oldX].releaseLock();  // Unlock the old position

                // Update trail and player position
                updateTrail(oldX, oldY, localPlayer.getId());
                updatePlayerPosition(localPlayer);
            } else {
                // If the square is locked (or invalid), prevent the move
                System.out.println("Move blocked: Target square is occupied.");
            }
        }
    }


    private void updateTrail(int x, int y, String playerId) {
        if (gridLabels != null) {
            gridLabels[y][x].setText(""); // Clear previous trail
            gridLabels[y][x].setBackground(trailColors.get(playerId)); // Set the trail color
        }
    }

    private void updatePlayerPosition(Player player) {
        if (gridLabels != null) {
            int x = player.getX();
            int y = player.getY();
            gridLabels[y][x].setText("P"); // Player marker
            gridLabels[y][x].setBackground(parseColor(player.getColor())); // Update background color
        }
    }

    private boolean isValidMove(int x, int y) {
        // Check if the target coordinates are within bounds
        if (x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE) {
            return false; // Out of bounds
        }

        // Check if the target square is a wall (assuming you have a way to check if the square is a wall)
        if (gridSquares[y][x].isWall()) {
            return false; // Blocked by a wall
        }

        // Check if the target square is occupied by another player
        for (Player otherPlayer : players.values()) {
            if (otherPlayer.getX() == x && otherPlayer.getY() == y) {
                return false; // Target square is occupied by another player
            }
        }

        return true; // The move is valid
    }

    public void updateMaze(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(",");
            if (parts.length != 4) {
                System.err.println("Invalid message format: " + message);
                return;
            }

            // Message looks like: "playerId,newX,newY,color"
            String playerId = parts[0];
            int newX = Integer.parseInt(parts[1]);
            int newY = Integer.parseInt(parts[2]);
            String colorName = parts[3];

            // Convert color name to a Color object
            Color color = parseColor(colorName);

            Player p = players.get(playerId);
            if (p == null) {
                p = new Player(playerId, newX, newY, colorName);
                players.put(playerId, p);
                trailColors.put(playerId, calculateTrailColor(color));
            } else {
                int prevX = p.getX();
                int prevY = p.getY();
                updateTrail(prevX, prevY, playerId);
                p.setX(newX);
                p.setY(newY);
            }

            updatePlayerPosition(p);
        });
    }

    private Color calculateTrailColor(Color baseColor) {
        // Dim the brightness by reducing RGB components
        int r = (int) (baseColor.getRed() * 0.5);
        int g = (int) (baseColor.getGreen() * 0.5);
        int b = (int) (baseColor.getBlue() * 0.5);

        return new Color(r, g, b);
    }

    public GameClient getClient() {
        return client;
    }

    public void addPlayer(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] playerData = message.split(",");
            if (playerData.length != 4) {
                System.err.println("Invalid message format: " + message);
                return;
            }
            String playerId = playerData[0];
            int x = Integer.parseInt(playerData[1]);
            int y = Integer.parseInt(playerData[2]);
            String color = playerData[3];
            // Ignore local player
            if (this.localPlayer.getId().equals(playerId)) {
                return;
            }

            Player newPlayer = new Player(playerId, x, y, color);
            players.put(playerId, newPlayer);
            trailColors.put(playerId, calculateTrailColor(parseColor(color)));
            updatePlayerPosition(newPlayer);
            revalidate();
            repaint();
        });
    }

    public void removePlayer(String playerId) {
        SwingUtilities.invokeLater(() -> {
            Player playerToRemove = players.remove(playerId);
            if (playerToRemove != null) {
                // Clear the player's last position
                int x = playerToRemove.getX();
                int y = playerToRemove.getY();
                if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE && gridSquares[y][x] != null) {
                    gridSquares[y][x].clearSquare();
                }
                trailColors.remove(playerId);
                System.out.println("Player " + playerId + " left the game.");
                revalidate();
                repaint();
            } else {
                System.err.println("Player " + playerId + " not found in players map.");
            }
        });
    }


}

