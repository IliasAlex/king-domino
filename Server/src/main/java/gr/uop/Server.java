package gr.uop;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * JavaFX App
 */


public class Server extends Application {
    
    ObservableList<String> items = FXCollections.observableArrayList();

    String GAMEISON = "FALSE";
    ArrayList<Integer> dominos = new ArrayList<>();
    ArrayList<Integer> dominoPool = new ArrayList<>();
    

    private static ArrayList<ClientHandler> clients = new ArrayList<>();

    Label serverLabel;
    final static int PORT = 5555;
    final int boardMax_X = 9;
    final int boardMax_Y = 9;

    ArrayList<Player> players = new ArrayList<>();
    ArrayList<Player> playersTurn = new ArrayList<>();

    Boolean isLastRound = false;

    @Override
    public void start(Stage stage) {
        serverLabel = new Label("Server logs");   
        serverLabel.setFont(Font.font("Verdana", 16));
        
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction((b)->{
            items.clear();
        });


        ListView<String> lv = new ListView<>(items);
        lv.setPrefHeight(470);
        VBox vb = new VBox(serverLabel, lv, clearBtn);
        vb.setAlignment(Pos.CENTER);
        vb.setSpacing(5);
        vb.setPadding(new Insets(10));


        var scene = new Scene(vb, 320, 480);
        stage.setScene(scene);
        stage.show();
        
        initServer server = new initServer();
        server.start();
        Platform.runLater(() -> {
            stage.sizeToScene();
            stage.setMinHeight(stage.getHeight());
            stage.setMinWidth(stage.getWidth());
            stage.setMaxHeight(stage.getHeight());
            stage.setMaxWidth(stage.getWidth());
        });

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Exit");
                alert.setContentText("You will stop ongoing game if exists.");
                alert.setHeaderText(null);
                alert.initModality(Modality.WINDOW_MODAL);
                alert.initOwner(stage);

                ButtonType okbtn = new ButtonType("OK", ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(okbtn, ButtonType.CANCEL);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == okbtn) {
                    for (int i = 0; i < clients.size(); i++) {
                        if(clients.get(i).isAlive()){
                            clients.get(i).toClient.println("STOPGAME");
                        }
                    }
                    System.exit(0);
                } else if (result.get() == ButtonType.CANCEL) {
                    we.consume();
                }
                
            }
        });
    }
    

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    private class initServer extends Thread{
        @Override
        public void run(){
            try{
                ServerSocket listener = new ServerSocket(PORT);
                while(true){
                    Socket client = listener.accept();
                    ClientHandler clientThread = new ClientHandler(client, clients);
                    clientThread.start();
                }
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
    }   
    private class ClientHandler extends Thread{
        private Socket client;
        private Scanner fromClient;
        private PrintWriter toClient;
        private ArrayList<ClientHandler> clients;
        
        public ClientHandler(Socket client, ArrayList<ClientHandler> clients) throws IOException{
            this.client = client;
            fromClient = new Scanner(this.client.getInputStream());
            toClient =new PrintWriter(this.client.getOutputStream(), true);
            this.clients = clients;
        }
        
        @Override
        public void run(){
            try{
                while(fromClient.hasNextLine()) {
                    String msg = fromClient.nextLine();
                    System.out.println("Recieved: "+msg);
                    if(msg.equals("LISTENER")) {
                        System.out.println("Adding Listener: ");
                        clients.add(this);
                    }
                    handle(msg);
                }
                fromClient.close();
                toClient.close();
            }
            catch(IOException e){

            }
        }
    
    
        public void handle(String msg) throws IOException{
            if( msg.equals("CONNECT")){
                Platform.runLater(()->{
                    //serverLabel.setText(serverLabel.getText()+"\nPlayer connected");
                    items.add("Player connected");
                });
                toClient.println("OK");
            }
            else if ( msg.equals("PLAYERS")){
                for (Player player : players) {
                    toClient.println(player);
                }
                toClient.println("PLAYERS_END");
            }
            else if (msg.equals("JOIN")) {
                String line = fromClient.nextLine();
                System.out.println("Player: " + line);
                String[] p = line.split(" ");

                for (Player player : players) {
                    if(player.getName().equals(p[0])) {
                        toClient.println("NOT_JOINED");
                        return;
                    }
                }
                
                players.add(new Player(p[0], p[1]));
                Platform.runLater(()->{
                    //serverLabel.setText(serverLabel.getText()+"\nName: '"+ p[0] +"'' color: '"+ p[1]+"'' joined");
                    items.add("Name: '"+ p[0] +"'' color: '"+ p[1]+"'' joined");
                });

                toClient.println("JOINED");
            }
            else if(msg.startsWith("REMOVE_PLAYER ")) {
                String s = msg.substring(14);
                String[] lines = s.split(" ");
                for (Player player : players) {
                    if(player.getName().equals(lines[0])) {
                        toAll("REMOVE_PLAYER " + player);
                        players.remove(player);
                        break;
                    }
                }
            }
            else if( msg.equals("START_GAME")){
                items.add("Game has started.\n==================================");
                GAMEISON = "TRUE";
                int size = 0;
                if(players.size() == 2) {
                    size = 24;
                }
                else if(players.size() == 3) {
                    size = 36;
                }
                else {
                    size = 48;
                }

                for (int i = 1; i <= 48; i++) {
                    dominoPool.add(i);
                }

                Collections.shuffle(dominoPool);
                for (int i = 0; i < size; i++) {
                    dominos.add(dominoPool.get(i));
                }
                System.out.println(dominos);

                Collections.shuffle(players);                
                
                if(players.size() == 2) {
                    players.add(players.get(0));
                    players.add(players.get(1));
                }

                initPlayersTurn();
                toAll(msg);
                toAll("TURN_" +playersTurn.get(0).getName());
            }
            else if( msg.equals("GAMEISON")){
                toClient.println(GAMEISON);
            }
            else if(msg.equals("STOPGAME")){
                toAll(msg);
                GAMEISON = "FALSE";
                players.clear();
                playersTurn.clear();
                dominos.clear();
                dominoPool.clear();
                isLastRound = false;
                toAll("END_LISTENER");

                Platform.runLater(() -> {
                    //serverLabel.setText(serverLabel.getText() + "\nGame has been stopped.\n====================================");
                    items.add("Game has been stopped.\n==================================");
                });
            }
            else if(msg.equals("GET_DOMINOS")) {
                if(Boolean.parseBoolean(GAMEISON) && dominos.size() > 0) {
                    ArrayList<Integer> dominosTemp = new ArrayList<>();
                    for (int i = 0; i < players.size(); i++) {
                        dominosTemp.add(dominos.get(i));
                    }
                    Collections.sort(dominosTemp);
                    for ( Integer d : dominosTemp ) {
                        toClient.println("" + d);
                    }
                    toClient.println("GET_DOMINOS_END");
                }
                else{
                    toClient.println("GET_DOMINOS_END");
                }
            }
            else if(msg.startsWith("SELECTED_DOMINO ")) {
                String s = msg.substring(16);
                String[] parts = s.split(",");
                toAll("DRAW_DOMINO " + parts[0] + " " + parts[3]);
                items.add(playersTurn.get(0).getName() +" selected domino with number: "+ parts[0]);
            }
            else if(msg.startsWith("SELECTED_DOMINO2 ")) {
                String s = msg.substring(17);
                String[] parts = s.split(",");
                toAll("DRAW_DOMINO " + parts[0] + " " + parts[3]);
                items.add(playersTurn.get(0).getName() +" selected domino with number: "+ parts[0]);
                playersTurn.remove(0);
                if(playersTurn.isEmpty()) {
                    initPlayersTurn();
                    for (int i = 0; i < players.size(); i++) {
                        dominos.remove(0);
                    }
                    toAll("DOMINOS_SHIFT_LEFT");

                    toAll("DRAW_DOMINOS2");
                    toAll("ADD_SELECT2_LISTENER");
                }
                toAll("TURN_PLACE " +playersTurn.get(0).getName());
                if( dominos.size() == 4){
                    isLastRound = true;
                }
            }
            else if (msg.equalsIgnoreCase("END_TURN")) {
                playersTurn.remove(0);
                if (playersTurn.isEmpty()) {
                    initPlayersTurn();
                    for (int i = 0; i < players.size(); i++) {
                        dominos.remove(0);
                    }
                    toAll("TURN_PLACE " + playersTurn.get(0).getName());
                    toAll("DRAW_DOMINOS2");
                } else {
                    toAll("TURN_" + playersTurn.get(0).getName());
                }
                if (dominos.size() == 4) {
                    isLastRound = true;
                }
            }
            else if(msg.equalsIgnoreCase("END_TURN_PLACE")) {
                if(dominos.size() == 0){
                    playersTurn.remove(0);
                    if(playersTurn.size() != 0){
                        toAll("TURN_PLACE " + playersTurn.get(0).getName());
                    }
                    else{
                        isLastRound = false;
                        toAll("COUNT_POINTS");
                    }
                }
                else{
                    toAll("TURN_SELECT2 "+playersTurn.get(0).getName());
                    toAll("ADD_SELECT2_LISTENER");
                }
            }
            else if(msg.startsWith("REMOVE_SELECTED_DOMINO ")) {
                toAll(msg);
            }
            else if(msg.startsWith("POINTS ") ){
                String s = msg.substring(7);
                String[] parts = s.split(" ");
                int points = Integer.parseInt(parts[1]);
                String name = parts[0];
                
                for (Player p : players) {
                    if(p.getName().equals(name)){
                        p.setPoints(points);
                        break;
                    }
                }

                int p_counter = 0;
                int max_points = -1;
                String winner="";
                for (Player p : players) {
                    if(p.getPoints() != -1 ){
                        p_counter++;
                    }
                    if( p.getPoints() > max_points){
                        max_points = p.getPoints();
                        winner = p.getName();
                    }
                }
                if(p_counter == players.size()){
                    toAll("WINNER " + winner + " "+ max_points);
                    toAll("STOPGAME");
                    GAMEISON = "FALSE";
                    players.clear();
                    playersTurn.clear();
                    dominos.clear();
                    dominoPool.clear();
                    isLastRound = false;
                    toAll("END_LISTENER");
                    clients.clear();
                }
            }
            else if( msg.equals("toAll")){
                String line = fromClient.nextLine();
                System.out.println("toAll: "+line);
                toAll(line);
            }
            
        }
        public void toAll(String msg){
            for (int i = 0; i < clients.size(); i++) {
                if(clients.get(i).isAlive()){
                    clients.get(i).toClient.println(msg);
                }
            }
        }
    
        public void initPlayersTurn() {
            for (Player pt : players ) {
                playersTurn.add(pt);
            }
        }
    }
}

