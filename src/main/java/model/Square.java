package main.java.model;

public class Square {
   private boolean locked = false;
   private Player owner = null;

   public synchronized boolean isLocked() {
        return locked;
   }

   public synchronized Player getOwner() {
       return owner;
   }

   public synchronized boolean canEnter(Player player) {
       return !locked || owner == player;
   }

   public synchronized void lock(Player player) {
       locked = true;
       owner = player;
   }

   // do not use
   public synchronized void unlock() {
       locked = false;
       owner = null;
   }
}
