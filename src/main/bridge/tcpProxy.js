const net = require("net"); // Create a TCP connection
const express = require("express"); // Handling HTTP requests
const cors = require('cors'); // Allow Cross-Origin Resource Sharing

// Create an instance of the express app
const app = express(); // Enable CORS for all requests
app.use(cors()); // Allow cross-origin requests from any origin
// Preflight request handling to respond to OPTIONS requests and set CORS headers
app.options('*', (req, res) => {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    res.header("Access-Control-Allow-Headers", "Content-Type");
    res.sendStatus(200);
});

// Middleware to parse JSON request bodies
app.use(express.json()); // Ensure JSON body parsing

const GAME_SERVER_HOST = "localhost";
const GAME_SERVER_PORT = 12345;

// Send data to the game server over TCP and handle the response
function sendToGameServer(data, callback) {
    const client = new net.Socket();

    client.connect(GAME_SERVER_PORT, GAME_SERVER_HOST, () => {
        // Once connected, send the provided data to the server
        client.write(data);
    });

    client.on("data", (response) => {
        callback(response.toString());
        // Close the connection after receiving the response
        client.destroy();
    });

    client.on("error", (err) => {
        console.error("Error:", err);
        // Call the callback with null, indicating an error occurred
        callback(null);
    });
}

app.post("/join", (req, res) => {
    sendToGameServer("JOIN", (response) => {
        console.log(response);
        // Send a JSON response back to the client with the server's message
        res.json({ message: response });
    });
});

app.get("/gameState", (req, res) => {
    sendToGameServer("GET_GAME_STATE", (gameState) => {
        // Send the game state back to the client as a JSON object
        res.json({ gameState });
    });
});

// Start the Express server and listen on port 3000
app.listen(3000, () => console.log("TCP Proxy running on port 3000"));