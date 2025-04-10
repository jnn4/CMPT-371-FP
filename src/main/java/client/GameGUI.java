package main.java.client;

import main.java.model.Player;
import main.java.model.Square;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The GameGUI class represents the main graphical user interface for the multiplayer maze game.
 * It handles both the lobby interface and the game board display, managing player visualization,
 * movement, and game state updates.
 */
public class GameGUI extends JFrame {
    private static final int WINDOW_SIZE = 400;
    private static final int GRID_SIZE = 10;

    // Lobby UI components
    private JPanel lobbyPanel;
    private JLabel countdownLabel;
    private DefaultListModel<String> playerListModel;
    private JList<String> playerList;
    private JButton readyButton;

    // Game UI components
    private JLabel[][] gridLabels; // Visual representation of the grid
    private Square[][] gridSquares; // Logical representation of grid squares
    private Player localPlayer; // The player associated with this client
    private GameClient client; // Reference to the network client
    private final Map<String, Player> players = new HashMap<>(); // All players in the game
    private final Map<String, Color> trailColors = new HashMap<>(); // Player trail colors

    /**
     * Parses a color name string into a Color object.
     * @param colorName The name of the color (RED, GREEN, BLUE, YELLOW)
     * @return The corresponding Color object, or GRAY if unknown
     */
    private Color parseColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "BLUE" -> Color.BLUE;
            case "YELLOW" -> Color.YELLOW;
            default -> Color.GRAY;
        };
    }

    // Synchronization lock for player operations
    private final Object playerLock = new Object();

    /**
     * Sets the local player for this client with thread-safe synchronization.
     * @param id The player's unique identifier
     * @param x The initial x-coordinate position
     * @param y The initial y-coordinate position
     * @param color The player's color in hex or name format
     */
    public void setLocalPlayer(String id, int x, int y, String color) {
        synchronized(playerLock) {
            // System.out.println("Setting local player: " + id);
            try {
                this.localPlayer = new Player(id, x, y, color);
                players.put(id, this.localPlayer);
                trailColors.put(id, calculateTrailColor(Color.decode(color)));
                // System.out.println("Local player set successfully: " + localPlayer);
            } catch (Exception e) {
                System.err.println("Error setting local player: ");
                e.printStackTrace();
            }
        }
    }

    /**
     * Constructs the game GUI and initializes all components.
     * @param client The GameClient instance handling network communication
     */
    public GameGUI(GameClient client) {
        this.client = client;
        setTitle("Multiplayer Maze Game");
        setSize(WINDOW_SIZE, WINDOW_SIZE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        setupLobbyPanel();
        getContentPane().add(lobbyPanel, BorderLayout.CENTER);
        lobbyPanel.setVisible(true);

        setupKeyBindings();
        setFocusable(true);
        setVisible(true);
        requestFocusInWindow();
    }

    /**
     * Initializes the lobby panel with player list and ready button.
     */
    private void setupLobbyPanel() {
        lobbyPanel = new JPanel(new BorderLayout());
        lobbyPanel.setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

        // Countdown label
        countdownLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
        lobbyPanel.add(countdownLabel, BorderLayout.PAGE_START);

        // Player list
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setVisibleRowCount(4);
        playerList.setFixedCellHeight(20);

        JScrollPane playerListScrollPane = new JScrollPane(playerList);
        playerListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        playerListScrollPane.setPreferredSize(new Dimension(200, 150));
        lobbyPanel.add(playerListScrollPane, BorderLayout.CENTER);

        // Ready button
        readyButton = new JButton("READY");
        readyButton.addActionListener(_ -> toggleReadyState());
        lobbyPanel.add(readyButton, BorderLayout.PAGE_END);
    }

    /**
     * Toggles the player's ready state and notifies the server.
     */
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

    /**
     * Updates the lobby display with current player states.
     * @param message The server message containing player readiness information
     */
    public void updateLobby(String message) {
        SwingUtilities.invokeLater(() -> {
            playerListModel.clear();
            String[] players = message.split(";");
            for (String playerInfo : players) {
                if (!playerInfo.isEmpty() && playerInfo.contains(",")) {
                    String[] parts = playerInfo.split(",");
                    playerListModel.addElement(parts[0] + " - " + parts[1]);
                }
            }
            lobbyPanel.revalidate();
            lobbyPanel.repaint();
        });
    }

    /**
     * Updates the countdown display in the lobby.
     * @param seconds The remaining seconds until game start
     */
    public void updateCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            countdownLabel.setText("Game starting in " + seconds);
            if (seconds == 0) {
                getContentPane().removeAll();
                revalidate();
                repaint();
                startGame();
            }
        });
    }

    /**
     * Handles countdown abortion from the server.
     */
    public void abortCountdown() {
        SwingUtilities.invokeLater(() -> countdownLabel.setText("Countdown aborted. Waiting for all players to be ready."));
    }

    /**
     * Initializes and displays the game board.
     */
    public void startGame() {
        System.out.println("Attempting to start game...");
        if (localPlayer == null) {
            System.err.println("CRITICAL: Game start attempted with null localPlayer");
            System.err.println("Current players: " + players.keySet());
            JOptionPane.showMessageDialog(this,
                    "Player initialization failed. Please reconnect.",
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        getContentPane().removeAll();
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        // Initialize game grid
        gridLabels = new JLabel[GRID_SIZE][GRID_SIZE];
        gridSquares = new Square[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                gridLabels[row][col] = new JLabel("", SwingConstants.CENTER);
                gridLabels[row][col].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                gridLabels[row][col].setOpaque(true);

                gridSquares[row][col] = new Square(gridLabels[row][col]);
                gridSquares[row][col].tryLock(localPlayer);
                gridSquares[row][col].setWall(false);

                add(gridLabels[row][col]);
            }
        }

        // Update all player positions
        for (Player player : players.values()) {
            updatePlayerPosition(player);
        }

        revalidate();
        repaint();
        requestFocusInWindow();
    }

    // ----- Local player methods -----
    /**
     * Sets up key bindings for player movement controls. The player can move
     * in four directions: up, down, left, and right using the arrow keys.
     */
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

    /**
     * Attempts to move the local player to a new position.
     * If the move is valid, the move request is sent to the server.
     *
     * @param newX The new X-coordinate.
     * @param newY The new Y-coordinate.
     */
    private void attemptMove(int newX, int newY) {
        if (isValidMove(newX, newY)) {
            client.sendMove(newX, newY);
        }
    }

    /**
     * Updates the trail of a player at the given coordinates by clearing
     * the previous trail and updating the background color to reflect the player's trail.
     *
     * @param x The X-coordinate of the trail.
     * @param y The Y-coordinate of the trail.
     * @param playerId The ID of the player.
     */
    private void updateTrail(int x, int y, String playerId) {
        if (gridLabels != null) {
            gridLabels[y][x].setText(""); // Clear previous trail
            gridLabels[y][x].setBackground(trailColors.get(playerId)); // Set the trail color
        }
    }

    /**
     * Updates the position of a player on the grid, marking the player's new position
     * and updating the background color to the player's assigned color.
     *
     * @param player The player whose position needs to be updated.
     */
    private void updatePlayerPosition(Player player) {
        if (gridLabels != null) {
            int x = player.getX();
            int y = player.getY();
            gridLabels[y][x].setText("P"); // Player marker
            gridLabels[y][x].setBackground(parseColor(player.getColor())); // Update background color
        }
    }

    /**
     * Checks whether a move to the specified coordinates is valid.
     * Validates whether the target square is within bounds, not blocked by a wall,
     * and not occupied by another player.
     *
     * @param x The X-coordinate of the target square.
     * @param y The Y-coordinate of the target square.
     * @return True if the move is valid, false otherwise.
     */
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

    /**
     * Updates the maze based on a message from the server. The message includes
     * the player ID, the new X and Y coordinates, and the player's color.
     *
     * @param message The message containing player update details in the format: "playerId,newX,newY,color".
     */
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

    /**
     * Calculates a dimmed version of the base color for the player's trail.
     * The trail color is obtained by reducing the RGB components of the base color.
     *
     * @param baseColor The base color.
     * @return A new color with dimmed RGB components for the trail.
     */
    private Color calculateTrailColor(Color baseColor) {
        // Dim the brightness by reducing RGB components
        int r = (int) (baseColor.getRed() * 0.5);
        int g = (int) (baseColor.getGreen() * 0.5);
        int b = (int) (baseColor.getBlue() * 0.5);

        return new Color(r, g, b);
    }

    /**
     * Adds a new player to the game based on the server's message. The message includes
     * the player ID, the player's initial X and Y coordinates, and the player's color.
     *
     * @param message The message containing player details in the format: "playerId,x,y,color".
     */
    public void addPlayer(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                // System.out.println("Adding player: " + message);
                String[] playerData = message.split(",");
                if (playerData.length != 4) {
                    System.err.println("Invalid player data: " + message);
                    return;
                }

                String playerId = playerData[0];
                // Skip if this is our local player
                if (localPlayer != null && localPlayer.getId().equals(playerId)) {
                    System.out.println("Skipping local player in addPlayer");
                    return;
                }

                int x = Integer.parseInt(playerData[1]);
                int y = Integer.parseInt(playerData[2]);
                String color = playerData[3];

                Player newPlayer = new Player(playerId, x, y, color);
                players.put(playerId, newPlayer);
                trailColors.put(playerId, calculateTrailColor(parseColor(color)));

                if (gridLabels != null) {
                    updatePlayerPosition(newPlayer);
                }
            } catch (Exception e) {
                System.err.println("Error in addPlayer:");
                e.printStackTrace();
            }
        });
    }

    /**
     * Removes a player from the game based on their player ID. Clears the player's
     * last known position and updates the game state.
     *
     * @param playerId The ID of the player to remove.
     */
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

    /**
     * Confirms a player's move. This method updates the player's position on the grid
     * and handles the locking and unlocking of grid squares.
     *
     * @param playerId The ID of the player who moved.
     * @param newX The new X-coordinate of the player.
     * @param newY The new Y-coordinate of the player.
     */
    public void onMoveConfirmed(String playerId, int newX, int newY) {
        SwingUtilities.invokeLater(() -> {
            Player player = players.get(playerId);
            if (player == null) return;

            int oldX = player.getX();
            int oldY = player.getY();

            if (!gridSquares[newY][newX].tryLock(player)) {
                System.out.println("Move blocked: Target square is locked.");
                return;
            }

            player.setX(newX);
            player.setY(newY);

            if (gridSquares[oldY][oldX] != null) {
                gridSquares[oldY][oldX].releaseLock();
            }

            updateTrail(oldX, oldY, playerId);
            updatePlayerPosition(player);
        });
    }

    /**
     * Sets the client instance to interact with the server.
     *
     * @param client The GameClient instance to set.
     */
    public void setClient(GameClient client) {
        this.client = client;
    }
}