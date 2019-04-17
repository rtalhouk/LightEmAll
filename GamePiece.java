import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import javalib.worldimages.AlignModeX;
import javalib.worldimages.AlignModeY;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayImage;
import javalib.worldimages.OverlayOffsetAlign;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.StarImage;
import javalib.worldimages.WorldImage;

//represents a single tile on the board
class GamePiece {
  GamePiece(boolean l, boolean r, boolean t, boolean b, int ro, int co) {
    this.left = l;
    this.right = r;
    this.top = t;
    this.bottom = b;
    this.row = ro;
    this.col = co;
    this.powerStation = false;
    this.powerLevel = 0;
    this.neighbors = new HashMap<String, GamePiece>();
  }

  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;

  // whether this GamePiece is able to be connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;

  // whether the power station is on this piece
  boolean powerStation;

  // if this piece is being powered (ie close enough to the station)
  int powerLevel;

  // Represents the neighbors of this gamepiece
  HashMap<String, GamePiece> neighbors;

  // default size of image representation
  static int TILE_SIZE = 50;

  // piece before wires are placed onto it
  static WorldImage BACKGROUND = 
      new OverlayImage(
          new RectangleImage(TILE_SIZE - 1, TILE_SIZE - 1, OutlineMode.SOLID, Color.DARK_GRAY),
          new RectangleImage(TILE_SIZE, TILE_SIZE, OutlineMode.SOLID, Color.black));

  // puts a power station on this tile
  void makePowerSt() {
    this.powerStation = true;
  }

  //Connects this tile to its neighbor tiles on the board
  void connectToNeighbors(ArrayList<ArrayList<GamePiece>> board) {
    if (this.col > 0) {
      this.neighbors.put("Left", board.get(this.col - 1).get(this.row));
    }
    if (this.row > 0) {
      this.neighbors.put("Top", board.get(this.col).get(this.row - 1));
    }
    if (this.col < board.size() - 1) {
      this.neighbors.put("Right", board.get(this.col + 1).get(this.row));
    }
    if (this.row < board.get(0).size() - 1) {
      this.neighbors.put("Bottom", board.get(this.col).get(this.row + 1));
    }
  }

  //is this tile connected to the neighboring tile on the given side
  boolean isConnectedOnSide(String key) {
    GamePiece that = this.neighbors.get(key);

    if (that == null) {
      return false;
    }
    else if (key.equals("Right")) {
      return this.right && that.left;
    }
    else if (key.equals("Left")) {
      return this.left && that.right;
    }
    else if (key.equals("Top")) {
      return this.top && that.bottom;
    }
    else if (key.equals("Bottom")) {
      return this.bottom && that.top;
    }
    else {
      throw new IllegalArgumentException("Not a valid side.");
    }
  }

  //Rotates the tile left by changing each direction boolean to the one counterclockwise to it
  void rotateLeft() {
    boolean tempTop = this.top;
    this.top = this.right;
    this.right = this.bottom;
    this.bottom = this.left;
    this.left = tempTop;
  }

  // Rotates the tile right in the same manner but checking the booleans clockwise
  void rotateRight() {
    boolean tempTop = this.top;
    this.top = this.left;
    this.left = this.bottom;
    this.bottom = this.right;
    this.right = tempTop;
  }

  // draws the representation of this gamepiece by placing the wires on the blank square gamepiece
  WorldImage drawTile(int radius) {
    WorldImage result = GamePiece.BACKGROUND;
    Color color = Color.gray;
    if (this.powerLevel >= (radius * 0.75)) {
      color = Color.yellow;
    }
    else if (this.powerLevel >= (radius * 0.5)) {
      color = Color.orange;
    }
    else if (this.powerLevel >= (radius * 0.25)) {
      float[] hsb = Color.RGBtoHSB(250, 140, 0, null);
      color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]); 
    }
    else if (this.powerLevel > 0) {
      float[] hsb = Color.RGBtoHSB(160, 82, 45, null);
      color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]); 
    }

    if (this.right) {
      result = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
          new RectangleImage(TILE_SIZE / 2, 3, OutlineMode.SOLID, color), TILE_SIZE / -2, 0,
          result);
    }
    if (this.left) {
      result = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
          new RectangleImage(TILE_SIZE / 2, 3, OutlineMode.SOLID, color), TILE_SIZE / 2, 0,
          result);
    }
    if (this.bottom) {
      result = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
          new RectangleImage(3, TILE_SIZE / 2, OutlineMode.SOLID, color), 0, TILE_SIZE / -2,
          result);
    }
    if (this.top) {
      result = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
          new RectangleImage(3, TILE_SIZE / 2, OutlineMode.SOLID, color), 0, TILE_SIZE / 2,
          result);
    }
    if (this.powerStation) {
      result = new OverlayImage(
          new StarImage(TILE_SIZE / 4, 7, OutlineMode.SOLID, Color.cyan), result);
    }

    return result;
  }

  // Powers tiles within the radius given (Stops if tile is already powered)
  void powerTiles(int radius) {
    ArrayList<String> sides =
        new ArrayList<String>(Arrays.asList("Top", "Right", "Bottom", "Left"));

    this.powerLevel = radius;

    if (radius != 0) {
      for (String side : sides) {
        GamePiece neighbor = this.neighbors.get(side);
        boolean isConnected = this.isConnectedOnSide(side);

        if (isConnected && neighbor.powerLevel == 0) {
          neighbor.powerTiles(radius - 1);
        }

      }
    }
  }

  // Changes this to not have power supplied to it
  void unpower() {
    this.powerLevel = 0;
  }

  void removeStation() {
    this.powerStation = false;
  }

  void fractalConnect(String side) {
    if (side.equals("Top")) {
      this.top = true;
    }
    else if (side.equals("Bottom")) {
      this.bottom = true;
    }
    else if (side.equals("Left")) {
      this.left = true;
    }
    else if (side.equals("Right")) {
      this.right = true;
    }
  }

  void clear() {
    this.top = false;
    this.bottom = false;
    this.right = false;
    this.left = false;
  }

}

// Represents a connection between two nodes
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
}

// Represents a function that finds a node and its distance
class SearchResult {
  GamePiece node;
  int depth;

  SearchResult(GamePiece node, int depth) {
    this.node = node;
    this.depth = depth;
  }
}

//Function Class that compares two Edges by their weights
class LargerWeight implements Comparator<Edge> {

  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  }

}