# CMPT 371 Final Project - Onigiri Wars🍙 

### *A Multiplayer Land-Claiming Strategy Game*  
---

## **Overview**  
**Onigiri Wars** is a multiplayer, real-time, competitive land-claiming game built using a client-server architecture with raw socket communication. Players move across a shared 10x10 grid, claiming tiles and blocking opponents from crossing their paths. The goal is to have the most tiles by the end of the game.  

---

## **Video Demo**
YouTube Link: https://www.youtube.com/watch?v=evRVT6tB6cI

---
## **How to Play** 🎮
1. **Start the server** – Any player can host the game.  
2. **Join as a client** – Other players connect remotely.  
3. **Move with arrow keys** to claim tiles.  
4. **Claimed tiles act as walls**, blocking other players.  
5. **Use strategy to cut off opponents** and expand your territory.  
6. **Game ends when the timer runs out** – the player with the most claimed tiles **wins**! 🥳

---

## Interface Screenshots
### ⭐ Landing Screen
Players will land at this screen upon joining the game. To join the lobby, click any key on the keyboard.

![image](https://github.com/user-attachments/assets/db89f398-b8b0-410c-98c0-923bf9a0bfb2)
---
### 📜 Lobby with Game Instructions
The lobby contains instructions on how to play the game, keyboard controls, and a list of players in the lobby. 
\
To ready up, click the "READY" button. The game will start once all palyers are ready.

![image](https://github.com/user-attachments/assets/bbe36417-2b6c-478a-839d-ca1adaf42ef1)
---
### 🎮 Game Screen
The game board consists of a 10x10 grid. Claim the most tiles to win! 

![image](https://github.com/user-attachments/assets/257f804e-76ec-4b34-b38d-608fce3cd9fa)
---
### 💢 Game Over Screen
When the game ends, a recap popup will display the winning player and a scoreboard.

![image](https://github.com/user-attachments/assets/73efb3c2-2bcb-4a93-a98e-712b8a781e4f)

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
