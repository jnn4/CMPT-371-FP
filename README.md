# CMPT-371-FP
Final Project for CMPT 371

## notes
- check winner game over on timer or when all squares full?

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