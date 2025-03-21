async function joinGame() {
    const name = document.getElementById("playerName").value;
    if (!name) {
        alert("Please enter a name to join the game!");
        return;
    }

    try {
        const response = await fetch("http://localhost:3000/join", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name })
        });

        if (!response.ok) {
            throw new Error("Failed to join the game");
        }

        const data = await response.json();
        document.getElementById("gameStatus").innerHTML += `<p>${data.message}</p>`;
    } catch (error) {
        console.error("Error:", error);
        document.getElementById("gameStatus").innerHTML += `<p style="color: red;">Error: ${error.message}</p>`;
    }
}

async function updateGameState() {
    try {
        const response = await fetch("http://localhost:3000/gameState");

        if (!response.ok) {
            throw new Error("Failed to retrieve game state");
        }

        const data = await response.json();

        document.getElementById("gameStatus").innerHTML = `<h3>Players:</h3>`;

        if (data.players && data.players.length > 0) {
            document.getElementById("gameStatus").innerHTML += `<p>${data.players.join(", ")}</p>`;
        } else {
            document.getElementById("gameStatus").innerHTML += `<p>No players currently</p>`;
        }
    } catch (error) {
        console.error("Error:", error);
        document.getElementById("gameStatus").innerHTML += `<p style="color: red;">Error: ${error.message}</p>`;
    }
}