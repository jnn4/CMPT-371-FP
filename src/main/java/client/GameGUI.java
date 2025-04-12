package main.java.client;

import main.java.model.Player;
import main.java.model.Square;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The GameGUI class represents the main graphical user interface for the multiplayer maze game.
 * It handles both the lobby interface and the game board display, managing player visualization,
 * movement, and game state updates.
 */
public class GameGUI extends JFrame {
    private static final int WINDOW_SIZE = 1000;
    private static final int GRID_SIZE = 10;

    // Start Screen UI components
    private JPanel backgroundPanel;
    private JLabel lobbyBackground;
    private JLabel logoLabel;
    private JLabel startPromptText;
    private Font fontInkyThinPixelsLarge;
    private Font fontInkyThinPixelsBase;
    private JLayeredPane layeredPane;

    // Lobby UI components
    private JLayeredPane lobbyPanel;
    private JLabel howToPlayLabel;
    private JLabel instructions;
    private JLabel controls;
    private JLabel playersContainerLabel;
    private JLabel playerListText;
    private JLabel countdownLabel;
    private JButton readyButton;

    // Game UI components
    private JLabel[][] gridLabels; // Visual representation of the grid
    private Square[][] gridSquares; // Logical representation of grid squares
    private Player localPlayer; // The player associated with this client
    private ImageIcon characterIcon; // Image icon object for the player sprite
    private GameClient client; // Reference to the network client
    private final Map<String, Player> players = new HashMap<>(); // All players in the game
    private final Map<String, ImageIcon> playerSprites = new HashMap<>(); // Player sprites corresponding to player IDs
    private final Map<String, Color> trailColors = new HashMap<>(); // Player trail colors

    // Post Game UI components
    private JLabel gameOverPanel;
    private JLabel scoreboard;

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
            try {
                this.localPlayer = new Player(id, x, y, color);
                players.put(id, this.localPlayer);
                trailColors.put(id, calculateTrailColor(Color.decode(color)));
                playerSprites.put("P1", new ImageIcon("../../resources/images/sprites/p1.png"));
                playerSprites.put("P2", new ImageIcon("../../resources/images/sprites/p2.png"));
                playerSprites.put("P3", new ImageIcon("../../resources/images/sprites/p3.png"));
                playerSprites.put("P4", new ImageIcon("../../resources/images/sprites/p4.png"));
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
        setTitle("Onigiri Wars");
        setSize(WINDOW_SIZE, WINDOW_SIZE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // Create layered pane
        layeredPane = new JLayeredPane();
        layeredPane.setBounds(0, 0, WINDOW_SIZE, WINDOW_SIZE);
        setContentPane(layeredPane);

        // Lobby Background Panel
        backgroundPanel = new JPanel();
        backgroundPanel.setBounds(0, 0, 2065, 1000);
        backgroundPanel.setLayout(null);
        lobbyBackground = new JLabel(new ImageIcon("../../resources/images/lobby_background.png"));
        lobbyBackground.setBounds(0, 0, 2065, 1000);
        backgroundPanel.add(lobbyBackground);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        // Logo Panel
        logoLabel = new JLabel(new ImageIcon("../../resources/images/onigiri_wars_logo.png"));
        logoLabel.setBounds(100, 345, 767, 146);
        layeredPane.add(logoLabel, JLayeredPane.PALETTE_LAYER); 

        // Press Any Key To Start Label
        try {
            // Load the Inky Thin Pixels font file from resources
            fontInkyThinPixelsLarge = Font.createFont(Font.TRUETYPE_FONT, new File("../../resources/fonts/Inky Thin Pixels.ttf")).deriveFont(48f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(fontInkyThinPixelsLarge);

            // Create JLabel with custom font
            startPromptText = new JLabel("> PRESS ANY KEY TO START");
            startPromptText.setFont(fontInkyThinPixelsLarge);
            startPromptText.setForeground(new Color(41,50,65));
            startPromptText.setBounds(430, 530, 450, 55);

            // Add the text label to the layered pane
            layeredPane.add(startPromptText, JLayeredPane.PALETTE_LAYER);

        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        // Timer for blinking effect
        Timer timer = new Timer(600, new ActionListener() {
            private boolean isVisible = true; // Track visibility state
        
            @Override
            public void actionPerformed(ActionEvent e) {
                isVisible = !isVisible; // Toggle visibility
                startPromptText.setVisible(isVisible);
            }
        });

        timer.start();

        // Setup Lobby Panel
        setupLobbyPanel();
        lobbyPanel.setBounds(0, 0, WINDOW_SIZE, WINDOW_SIZE);
        lobbyPanel.setOpaque(false);
        lobbyPanel.setVisible(false); // Initially hidden
        layeredPane.add(lobbyPanel, JLayeredPane.PALETTE_LAYER); // Place above background

        // Add KeyListener to detect any key press
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                lobbyPanel.setVisible(true); // Show lobby panel when any key is pressed
                logoLabel.setVisible(false); // Hide logo when any key is pressed
                timer.stop();
                startPromptText.setVisible(false); // Hide "Press Any Key To Start" text when any key is pressed
            }
        });

        // Ensure focus for key detection
        setFocusable(true);
        requestFocusInWindow();
        
        setupKeyBindings();
        setFocusable(true);
        setVisible(true);
        requestFocusInWindow();
    }

