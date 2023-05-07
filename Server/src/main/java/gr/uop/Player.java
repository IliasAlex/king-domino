package gr.uop;

public class Player {
    private String name, color;
    private int points;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
        points = -1;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name + " " + color;
    }
}
