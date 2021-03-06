# LightEmAll
### Gameplay
You goal is to light up every tile of the board, by ensuring that all the wires are connected to the power station. Initially, the board has been scrambled by randomly rotating each tile:

<img src="Images/Initial.png" height="180" width="150"/>

You can click to rotate tiles to join the wires together, but you cannot move tiles:

<img src="Images/Rotation.png" height="180" width="150"/>

However, there is more to the puzzle — the station is only effective up to a finite radius, and tiles beyond that radius will be unpowered:

<img src="Images/Radius.png" height="180" width="150"/>

Therefore, you will have to use the arrow keys to move the power station. Furthermore, the power station must follow the path created by the wiring. The player wins when all the tiles are lit up:

<img src="Images/Final.png" height="180" width="150"/>

### Installation
To get LightEmAll up and running, both the code and the provided JARS must be downloaded. In your IDE of choice, import all three code files, and make sure both JARS are included in the reference libraries. To play the game, run the ExamplesLightWorld class, a window should appear and the game should function as described above.
