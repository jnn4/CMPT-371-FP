# CMPT-371-FP
Final Project for CMPT 371

### CLIENT
##### GameClient
- runs Java Swift for GUI
- creates new Thread for each player added with unique ID
- listens for messages on server and sends messages to update GUI

##### GameGUI
- updates GUI based on key pressed
### MODEL
##### Maze
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
