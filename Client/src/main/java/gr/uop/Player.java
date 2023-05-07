package gr.uop;

public class Player {
    private String name, color;
    private Boolean turn;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
        turn = false;
    }

    @Override
    public String toString() {
        return name + " " + color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public void setTurn(Boolean turn){
        this.turn = turn;
    }

    public Boolean getTurn(){
        return turn;
    }
}
