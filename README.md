# **CMPT 371 Final Project - Onigiri Wars**  

### *A Multiplayer Land-Claiming Strategy Game*  

---

## **Overview**  
**Onigiri Wars** is a multiplayer, real-time, competitive land-claiming game built using a client-server architecture with raw socket communication. Players move across a shared 10x10 grid, claiming tiles and blocking opponents from crossing their paths. The goal is to have the most tiles by the end of the game.  

---


## **How to Play** 🎮
1. **Start the server** – Any player can host the game.  
2. **Join as a client** – Other players connect remotely.  
3. **Move with arrow keys** to claim tiles.  
4. **Claimed tiles act as walls**, blocking other players.  
5. **Use strategy to cut off opponents** and expand your territory.  
6. **Game ends when the timer runs out** – the player with the most claimed tiles **wins**! 🥳

---

## **More information...**
- **How the Game Works**:
    - The **server keeps track of the game** and makes sure all players are in sync with each other.
    - Players send their **movement commands** to the server and receive updates in real time about other players' actions.

- **Claiming Tiles**:
    - **The grid is made up of tiles** that players can claim.
    - Once a player claims a tile, it **becomes locked** and no other player can step on it.
    - The **server manages the flow of the game**, ensuring that players' moves are handled in the right order.

- **Winning the Game**:
    - The game **ends when the timer runs out** or when **all tiles have been claimed**.
    - The player who controls the **most tiles at the end** wins the game!

---

## **Setup & Installation**  
1. Clone the repository:  
   ```sh
      git clone https://github.com/jnn4/CMPT-371-FP
   ```  
2. Start the server:
   ```sh
      cd src/main/java/server
      java GameServer.java
   ```  
3. Start a client:
   ```sh
      cd src/main/java/client
      java GameClient.java # run multiple instances of this command to simulate multiple players
   ```

4. Start playing!

---

## **Contributors**
- Bianca Dimaano
- Jaycie Say
- Jennifer Huang
- Quang Anh Pham
