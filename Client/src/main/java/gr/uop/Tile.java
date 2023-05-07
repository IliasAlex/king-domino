package gr.uop;

import javafx.scene.shape.Rectangle;

class Tile {
    private Rectangle rect;
    private int x, y, dominoNumber;
    private Boolean isSelected;
    private int numTile;

    public Tile(Rectangle rect, int x, int y){
        this.rect = rect;
        this.x = x;
        this.y = y;
        isSelected = false;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public int getNumTile() {
        return numTile;
    }

    public void setNumTile(int numTile) {
        this.numTile = numTile;
    }

    public Rectangle getRect(){
        return rect;
    }

    public void setRect(Rectangle rect){
        this.rect = rect;
    }

    public void setDominoNumber(int dominoNumber){
        this.dominoNumber = dominoNumber;
    }

    public int getDominoNumber(){
        return dominoNumber;
    }
    
    public void setSelected(Boolean selected){
        isSelected = selected;
    }
    
    public Boolean isSelected(){
        return isSelected;
    }
}
