package main.java.model;

import javax.swing.*;
import java.awt.*;

/**
 * Represents the game board as a 2D grid of squares.
 * The Grid class maintains both the representations of each square in the grid. 
 * 
 * This class provides methods to access individual squares and their corresponding 
 * visual components, ensuring proper bounds checking.
 */
public class Grid {
    private final int size;
    private final Square[][] grid;
    private final JLabel[][] gridLabels; // To store the JLabel references for UI

    /**
     * Constructor a new Grid with the specified dimensions
     * Initializes both the logical grid of Square objects and their
     * JLabel visual representations.
     *
     * @param size The width and height of the grid (grid will be size x size)
     */
    public Grid(int size) {
        this.size = size;
        grid = new Square[size][size];
        gridLabels = new JLabel[size][size];

        // Initialize the grid and the gridLabels
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                gridLabels[i][j] = new JLabel(" ", SwingConstants.CENTER); // Initialize JLabel
                gridLabels[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                gridLabels[i][j].setOpaque(true);

                // Create the Square with the corresponding JLabel
                grid[i][j] = new Square(gridLabels[i][j]);
            }
        }
    }
    /**
     * Retrieves the Square object at the specified coordinates.
     *
     * @param x The x-coordinate (column) of the square
     * @param y The y-coordinate (row) of the square
     * @return The Square object at the specified coordinates
     */
    public Square getSquare(int x, int y) {
        if (0 <= x && x < size && 0 <= y && y < size) {
            return grid[x][y];
        } else {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
    }

    /**
     * Gets the size (width and height) of the grid.
     *
     * @return The size of the grid
     */
    public int getSize() {
        return size;
    }

    /**
     * Retrieves the JLabel object at the specified coordinates.
     *
     * @param x The x-coordinate (column) of the label
     * @param y The y-coordinate (row) of the label
     * @return The JLabel object at the specified coordinates
     */
    public JLabel getLabel(int x, int y) {
        if (0 <= x && x < size && 0 <= y && y < size) {
            return gridLabels[x][y];
        } else {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
    }
}