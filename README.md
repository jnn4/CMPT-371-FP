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

## **Setup & Installation**  
1. Clone the repository:  
   ```sh
   git clone https://github.com/jnn4/CMPT-371-FP.git
   ```  
2. Start the server:  
   ```sh
    tbd
   ```  
3. Start a client:  
   ```sh
   tbd
   ```  
4. Start playing!


## **Contributors**  
- Bianca Dimaano
- Jaycie Say
- Jennifer Huang 
- Quang Anh Pham
