package main.java.model;

import javax.swing.*;
import java.awt.*;

public class Grid {
    private final int size;
    private final Square[][] grid;
    private final JLabel[][] gridLabels; // To store the JLabel references for UI

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

    public Square getSquare(int x, int y) {
        if (0 <= x && x < size && 0 <= y && y < size) {
            return grid[x][y];
        } else {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
    }

    public int getSize() {
        return size;
    }

    public JLabel getLabel(int x, int y) {
        if (0 <= x && x < size && 0 <= y && y < size) {
            return gridLabels[x][y];
        } else {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
    }
}