    /**
     * Initializes the lobby panel with player list and ready button.
     */
    private void setupLobbyPanel() {
        lobbyPanel = new JLayeredPane();
        lobbyPanel.setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

        // How To Play Panel
        howToPlayLabel = new JLabel(new ImageIcon("../../resources/images/how_to_play_text.png"));
        howToPlayLabel.setBounds(200, 100, 604, 207);
        lobbyPanel.add(howToPlayLabel, JLayeredPane.DEFAULT_LAYER); 

        // How To Play Text
        try {
            // Load the Inky Thin Pixels font file from resources
            fontInkyThinPixelsBase = Font.createFont(Font.TRUETYPE_FONT, new File("../../resources/fonts/Inky Thin Pixels.ttf")).deriveFont(36f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(fontInkyThinPixelsLarge);

            // Create JLabel with custom font
            // Have to use <html> and <br> tags for newline support
            instructions = new JLabel(
                "<html>Capture tiles by stepping on them! Other players can't<br>" + 
                "step on your territory so maneuver around them<br>" + 
                "to capture the largest area you can. The player with the<br>" + 
                "most territory at the end wins.</html>");
            instructions.setFont(fontInkyThinPixelsBase);
            instructions.setForeground(new Color(41,50,65));
            instructions.setBounds(100, 265, 870, 170);

            // Add the text label to the layered pane
            lobbyPanel.add(instructions, JLayeredPane.PALETTE_LAYER);

            // Countdown label at the top
            countdownLabel = new JLabel("WAITING FOR PLAYERS...");
            countdownLabel.setFont(fontInkyThinPixelsBase);
            countdownLabel.setForeground(new Color(41,50,65));
            countdownLabel.setBounds(540, 800, 370, 50);
            lobbyPanel.add(countdownLabel, JLayeredPane.PALETTE_LAYER);

        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        // Timer for blinking effect
        Timer timer = new Timer(800, new ActionListener() {
            private boolean isVisible = true; // Track visibility state
        
            @Override
            public void actionPerformed(ActionEvent e) {
                isVisible = !isVisible; // Toggle visibility
                countdownLabel.setVisible(isVisible);
            }
        });

        timer.start();
        
        // Container for controls
        controls = new JLabel(new ImageIcon("../../resources/images/controls.png"));
        controls.setBounds(137, 512, 253, 246);
        lobbyPanel.add(controls, JLayeredPane.DEFAULT_LAYER);

        // Container for players joined
        playersContainerLabel = new JLabel(new ImageIcon("../../resources/images/players_container.png"));
        playersContainerLabel.setBounds(500, 512, 390, 355);
        lobbyPanel.add(playersContainerLabel, JLayeredPane.DEFAULT_LAYER);

        // Player list in the center
        // Create a JLabel for the player list text
        playerListText = new JLabel("<html></html>");
        playerListText.setFont(fontInkyThinPixelsBase);
        playerListText.setForeground(new Color(41, 50, 65));
        playerListText.setBounds(540, 475, 305, 300);

        // Add the label to the lobby panel
        lobbyPanel.add(playerListText, JLayeredPane.PALETTE_LAYER);

        // Ready button at the bottom
        readyButton = new JButton("READY");
        readyButton.setFont(fontInkyThinPixelsBase);
        readyButton.setForeground(Color.decode("#FFFFFF"));
        readyButton.setBackground(Color.decode("#708495"));
        readyButton.addActionListener(_ -> toggleReadyState());
        readyButton.setBounds(540, 740, 325, 54);
        readyButton.setVerticalAlignment(SwingConstants.CENTER);
        lobbyPanel.add(readyButton, JLayeredPane.PALETTE_LAYER);
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
            // playerListModel.clear();
            StringBuilder playerText = new StringBuilder("<html>"); // Start HTML formatting
            String[] players = message.split(";");
            for (String playerInfo : players) {
                if (!playerInfo.isEmpty() && playerInfo.contains(",")) {
                    String[] parts = playerInfo.split(",");
                    // playerListModel.addElement(parts[0] + " - " + parts[1]);
                    playerText.append(parts[0]).append(" - ").append(parts[1]).append("<br>");
                }
            }
            playerText.append("</html>"); // Close the HTML tags

            // Update the text of the player list label
            playerListText.setText(playerText.toString());

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
        if (localPlayer == null) {
            System.err.println("CRITICAL: Game start attempted with null localPlayer");
            System.err.println("Current players: " + players.keySet());
            JOptionPane.showMessageDialog(this,
                    "Player initialization failed. Please reconnect.",
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Set sprite image for corresponding player ID
        switch(localPlayer.getId()){
            case "P1":
                characterIcon = new ImageIcon("../../resources/images/sprites/p1.png");
                break;
            default:
                characterIcon = null;
                break;
    
        }

        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        gridPanel.setPreferredSize(new Dimension(800, 800));

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

                // add(gridLabels[row][col]);
                gridPanel.add(gridLabels[row][col]);
            }
        }
        
        client.sendMessage("INIT_STATE");
        // Update all player positions
        for (Player player : players.values()) {
            updatePlayerPosition(player);
            gridSquares[player.getY()][player.getX()].tryLock(player);
        }

        gridPanel.setBounds(1000, 100, 800, 800);
        lobbyPanel.add(gridPanel, JLayeredPane.DEFAULT_LAYER);

        // Create a Timer to make the background and grid scroll
        Timer timer = new Timer(5, new ActionListener() {
            int xPosition = 0;  // Position of the background image and grid panel
            int speed = 10; // Initial speed of scrolling
        
            @Override
            public void actionPerformed(ActionEvent e) {
                // Scroll the background, grid, and labels
                if (xPosition <= -900) {
                    xPosition = -900;
                    // Remove components after reaching target position
                    for (Component comp : lobbyPanel.getComponentsInLayer(JLayeredPane.PALETTE_LAYER)) {
                        lobbyPanel.remove(comp);
                    }
                    lobbyPanel.remove(howToPlayLabel);
                    lobbyPanel.remove(playersContainerLabel);
                } else {
                    // Gradually decrease the speed as the position moves
                    if (xPosition <= -800) {
                        speed = 5; // Slow down when nearing the end
                    }
                    xPosition -= speed; 
                }

                // Set new bounds for the background and grid
                lobbyBackground.setBounds(xPosition, 0, 2065, 1000);
                gridPanel.setBounds(xPosition + 1000, 100, 800, 800); 
                howToPlayLabel.setBounds(xPosition + 200, 100, 604, 207); 
                playersContainerLabel.setBounds(xPosition + 500, 512, 382, 355);
                instructions.setBounds(xPosition + 100, 265, 870, 170);
                controls.setBounds(xPosition + 137, 512, 253, 246);
                countdownLabel.setBounds(xPosition + 540, 800, 370, 50);
                playerListText.setBounds(xPosition + 540, 475, 305, 300);
                readyButton.setBounds(xPosition + 540, 740, 325, 54);

            }
        });

        timer.start();
        
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
            gridLabels[y][x].setBackground(trailColors.get(playerId)); // Set the trail color
            gridLabels[y][x].setIcon(null); // Clear previous trail
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
            gridLabels[y][x].setBackground(parseColor(player.getColor())); // Update background color
            gridLabels[y][x].setIcon(playerSprites.get(player.getId())); // Player sprite
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
            if (gridSquares != null) {
                gridSquares[newY][newX].tryLock(p);
            }
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
        int r = (int) (baseColor.getRed());
        int g = (int) (baseColor.getGreen());
        int b = (int) (baseColor.getBlue());

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
                String[] playerData = message.split(",");
                if (playerData.length != 4) {
                    System.err.println("Invalid player data: " + message);
                    return;
                }

                String playerId = playerData[0];
                // Skip if this is our local player
                if (localPlayer != null && localPlayer.getId().equals(playerId)) {
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
     * Displays a game over dialog with the winner's information and final scores.
     *
     * @param winnerId The ID of the winning player.
     * @param winnerScore The score of the winning player.
     * @param allScores A string containing all players' scores.
     */
    public void showGameOver(String winnerId, int winnerScore, String allScores) {
        SwingUtilities.invokeLater(() -> {
            try {
                gameOverPanel = new JLabel(new ImageIcon("../../resources/images/game_over.png"));
                gameOverPanel.setBounds(130, 45, 730,830);
                layeredPane.add(gameOverPanel, JLayeredPane.POPUP_LAYER);
                
                JLabel winnerText = null;
                switch(winnerId){
                    case "P1":
                        winnerText = new JLabel(new ImageIcon("../../resources/images/p1_win.png"));
                        break;
                    case "P2":
                        winnerText = new JLabel(new ImageIcon("../../resources/images/p2_win.png"));
                        break;
                    case "P3":
                        winnerText = new JLabel(new ImageIcon("../../resources/images/p3_win.png"));
                        break;
                    case "P4":
                        winnerText = new JLabel(new ImageIcon("../../resources/images/p4_win.png"));
                        break;
                }

                winnerText.setBounds(280, 300, 450, 130);
                layeredPane.add(winnerText, JLayeredPane.POPUP_LAYER);
                layeredPane.moveToFront(winnerText);

                // Build the scoreboard HTML from allScores
                StringBuilder scoreHtml = new StringBuilder("<html>");
                if (allScores != null && !allScores.isEmpty()) {
                    String[] playerScorePair = allScores.split(";");
                    for (String playerScore : playerScorePair) {
                        if (!playerScore.isEmpty()) {
                            String[] parts = playerScore.split(":");
                            if (parts.length == 2) {
                                scoreHtml.append(parts[0]).append(": ").append(parts[1]).append(" TILES<br>");
                            }
                        }
                    }
                }
                scoreHtml.append("</html>");
                scoreboard = new JLabel(scoreHtml.toString());
                scoreboard.setFont(fontInkyThinPixelsLarge);
                scoreboard.setForeground(new Color(255,255,255));
                scoreboard.setSize(520, Short.MAX_VALUE);  // Set desired width
                Dimension preferred = scoreboard.getPreferredSize();
                scoreboard.setBounds(250, 580, 520, preferred.height); // Add the text label to the layered pane
                layeredPane.add(scoreboard, JLayeredPane.POPUP_LAYER);
                layeredPane.moveToFront(scoreboard);
                
            } catch (Exception e) {
                System.err.println("Error showing dialog: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
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