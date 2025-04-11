package main.java.server;

/**
 * The Observer interface defines the contract for objects that should be notified
 * of changes in observable subjects. This follows the Observer design pattern.
 *
 * Implementations of this interface can register with observable objects
 * and receive updates when the observable's state changes.
 */
public interface Observer {

    // Called when the observed object's state changes
    void update(String message);
}
