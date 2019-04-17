import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.*;

// Represents entire game state of LightEmAll
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  Random rand;

  LightEmAll(int w, int h) {
    this.width = w;
    this.height = h;
    this.powerCol = 0;
    this.powerRow = 0;
    this.rand = new Random();
    this.board = this.makeBoard(this.width, this.height);
    this.nodes = this.makeNodeList();
    this.connectGamePieces();
    this.mst = this.kruskals();
    this.makeMSTBoard();
    this.findRadius();
    this.rotateRandomly();
    this.createPower();
  }

  LightEmAll(int w, int h, Random rand) {
    this.width = w;
    this.height = h;
    this.powerCol = 0;
    this.powerRow = 0;
    this.rand = rand;
    this.board = this.makeBoard(this.width, this.height);
    this.nodes = this.makeNodeList();
    this.connectGamePieces();
    this.mst = this.kruskals();
    this.makeMSTBoard();
    this.findRadius();
    this.rotateRandomly();
    this.createPower();
  }



  // Initializes the board to implement a fractal-like pattern by recursively breaking up
  // the board into base cases
  ArrayList<ArrayList<GamePiece>> makeBoard(int width, int height) {
    ArrayList<ArrayList<GamePiece>> result = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> curCol = new ArrayList<GamePiece>();

    for (int i = 0; i < width; i++) {
      curCol = new ArrayList<GamePiece>();
      for (int j = 0; j < height; j++) {
        curCol.add(new GamePiece(false, false, false, false, j, i));
      }
      result.add(curCol);
    }

    return result;
  }

  // Applies Kruskal's algorithm to find the minimum spanning tree of this board
  ArrayList<Edge> kruskals() {
    ArrayList<Edge> sortedEdges = this.makeEdges();
    HashMap<GamePiece, GamePiece> reps = new HashMap<GamePiece, GamePiece>();
    for (GamePiece gp : this.nodes) {
      reps.put(gp, gp);
    }
    ArrayList<Edge> mst = new ArrayList<Edge>();

    for (Edge e : sortedEdges) {
      GamePiece toNodeRep = this.findFinalRep(e.toNode, reps);
      GamePiece fromNodeRep = this.findFinalRep(e.fromNode, reps);
      if (toNodeRep != fromNodeRep) {
        reps.replace(toNodeRep, fromNodeRep);
        mst.add(e);
      }
    }
    return mst;

  }

  // Returns the GamePiece that is the given GamePiece's Kruskal representative in the
  // given Hashmap
  GamePiece findFinalRep(GamePiece piece, HashMap<GamePiece, GamePiece> reps) {
    if (reps.get(piece) == piece) {
      return piece;
    }
    else {
      return this.findFinalRep(reps.get(piece), reps);
    }
  }

  //Returns a list of edges connecting all GamePieces to their neighbors,
  //Each edge has a random weight between 0 and 250.
  //NOTE THIS CREATES BIAS FOR HORIZONTAL EDGES AS SET FORTH IN EXTRA CREDIT
  ArrayList<Edge> makeEdges() {
    ArrayList<Edge> result = new ArrayList<Edge>();
    for (int i = 0; i < width - 1; i++) {
      for (int j = 0; j < height; j++) {
        result.add(
            new Edge(this.board.get(i).get(j),
                this.board.get(i + 1).get(j),
                this.rand.nextInt(250)));
      } 
    }

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height - 1; j++) {
        result.add(
            new Edge(this.board.get(i).get(j),
                this.board.get(i).get(j + 1),
                this.rand.nextInt(250) + 100));
      }
    }

    result.sort(new LargerWeight());

    return result;
  }

  // Makes representation of the game board based on the determined minimum spanning tree of
  // the game
  void makeMSTBoard() {
    for (Edge e : this.mst) {
      GamePiece t1 = e.fromNode;
      GamePiece t2 = e.toNode;

      if (t1.row < t2.row && t1.col == t2.col) {
        t1.fractalConnect("Bottom");
        t2.fractalConnect("Top");
      }
      else if (t1.row > t2.row && t1.col == t2.col) {
        t1.fractalConnect("Top");
        t2.fractalConnect("Bottom");     
      }
      else if (t1.col < t2.col && t1.row == t2.row) {
        t1.fractalConnect("Right");
        t2.fractalConnect("Left");
      }
      else if (t1.col > t2.col && t1.row == t2.row) {
        t1.fractalConnect("Left");
        t2.fractalConnect("Right");
      }
    }
  }

  //Rotates all the GamePieces a random number of times
  void rotateRandomly() {
    for (GamePiece gp : this.nodes) {
      for (int i = 0; i < this.rand.nextInt(3); i++) {
        gp.rotateLeft();
      }
    }
  }


  // Creates a 1D array-list of all GamePieces on the board
  ArrayList<GamePiece> makeNodeList() {
    ArrayList<GamePiece> result = new ArrayList<GamePiece>();
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        result.add(this.board.get(i).get(j));
      }
    }
    return result;
  }

  // Connects GamePieces to each other so they can access their neighbors
  void connectGamePieces() {
    for (GamePiece gp : this.nodes) {
      gp.connectToNeighbors(this.board);
    }
  }

  // puts a power station on the GamePiece with the coordinate (powerCol, powerRow)
  void createPower() {
    GamePiece powerSt = this.board.get(powerCol).get(powerRow);
    powerSt.makePowerSt();
  }

  // Calls the breadth-first search on the power station and then the farthest
  // node from that power station to find the total diameter of the graph
  void findRadius() {
    SearchResult farthestFromPower = this.farthestAway(this.board.get(powerCol).get(powerRow));
    SearchResult farthestFromfarthest = this.farthestAway(farthestFromPower.node);

    int diameter = farthestFromfarthest.depth;

    this.radius = (diameter / 2) + 2;
  }

  //Performs breadth-first search from the given starting node
  // and resturns the farthest node from it and its depth.
  SearchResult farthestAway(GamePiece start) {
    ArrayList<GamePiece> visited = new ArrayList<GamePiece>();
    ArrayList<Integer> depths = new ArrayList<Integer>();

    LinkedList<SearchResult> worklist = new LinkedList<SearchResult>();

    ArrayList<String> sides = 
        new ArrayList<String>(Arrays.asList("Top", "Right", "Bottom", "Left"));

    worklist.add(new SearchResult(start, 0));
    visited.add(start);
    depths.add(0);

    while (! worklist.isEmpty()) {
      SearchResult current = worklist.remove();
      GamePiece curNode = current.node;
      int curDepth = current.depth;
      ArrayList<GamePiece> connectedTiles = new ArrayList<GamePiece>();
      for (String side : sides) {
        if (curNode.isConnectedOnSide(side)) {

          connectedTiles.add(curNode.neighbors.get(side));
        }
      }

      for (GamePiece gp : connectedTiles) {
        if (! visited.contains(gp)) {
          worklist.add(new SearchResult(gp, curDepth + 1));
          visited.add(0, gp);
          depths.add(0, curDepth + 1);
        }
      }
    }

    return new SearchResult(visited.get(0), depths.get(0));
  }

  // Returns an image representation of the World state
  public WorldScene makeScene() {
    WorldScene scene =
        new WorldScene(this.width * GamePiece.TILE_SIZE, this.height * GamePiece.TILE_SIZE);
    for (GamePiece gp : this.nodes) {
      scene.placeImageXY(gp.drawTile(this.radius),
          (gp.col * GamePiece.TILE_SIZE) + GamePiece.TILE_SIZE / 2,
          (gp.row * GamePiece.TILE_SIZE) + GamePiece.TILE_SIZE / 2);
    }

    return scene;
  }

  //Lets the user know when they have won the game
  public WorldScene lastScene(String msg) {
    WorldScene win = this.makeScene();
    win.placeImageXY(
        new TextImage(msg, 28, Color.green),
        this.width * (GamePiece.TILE_SIZE / 2),
        this.height * (GamePiece.TILE_SIZE / 2));
    return win;
  }

  // Handles mouse behavior, rotating the tile the mouse is over either left or right
  // depending on which button clicked
  public void onMouseClicked(Posn location, String button) {
    if (button.equals("LeftButton")) {
      this.getTileAt(location).rotateLeft();
    }
    else if (button.equals("RightButton")) {
      this.getTileAt(location).rotateRight();
    }
  }

  // Returns the tile on the board at which the given Posn lies
  GamePiece getTileAt(Posn location) {
    Posn tileLoc =
        new Posn(
            Math.floorDiv(location.x, GamePiece.TILE_SIZE),
            Math.floorDiv(location.y, GamePiece.TILE_SIZE));
    return this.board.get(tileLoc.x).get(tileLoc.y);
  }

  // handles behavior that occurs constantly
  // so powers every tile that is within range of the power station
  public void onTick() {
    boolean gameWon = true;

    for (GamePiece gp : this.nodes) {
      if (gp.powerLevel == 0) {
        gameWon = false;
        break;
      }
    }

    if (gameWon) {
      this.endOfWorld("You Win!");
    }
    else {
      for (GamePiece gp : this.nodes) {
        gp.unpower();
      }
      this.board.get(powerCol).get(powerRow).powerTiles(this.radius);
    } 
  }

  // Moves the power station in the direction of the arrow key pressed
  public void onKeyEvent(String keyName) {
    GamePiece powerSt = this.board.get(powerCol).get(powerRow);
    powerSt.removeStation();

    if (keyName.equals("up")) {
      if (powerSt.isConnectedOnSide("Top") && this.powerRow != 0) {
        this.powerRow--;
      }
    }
    else if (keyName.equals("down")) {
      if (powerSt.isConnectedOnSide("Bottom") && this.powerRow != this.height) {
        this.powerRow++;
      }
    }
    else if (keyName.equals("left")) {
      if (powerSt.isConnectedOnSide("Left") && this.powerCol != 0) {
        this.powerCol--;
      }
    }
    else if (keyName.equals("right")) {
      if (powerSt.isConnectedOnSide("Right") && this.powerCol != this.width) {
        this.powerCol++;
      }
    }
    else if (keyName.equals("r")) {
      this.clearBoard();
      this.powerCol = 0;
      this.powerRow = 0;
      this.mst = this.kruskals();
      this.makeMSTBoard();
      this.findRadius();
      this.rotateRandomly();
    }
    this.createPower();
  }

  void clearBoard() {
    for (GamePiece gp : this.nodes) {
      gp.clear();
    }
  }
}


