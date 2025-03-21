# CMPT-371-FP
### Start the Java TCP Server
```shell
    cd game-server
    javac GameServer.java PlayerHandler.java
    java GameServer
```
### Start the Node.js Proxy
```shell
    cd bridge
    npm install express cors net
    node tcpProxy.js
```
### Open the Frontend
Open `start.html` in a browser!