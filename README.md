# **CMPT 371 Final Project - [game name]**  

### *A Multiplayer Land-Claiming Strategy Game*  

## **Overview**  
[game name] is a multiplayer, real-time, competitive land-claiming game built using a client-server architecture with raw socket communication. Players move across a shared 30x30 grid, claiming tiles and blocking opponents from crossing their paths. The goal is to have the most tiles by the end of the game.  

## **How to Play** 🎮 
1. **Start the server** – Any player can host the game.  
2. **Join as a client** – Other players connect remotely.  
3. **Move with arrow keys (or WASD)** to claim tiles.  
4. **Claimed tiles act as walls**, blocking other players.  
5. **Use strategy to cut off opponents** and expand your territory.  
6. **Game ends when the timer runs out** – the player with the most claimed tiles **wins**! 🥳


## **Game Mechanics**  
- **Client-Server Model**:  
  - The **server manages the game state** and synchronizes player positions.  
  - **Clients send movement commands** and receive real-time updates.  

- **Shared Object & Concurrency**:  
  - **The grid tiles are shared resources.**  
  - Once a tile is claimed, **it is locked** and **cannot be overwritten or stepped on by others**.  
  - The **server ensures concurrency control**, processing movement requests sequentially.  

- **Winning Condition**:  
  - The game **ends after a timer expires or when all tiles are claimed**.  
  - The player with the **most claimed tiles wins**.  
---
### CLIENT
##### GameClient
- runs Java Swift for GUI
- creates new Thread for each player added with unique ID
- listens for messages on server and sends messages to update GUI

##### GameGUI
- updates GUI based on key pressed
### MODEL
##### Grid
- 2D vector of Square Objects
##### Square
- Reentrant Lock when a user goes on a square, locks based on user
- user is only allowed to enter if they are the ones that locked the square or the square is unlocked
##### Player
- has unique ID and color
- can get position of player (x, y coordinates)
### SERVER
##### GameServer
- starts TCP server with sockets for communication between client and server 
- players map tracks current players in game, used for game logic
- clients set tracks active client connections for sending messages and updates to all clients

*ClientHandler*
- each instance of `ClientHandler` represents a single player in the game
- runs on a separate thread, handling communication between the client and server
- assigns unique ID, position and color to players
- process movement requests and ensures valid moves inside the maze
- notifies all players when someone joins, moves or leaves
- connection using socket
- PrintWriter output stream
- uses `Player` class in model

## **Setup & Installation**  
1. Clone the repository:  
   ```sh
   git clone https://github.com/jnn4/CMPT-371-FP.git
   ```  
2. Start the server:  
   ```sh
    java GameServer.java
   ```  
3. Start a client:  
   ```sh
   java GameClient.java 
   // run multiple instances of this command to act as other players
   ```  
4. Start playing!


## **Contributors**  
- Bianca Dimaano
- Jaycie Say
- Jennifer Huang 
- Quang Anh Pham