class ExamplesLightWorld {
  GamePiece gp;
  GamePiece gp2;
  GamePiece gp3;
  ArrayList<ArrayList<GamePiece>> board;
  ArrayList<ArrayList<GamePiece>> blankBoard;
  ArrayList<GamePiece> column;
  WorldImage gp3draw = new OverlayImage(new RectangleImage(
      49, 49, OutlineMode.SOLID, Color.DARK_GRAY),
      new RectangleImage(50, 50, OutlineMode.SOLID, Color.black));
  HashMap<GamePiece, GamePiece> exampleReps;
  LargerWeight weightComp;
  LightEmAll world;

  void reset() {
    gp = new GamePiece(true, true, true, true, 0, 0);
    gp2 = new GamePiece(false, false, true, false, 1, 0);
    gp3 = new GamePiece(false, false, false, false, 2, 0);
    board = new ArrayList<ArrayList<GamePiece>>();
    column = new ArrayList<GamePiece>();
    column.add(gp);
    column.add(gp2);
    column.add(gp3);
    board.add(column);
    world = new LightEmAll(5, 6, new Random(5));
    blankBoard = world.makeBoard(5, 6);
    exampleReps = new HashMap<GamePiece, GamePiece>();
    exampleReps.put(gp, gp);
    exampleReps.put(gp2, gp);
    exampleReps.put(gp3, gp2);
    weightComp = new LargerWeight();

  }


