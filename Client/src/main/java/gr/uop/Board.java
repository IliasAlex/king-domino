package gr.uop;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

class Board {
    private GridPane gameBoard;
    private ArrayList<Tile> tiles = new ArrayList<>();
    private ArrayList<Integer> selectedDominos = new ArrayList<>();
    private Stage dStage;
    
    public Board(GridPane gameBoard, int x, int y){
        this.gameBoard = gameBoard;
        this.gameBoard.setPrefSize(50*x, 50*y);
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
    
                Rectangle rect = new Rectangle(50, 50);
                tiles.add(new Tile(rect, i, j));
                
                rect.setStroke(Color.WHITE);
                rect.setStrokeWidth(3);
                rect.setFill(Color.WHEAT);
                
                this.gameBoard.add(new StackPane(rect), j, i);
            }
        }
    }

    public void drawCastle(String color) {
        Image img = new Image("file:tiles/castle" + color + ".png");
        for (Tile tile : tiles) {
            if (tile.getX() == 4 && tile.getY() == 4) {
                Platform.runLater(() -> {
                    tile.getRect().setFill(new ImagePattern(img));
                    System.out.println("CASTE FILL: "+ tile.getRect().getFill());
                });
                tile.setDominoNumber(-1);
                break;
            }
        }
    }

    public Tile getTile(int x, int y) {
        for (Tile tile : tiles) {
            if(tile.getX() == x && tile.getY() == y) {
                return tile;
            }
        }
        return null;
    }

    public ArrayList<Tile> getTiles() {
        return tiles;
    }

    public void drawNumber(int x, int y, int number) {
        gameBoard.add(new StackPane(new Label("" +number)),y,x);
    }

    public void removeNumber(int x, int y){
        Platform.runLater(()->{
            Rectangle rect = new Rectangle(50, 50);
            rect.setStroke(Color.WHITE);
            rect.setStrokeWidth(3);
            rect.setFill(Color.WHEAT);
            gameBoard.add(rect, y, x);
        });
    }

    public Boolean drawDomino(Tile tile, int dominoNumber, String orientation) {
        int minX = minX(), maxX = maxX(), minY = minY(), maxY = maxY();
        if( maxX-minX < 4){
            maxX = 100;
            minX = -1;
        }
        if( maxY - minY < 4){
            maxY = 100;
            minY = -1;
        }
        if(orientation.equalsIgnoreCase("horizontal 1->2")) {
            if(gameBoard.getRowCount() == 7) {
                ImagePattern tile1Image = getImage(dominoNumber, 1);
                ImagePattern tile2Image = getImage(dominoNumber, 2);
                tile.getRect().setFill(tile1Image);
                tile.setDominoNumber(dominoNumber);

                getTile(tile.getX(), tile.getY() + 1).getRect().setFill(tile2Image);
                getTile(tile.getX(), tile.getY() + 1).setDominoNumber(dominoNumber);
                return true;
            }
            else if (getTile(tile.getX(), tile.getY() + 1)!= null && getTile(tile.getX(), tile.getY() + 1).getRect().getFill().equals(Color.WHEAT)) {
                if (((tile.getX() >= minX && tile.getX() <= maxX)
                        && (tile.getY() >= minY && tile.getY() < maxY)) ) {
                    String tilezone1 = getTileZone(dominoNumber, 1);
                    String tilezone2 = getTileZone(dominoNumber, 2);
                            
                    if (checkNeighbor(tile, dominoNumber, tilezone1)
                            || checkNeighbor(getTile(tile.getX(), tile.getY() + 1), dominoNumber, tilezone2)) {
                        ImagePattern tile1Image = getImage(dominoNumber, 1);
                        ImagePattern tile2Image = getImage(dominoNumber, 2);
                        tile.getRect().setFill(tile1Image);
                        tile.setDominoNumber(dominoNumber);
                        tile.setNumTile(1);

                        getTile(tile.getX(), tile.getY() + 1).getRect().setFill(tile2Image);
                        getTile(tile.getX(), tile.getY() + 1).setDominoNumber(dominoNumber);
                        getTile(tile.getX(), tile.getY() + 1).setNumTile(2);

                        if( maxX()-minX() <= 4 && maxY()-minY() <= 4)
                        {
                            return true;
                        }
                        else{
                            tile.getRect().setFill(Color.WHEAT);
                            getTile(tile.getX(), tile.getY() + 1).getRect().setFill(Color.WHEAT);
                        }
                    }
                }
            }
        }
        else if (orientation.equalsIgnoreCase("horizontal 1<-2")) {
            if (getTile(tile.getX(), tile.getY() - 1)!=null && getTile(tile.getX(), tile.getY() - 1).getRect().getFill().equals(Color.WHEAT)) {
                if (((tile.getX() >= minX && tile.getX() <= maxX)
                        && (tile.getY() > minY && tile.getY() <= maxY))) {

                    String tilezone1 = getTileZone(dominoNumber, 1);
                    String tilezone2 = getTileZone(dominoNumber, 2);

                    if (checkNeighbor(tile, dominoNumber, tilezone1)
                            || checkNeighbor(getTile(tile.getX(), tile.getY() - 1), dominoNumber, tilezone2)) {
                        ImagePattern tile1Image = getImage(dominoNumber, 1);
                        ImagePattern tile2Image = getImage(dominoNumber, 2);
                        tile.getRect().setFill(tile1Image);
                        tile.setDominoNumber(dominoNumber);
                        tile.setNumTile(1);

                        getTile(tile.getX(), tile.getY() - 1).getRect().setFill(tile2Image);
                        getTile(tile.getX(), tile.getY() - 1).setDominoNumber(dominoNumber);
                        getTile(tile.getX(), tile.getY() - 1).setNumTile(2);
                        if( maxX()-minX() <= 4 && maxY()-minY() <= 4)
                        {
                            return true;
                        }
                        else{
                            tile.getRect().setFill(Color.WHEAT);
                            getTile(tile.getX(), tile.getY() - 1).getRect().setFill(Color.WHEAT);
                        }
                    }
                }
            }
        }
        else if (orientation.equalsIgnoreCase("vertical 1->2")) {
            if (getTile(tile.getX() + 1, tile.getY())!=null && getTile(tile.getX() + 1, tile.getY()).getRect().getFill().equals(Color.WHEAT)) {
                if (((tile.getX() >= minX && tile.getX() < maxX)
                        && (tile.getY() >= minY && tile.getY() <= maxY))) {

                    String tilezone1 = getTileZone(dominoNumber, 1);
                    String tilezone2 = getTileZone(dominoNumber, 2);
                    if (checkNeighbor(tile, dominoNumber, tilezone1)
                            || checkNeighbor(getTile(tile.getX() + 1, tile.getY()), dominoNumber, tilezone2)) {
                        ImagePattern tile1Image = getImage(dominoNumber, 1);
                        ImagePattern tile2Image = getImage(dominoNumber, 2);
                        tile.getRect().setFill(tile1Image);
                        tile.setDominoNumber(dominoNumber);
                        tile.setNumTile(1);

                        getTile(tile.getX() + 1, tile.getY()).getRect().setFill(tile2Image);
                        getTile(tile.getX() + 1, tile.getY()).setDominoNumber(dominoNumber);
                        getTile(tile.getX() + 1, tile.getY()).setNumTile(2);
                        if( maxX()-minX() <= 4 && maxY()-minY() <= 4)
                        {
                            return true;
                        }
                        else{
                            tile.getRect().setFill(Color.WHEAT);
                            getTile(tile.getX() + 1, tile.getY()).getRect().setFill(Color.WHEAT);
                        }
                    }
                }
            }
        }
        else if (orientation.equalsIgnoreCase("vertical 1<-2")) {
            if (getTile(tile.getX() - 1, tile.getY())!=null && getTile(tile.getX() - 1, tile.getY()).getRect().getFill().equals(Color.WHEAT)) {
                if (((tile.getX() > minX && tile.getX() <= maxX)
                        && (tile.getY() >= minY && tile.getY() <= maxY))) {

                    String tilezone1 = getTileZone(dominoNumber, 1);
                    String tilezone2 = getTileZone(dominoNumber, 2);
                    if (checkNeighbor(tile, dominoNumber, tilezone1)
                            || checkNeighbor(getTile(tile.getX() - 1, tile.getY()), dominoNumber, tilezone2)) {
                        ImagePattern tile1Image = getImage(dominoNumber, 1);
                        ImagePattern tile2Image = getImage(dominoNumber, 2);
                        tile.getRect().setFill(tile1Image);
                        tile.setDominoNumber(dominoNumber);
                        tile.setNumTile(1);

                        getTile(tile.getX() - 1, tile.getY()).getRect().setFill(tile2Image);
                        getTile(tile.getX() - 1, tile.getY()).setDominoNumber(dominoNumber);
                        getTile(tile.getX() - 1, tile.getY()).setNumTile(2);
                        if( maxX()-minX() <= 4 && maxY()-minY() <= 4)
                        {
                            return true;
                        }
                        else{
                            tile.getRect().setFill(Color.WHEAT);
                            getTile(tile.getX()-1, tile.getY()).getRect().setFill(Color.WHEAT);
                        }
                    }
                }
            }
        }
        return false;
    }

    public ImagePattern getImage(int dominoNumber, int tileNumber) {
        Image img;
        try {
            File file = new File("tiles/dominos.txt");
            Scanner ip = new Scanner(file);
            while(ip.hasNextLine()) {
                String line = ip.nextLine();
                String[] parts = line.split(",");
                if(Integer.parseInt(parts[0]) == dominoNumber) {
                    img = new Image("file:tiles/" + parts[tileNumber] + ".png");
                    return new ImagePattern(img);
                }
            }
            ip.close();
        }
        catch(FileNotFoundException e) {
            System.out.println(e);
        }
        return null;
    }

    public void addSelectedDomino(int dominoNumber) {
        selectedDominos.add(selectedDominos.size(),  dominoNumber);
    }

    public void setdStage(Stage stage) {
        this.dStage = stage;
    }

    public void resetSelectedDomino(){
        selectedDominos.clear();
    }

    public String getSelectedDomino() {
        try {
            File file = new File("tiles/dominos.txt");
            Scanner ip = new Scanner(file);
            while(ip.hasNextLine()) {
                String line = ip.nextLine();
                String[] parts = line.split(",");
                if(selectedDominos.size()> 0 && Integer.parseInt(parts[0]) == selectedDominos.get(0)) {
                    return "Domino: " + line;
                }
            }
        }
        catch(FileNotFoundException e) {
            System.out.println(e);
        }
        return "No domino selected";
    }

    public String getLastSelectedDomino() {
        try {
            File file = new File("tiles/dominos.txt");
            Scanner ip = new Scanner(file);
            while(ip.hasNextLine()) {
                String line = ip.nextLine();
                String[] parts = line.split(",");
                if(Integer.parseInt(parts[0]) == selectedDominos.get(selectedDominos.size() - 1)) {
                    return "Domino: " + line;
                }
            }
        }
        catch(FileNotFoundException e) {
            System.out.println(e);
        }
        return "No domino selected";
    }

    public void selectedDominosRemoveFirst() {
        System.out.println("Removing... " + selectedDominos.get(0));
        selectedDominos.remove(0);
    }

    public Boolean checkNeighbor(Tile tile, int dominoNumber, String tileZone) {
        if( tileZone.charAt(tileZone.length() - 1) == 'c' ) {
            tileZone = tileZone.substring(0, tileZone.length() - 2);
        }
        String up = "OUT";
        if(getTile(tile.getX()-1, tile.getY())!=null){
            up = getTileZone(getTile(tile.getX()-1, tile.getY()).getDominoNumber(), getTile(tile.getX()-1, tile.getY()).getNumTile());
        }
        else if ( tile.getX()-1 == 4 && tile.getY() == 4){
            up = "castle";
        }

        String down = "OUT";
        if(getTile(tile.getX()+1, tile.getY())!=null){
            down = getTileZone(getTile(tile.getX()+1, tile.getY()).getDominoNumber(), getTile(tile.getX()+1, tile.getY()).getNumTile());
        }
        else if( tile.getX()+1 == 4 && tile.getY() == 4) {
            down = "castle";
        }
        
        String left = "OUT";
        if(getTile(tile.getX(), tile.getY()-1)!=null){
            left = getTileZone(getTile(tile.getX(), tile.getY()-1).getDominoNumber(), getTile(tile.getX(), tile.getY()-1).getNumTile());
        }
        else if ( tile.getX() == 4 && tile.getY()-1 == 4){
            left = "castle";
        }
        
        String right = "OUT";
        if(getTile(tile.getX(), tile.getY()+1)!=null){
            right = getTileZone(getTile(tile.getX(), tile.getY()+1).getDominoNumber(), getTile(tile.getX(), tile.getY()+1).getNumTile());
        }
        else if (tile.getX() == 4 && tile.getY()+1 == 4){
            right = "castle";
        }

        if( up.equals(down) && down.equals(right) && right.equals(left)){
            return false;
        }

        if(up.startsWith(tileZone) || down.startsWith(tileZone) || left.startsWith(tileZone) || right.startsWith(tileZone) ||
            up.equals("castle") || down.equals("castle") || left.equals("castle") || right.equals("castle")) {
            return true;
        }
        else {
            return false;
        }
    }

    public String getTileZone(int dominoNumber, int numTile) {
        try {
            if(dominoNumber == -1) {
                return "castle";
            }
            File file = new File("tiles/dominos.txt");
            Scanner ip = new Scanner(file);
            while(ip.hasNextLine()) {
                String line = ip.nextLine();
                String[] parts = line.split(",");
                if(Integer.parseInt(parts[0]) == dominoNumber) {
                    return parts[numTile];
                }
            }
        }
        catch(FileNotFoundException e) {
            System.out.println(e);
        }
        return "";
    }

    public int calculatePoints(){
        int points = 0;
        for (Tile tile : tiles) {
            String tileZone =getTileZone(tile.getDominoNumber(), tile.getNumTile());
            if(tileZone.endsWith("c")){
                points  += Character.getNumericValue(tileZone.charAt(tileZone.length()-2));
            }
        }
        return points;
    }

    public int minX(){
        int min = 100;
        for(int i = 0; i < gameBoard.getRowCount(); i++){
            for(int j = 0; j < gameBoard.getColumnCount(); j++){
                if( !getTile(i, j).getRect().getFill().equals(Color.WHEAT) && i < min){
                    min = i;
                }
            }   
        }
        return min;
    }

    public int maxX(){
        int max = -1;
        for(int i = 0; i < gameBoard.getRowCount(); i++){
            for(int j = 0; j < gameBoard.getColumnCount(); j++){
                if( !getTile(i, j).getRect().getFill().equals(Color.WHEAT) && i > max){
                    max = i;
                }
            }   
        }
        return max;
    }

    public int minY(){
        int min = 100;
        for(int j = 0; j < gameBoard.getColumnCount(); j++){
            for(int i = 0; i < gameBoard.getRowCount(); i++){
                if( !getTile(i, j).getRect().getFill().equals(Color.WHEAT) && j < min){
                    min = j;
                }
            }   
        }
        return min;
    }

    public int maxY(){
        int max = -1;
        for(int j = 0; j < gameBoard.getColumnCount(); j++){
            for(int i = 0; i < gameBoard.getRowCount(); i++){
                if( !getTile(i, j).getRect().getFill().equals(Color.WHEAT) && j > max){
                    max = j;
                }
            }   
        }
        return max;
    }
}