  void testBigBang(Tester t) {
    reset();
    int worldWidth = this.world.width * GamePiece.TILE_SIZE;
    int worldHeight = this.world.height * GamePiece.TILE_SIZE;
    double tickRate = 1.0 / 28.0;
    this.world.bigBang(worldWidth, worldHeight, tickRate);
    reset();
  }

  void testMakePower(Tester t) {
    reset();
    this.gp.makePowerSt();
    t.checkExpect(this.gp.powerStation, true);
    reset();
  }

  void testConnect(Tester t) {
    reset();
    this.gp2.connectToNeighbors(board);
    this.gp.connectToNeighbors(board);
    t.checkExpect(this.gp2.neighbors.get("Top"), this.gp);
    t.checkExpect(this.gp.neighbors.get("Bottom"), this.gp2);
    reset();
  }

  void testRotate(Tester t) {
    reset();
    t.checkExpect(gp2.left, false);
    gp2.rotateLeft();
    t.checkExpect(gp2.left, true);
    gp2.rotateRight();
    t.checkExpect(gp2.left, false);
    reset();
  }

  void testDrawTile(Tester t) {
    reset();
    t.checkExpect(gp3.drawTile(10), this.gp3draw);
    t.checkExpect(gp2.drawTile(10), new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RectangleImage(3, 25, OutlineMode.SOLID, Color.gray), 0, 25,
        this.gp3draw));
  }

  void testUnpower(Tester t) {
    reset();
    gp.unpower();
    t.checkExpect(this.gp.powerLevel, 0);
    reset();
  }

  void testPowerTiles(Tester t) {
    reset();
    gp.connectToNeighbors(board);
    gp2.connectToNeighbors(board);
    gp3.connectToNeighbors(board);
    t.checkExpect(gp.powerLevel > 0, false);
    t.checkExpect(gp2.powerLevel > 0, false);
    t.checkExpect(gp3.powerLevel > 0, false);
    gp.powerTiles(10);
    t.checkExpect(gp.powerLevel > 0, true);
    t.checkExpect(gp2.powerLevel > 0,true);
    t.checkExpect(gp3.powerLevel > 0,false);
    reset();
  }

  void testIsConnected(Tester t) {
    reset();
    t.checkExpect(this.gp.isConnectedOnSide("Right"), false);
    t.checkExpect(this.gp.isConnectedOnSide("Bottom"), false);
    gp.connectToNeighbors(board);
    t.checkExpect(this.gp.isConnectedOnSide("Right"), false);
    t.checkExpect(this.gp.isConnectedOnSide("Bottom"), true);
    reset();
  }

  void testPowerStation(Tester t) {
    reset();
    GamePiece tile = world.board.get(0).get(0);
    t.checkExpect(tile.powerStation, true);
    this.world.powerCol = 0;
    this.world.powerRow = 0;
    this.world.createPower();
    t.checkExpect(tile.powerStation, true);
    tile.removeStation();
    t.checkExpect(tile.powerStation, false);
    tile.makePowerSt();
    t.checkExpect(tile.powerStation, true);
    reset();
  }

  void testOnKey(Tester t) {
    reset();
    int tempRow = this.world.powerRow;
    int tempCol = this.world.powerCol;

    this.world.onKeyEvent("up");
    t.checkExpect(world.powerCol, tempCol);
    t.checkExpect(world.powerRow, tempRow);

    world.board.get(0).get(1).rotateRight();

    this.world.onKeyEvent("down");
    t.checkExpect(world.powerCol, tempCol);
    t.checkExpect(world.powerRow, tempRow + 1);
    reset();

    this.world.onKeyEvent("r");
    t.checkExpect(this.world.powerCol, 0);
    t.checkExpect(this.world.powerRow, 0);
  }

  void testGetTileAt(Tester t) {
    reset();
    Posn psn = new Posn(GamePiece.TILE_SIZE / 2, GamePiece.TILE_SIZE / 2);
    t.checkExpect(this.world.getTileAt(psn), world.board.get(0).get(0));
    reset();
  }

  void testFindRadius(Tester t) {
    reset();
    t.checkExpect(world.radius, 8);
    reset();
  }

  void testFarthestAway(Tester t) {
    reset();
    world.board.get(0).get(1).rotateRight();
    GamePiece start = world.board.get(world.powerCol).get(world.powerRow);
    SearchResult result = this.world.farthestAway(start);
    t.checkExpect(result.node, world.board.get(0).get(1));
    t.checkExpect(result.depth, 1);
    reset();
  }

  void testMouseClick(Tester t) {
    reset();
    Posn psn = new Posn(GamePiece.TILE_SIZE / 2, GamePiece.TILE_SIZE / 2);
    GamePiece tile = this.world.board.get(0).get(0);
    t.checkExpect(tile.bottom, true);
    this.world.onMouseClicked(psn, "LeftButton");
    t.checkExpect(tile.right, true);
    t.checkExpect(tile.bottom,false);
    this.world.onMouseClicked(psn, "RightButton");
    t.checkExpect(tile.right, false);
    t.checkExpect(tile.bottom, true);
    reset(); 
  }

  void testFractalConnect(Tester t) {
    reset();
    t.checkExpect(gp3.right, false);
    gp3.fractalConnect("Right");
    t.checkExpect(gp3.right, true);
    reset();
  }

  void testMakeEdges(Tester t) {
    reset();
    ArrayList<Edge> edges = world.makeEdges();
    t.checkExpect(
        edges.get(0),
        new Edge(world.board.get(3).get(0), world.board.get(4).get(0), 3));
    t.checkExpect(edges.get(0).weight <= edges.get(1).weight, true);
    reset();
  }

  void testKruskals(Tester t) {
    reset();
    t.checkExpect(world.mst.size(), world.nodes.size() - 1);
    reset();
  }

  void testFindFinalRep(Tester t) {
    reset();
    t.checkExpect(world.findFinalRep(gp, exampleReps), gp);
    t.checkExpect(world.findFinalRep(gp2, exampleReps), gp);
    t.checkExpect(world.findFinalRep(gp3, exampleReps), gp);
    reset();
  }

  void testWeightComparator(Tester t) {
    reset();
    Edge e1 = new Edge(gp, gp2, 3);
    Edge e2 = new Edge(gp2, gp3, 4);
    t.checkExpect(weightComp.compare(e1, e2) < 0, true);
    t.checkExpect(weightComp.compare(e2, e1) > 0, true);
    reset();
  }

  void testClearBoard(Tester t) {
    reset();
    this.world.clearBoard();
    t.checkExpect(this.world.board.get(0).get(0).bottom, false);
    t.checkExpect(this.world.board.get(3).get(2).left, false);
    t.checkExpect(this.world.board.get(2).get(4).right, false);
    reset();
  }

  void testClear(Tester t) {
    reset();
    this.gp3.clear();
    t.checkExpect(gp3.top, false);
    t.checkExpect(gp3.bottom, false);
    t.checkExpect(gp3.left, false);
    t.checkExpect(gp3.right, false);
    reset();
  }

}
