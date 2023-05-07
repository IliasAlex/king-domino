package gr.uop;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * JavaFX App
 */
public class Client extends Application {

    private final int MAX_PLAYERS = 4;

    private String server_name = "";
    private final int PORT = 5555;

    private final int boardMax_X = 9;
    private final int boardMax_Y = 9;
    
    private ObservableList<Player> players = FXCollections.observableArrayList(); //ονόματα των παικτών που παίζουν 
    
    private ListView<Player> pList = new ListView<>(players);

    private Button gameBtn, helpBtn, help, about, helpBack, aboutBack, joinBtn, stopBtn, quitBtn, checkServerBtn, join,
            startGame, selectDominoBtn, placeDominoBtn, showDominos;
    private TextField servertf;
    private Label helpLabel, waitingLabel;
    private ComboBox<String> cb;
    private String playerColor;

    private Menu sMenu, gMenu, wMenu;
    private joinServerMenu serMenu;

    private boardMenu bMenu;

    private Player me;


    @Override
    public void start(Stage stage) {
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                System.exit(0);
            }
        });

        sMenu = new startMenu(stage);
        sMenu.init();
        sMenu.show();

        gameBtn.setOnAction((e) -> {

            gMenu = new gameMenu(stage);
            gMenu.init();
            gMenu.show();

            stopBtn.setOnAction((stop) -> {
                toServer("STOPGAME");
            });

            quitBtn.setOnAction((quit) -> {
                gMenu.close();
            });

            joinBtn.setOnAction((f) -> {

                serMenu = new joinServerMenu(gMenu.getStage());
                serMenu.init();
                serMenu.show();

                servertf.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue,
                            String newValue) {
                        servertf.setStyle("-fx-text-inner-color: black;");
                    }
                });
                checkServerBtn.setOnAction((g) -> {
                    bMenu = null;
                    if (checkServerStatus(servertf.getText())) {

                        server_name = servertf.getText();
                        servertf.setEditable(false);
                        servertf.setStyle("-fx-text-inner-color: green;");
                        checkServerBtn.setDisable(true);
                        serMenu.getUsernameLabel().setVisible(true);
                        serMenu.getUsername().setVisible(true);
                        serMenu.getCb().setVisible(true);
                        pList.setVisible(true);
                        join.setVisible(true);

                        initPlayers();

                        try {
                            ServerListener sListener = new ServerListener(server_name);
                            new Thread(sListener).start();
                        } catch (IOException er) {
                            System.out.println(er);
                        }

                        if (gameIsOn()) {
                            gameStartedAlert();
                        }

                        join.setOnAction((j) -> {
                            wMenu = new waitingMenu(serMenu.getStage());
                            
                            if (cb.getValue() == null) {
                                cb.setValue(cb.getItems().get(0));
                            }
                            if(addPlayer(serMenu.getUsername().getText(), serMenu.getCb().getValue())) {
                                wMenu.init();
                                
                                wMenu.show();
                                wMenu.getStage().setOnCloseRequest(new EventHandler<WindowEvent>() {
                                    public void handle(WindowEvent we) {
                                        Alert alert = new Alert(AlertType.CONFIRMATION);
                                        alert.setTitle("Leaving queue");
                                        alert.setContentText("Do you want to leave this queue?");
                                        alert.setHeaderText(null);
                                        alert.initModality(Modality.WINDOW_MODAL);
                                        alert.initOwner(wMenu.getStage());
                                        Optional<ButtonType> result = alert.showAndWait();
                                        if(result.get() == ButtonType.OK) {
                                            toServer("REMOVE_PLAYER " + me.getName());
                                            serMenu.close();
                                        }
                                        else if(result.get() == ButtonType.CANCEL) {
                                            we.consume();
                                        }

                                    }
                                });


                                if (players.size() <= 3) {
                                    if (players.size() == 1) {
                                        startGame.setDisable(true);
                                    }
                                    startGame.setOnAction((startG) -> {
                                        toServer("START_GAME");
                                    });
                                } else if (players.size() == MAX_PLAYERS) {
                                    toServer("START_GAME");
                                }
                            }
                        });
                    }
                });
            });
        });

        helpBtn.setOnAction((e) -> {
            helpMenu hMenu = new helpMenu(stage);
            hMenu.init();
            hMenu.show();

            help.setOnAction((f) -> {
                File file = new File("kingdomino.pdf");
                HostServices hostServices = getHostServices();
                hostServices.showDocument(file.getPath());
                helpLabel.setText("Just be better!");
            });

            helpBack.setOnAction((b) -> {
                hMenu.close();
            });

            about.setOnAction((g) -> {
                aboutMenu aMenu = new aboutMenu(hMenu.getStage());
                aMenu.init();
                aMenu.show();

                aboutBack.setOnAction((ab) -> {
                    helpLabel.setText("");
                    aMenu.close();
                });
                aMenu.getStage().setOnCloseRequest(new EventHandler<WindowEvent>() {
                    public void handle(WindowEvent we) {
                        helpLabel.setText("");
                    }
                });
            });
        });
    }

    public Boolean checkServerStatus(String server) {
        try (Socket socket = new Socket(server, PORT);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                Scanner fromServer = new Scanner(socket.getInputStream())) {

            toServer.println("CONNECT");
            String msg = fromServer.nextLine();
            if (msg.equals("OK")) {
                socket.close();
                return true;
            }

        } catch (UnknownHostException er) {
            System.out.println("Unknown host");
            servertf.setStyle("-fx-text-inner-color: red;");
        } catch (IOException er) {
            System.out.println(er);
            servertf.setStyle("-fx-text-inner-color: red;");
        }
        return false;
    }

    public void initPlayers() {
        try (Socket socket = new Socket(server_name, PORT);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                Scanner fromServer = new Scanner(socket.getInputStream())) {

            ArrayList<Player> temp_players = new ArrayList<>();

            toServer.println("PLAYERS");
            String line;
            
            while (fromServer.hasNextLine()) {
                line = fromServer.nextLine();
                System.out.println("Recieved: "+ line) ;
                if (line.equals("PLAYERS_END")) {
                    break;
                }
                String[] p = line.split(" ");
                temp_players.add(new Player(p[0], p[1]));

            }
            Platform.runLater(() -> {
                for (int i = players.size(); i < temp_players.size(); i++) {
                    players.add(temp_players.get(i));
                    String color = temp_players.get(i).getColor();
                    cb.getItems().remove(color);
                }
            });
            socket.close();

        } catch (UnknownHostException er) {
            System.out.println("Unknown host");

        } catch (IOException er) {
            System.out.println(er);
        }
    }

    public Boolean addPlayer(String name, String color) {
        try (Socket socket = new Socket(server_name, PORT);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                Scanner fromServer = new Scanner(socket.getInputStream())) {

            toServer.println("JOIN");
            toServer.println(name + " " + color);

            if (fromServer.nextLine().equals("JOINED")) {
                me = new Player(name, color);
                playerColor = color;
                players.add(new Player(name, color));
                pList.refresh();

                if (players.size() >= 4) {
                    join.setDisable(true);
                } else {
                    join.setDisable(false);
                }

            }
            else {
                socket.close();
                return false;
            }
            socket.close();

            Socket s = new Socket(server_name, PORT);
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);
            to.println("toAll");
            to.println("JOINING");
            s.close();
            return true;
        } catch (UnknownHostException er) {
            System.out.println(er);
        } catch (IOException er) {
            System.out.println(er);
        }
        return false;
    }

    public Boolean gameIsOn() {
        try (Socket socket = new Socket(server_name, PORT);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                Scanner fromServer = new Scanner(socket.getInputStream())) {

            toServer.println("GAMEISON");
            if (fromServer.nextLine().equals("TRUE")) {
                return true;
            }
            socket.close();

        } catch (UnknownHostException er) {
            System.out.println(er);
        } catch (IOException er) {
            System.out.println(er);
        }
        return false;
    }

    public void gameStartedAlert() {
        join.setDisable(true);
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game has started");
        alert.setContentText("You can't join");
        alert.setHeaderText("Game has started. Too slow :(");
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(serMenu.getStage());
        alert.showAndWait();
        serMenu.close();
    }

    public void toServer(String msg){
        try (Socket socket = new Socket(server_name, PORT);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                Scanner fromServer = new Scanner(socket.getInputStream())) {

            toServer.println(msg);
            socket.close();

        } catch (UnknownHostException er) {
            System.out.println(er);
        } catch (IOException er) {
            System.out.println(er);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private class ServerListener implements Runnable {
        private Socket server;
        private Scanner from;
        private PrintWriter to;

        public ServerListener(String server) throws IOException {
            this.server = new Socket(server, PORT);
            from = new Scanner(this.server.getInputStream());
            to = new PrintWriter(this.server.getOutputStream(), true);
            to.println("LISTENER");
        }

        @Override
        public void run() {
            
            while (from.hasNextLine()) {
                String msg = from.nextLine();
                if (msg.equals("END_LISTENER")) {
                    break;
                }
                handle(msg);
            }
            from.close();
            to.close();
        }

        public void handle(String msg) {
            if (msg.equals("JOINING")) {
                initPlayers();
                Platform.runLater(() -> {
                    if (waitingLabel != null) {
                        waitingLabel.setText("Players " + players.size() + "/" + MAX_PLAYERS);
                        if (players.size() > 1) {
                            startGame.setDisable(false);
                        }
                        if (players.size() == MAX_PLAYERS) {
                            join.setDisable(true);
                        }
                    }
                });
            }
            else if (msg.equals("START_GAME")) {
                Platform.runLater(() -> {
                    if (wMenu != null && bMenu == null && wMenu.getStage().isShowing()) {
                        wMenu.close();
                        wMenu = null;
                        serMenu.close();
                        bMenu = new boardMenu(sMenu.getStage());
                        bMenu.init(playerColor);
                        bMenu.show();
                        bMenu.showDominos();

                        bMenu.getStage().setOnCloseRequest(new EventHandler<WindowEvent>() {
                            public void handle(WindowEvent we) {
                                Alert alert = new Alert(AlertType.CONFIRMATION);
                                alert.setTitle("Stop Game");
                                alert.setContentText("You will stop the game.");
                                alert.setHeaderText(null);
                                alert.initModality(Modality.WINDOW_MODAL);
                                alert.initOwner(bMenu.getStage());

                                ButtonType okbtn = new ButtonType("OK", ButtonData.OK_DONE);
                                alert.getButtonTypes().setAll(okbtn, ButtonType.CANCEL);

                                Optional<ButtonType> result = alert.showAndWait();
                                if (result.get() == okbtn) {
                                    toServer("STOPGAME");
                                }
                                else if (result.get() == ButtonType.CANCEL) {
                                    we.consume();
                                }
                            }
                        });

                        joinBtn.setDisable(true);
                        stopBtn.setDisable(false);
                        quitBtn.setDisable(true);
                    }
                    else if (bMenu == null){
                        gameStartedAlert();
                    }
                });
            }
            else if(msg.startsWith("REMOVE_PLAYER")) {
                Platform.runLater(() -> {
                    String s = msg.substring(14);
                    String[] lines = s.split(" ");
                    for (Player player : players) {
                        if(player.getName().equals(lines[0])) {
                            players.remove(player);
                            cb.getItems().add(lines[1]);
                            break;
                        }
                    }
                });
            }
            else if (msg.equals("STOPGAME")) {
                Platform.runLater(() -> {
                    if (bMenu != null) {
                        bMenu.close();
                        joinBtn.setDisable(false);
                        stopBtn.setDisable(true);
                        quitBtn.setDisable(false);
                    } else {
                        join.setDisable(false);
                    }
                    
                });
                players.clear();
            }
            else if (msg.startsWith("TURN_")) {
                Platform.runLater(() -> {
                    bMenu.playersLabel.setText("");
                    for (Player player : players) {
                        bMenu.playersLabel.setText(bMenu.playersLabel.getText()+"\n"+player);
                    }
                    if(msg.startsWith("TURN_PLACE ")) {
                        String name = msg.substring(11);
                        if (name.equalsIgnoreCase(me.getName())) {
                            selectDominoBtn.setDisable(true);
                            placeDominoBtn.setDisable(false);
                            bMenu.setTurnIndicator("Your turn.\nPlease place your selected domino.\n" + bMenu.selectedDominoLabel.getText());
                        } else {
                            selectDominoBtn.setDisable(true);
                            placeDominoBtn.setDisable(true);
                            bMenu.setTurnIndicator("Wait. " + name + "'s" + " turn.");
                        }
                        
                    }
                    else if(msg.startsWith("TURN_SELECT2 ")) {
                        String name = msg.substring(13);
                        if (name.equalsIgnoreCase(me.getName())) {
                            selectDominoBtn.setDisable(false);
                            placeDominoBtn.setDisable(true);
                            bMenu.setTurnIndicator("Your turn. Please select a domino.\n" + bMenu.selectedDominoLabel.getText());
                        } else {
                            selectDominoBtn.setDisable(true);
                            placeDominoBtn.setDisable(true);
                            bMenu.setTurnIndicator("Wait. " + name + "'s" + " turn.");
                        }
                    }
                    else {
                        String name = msg.substring(5);
                        if (name.equalsIgnoreCase(me.getName())) {
                            selectDominoBtn.setDisable(false);
                            placeDominoBtn.setDisable(true);
                            bMenu.setTurnIndicator("Your turn. Please a select a domino.");
                        } else {
                            selectDominoBtn.setDisable(true);
                            placeDominoBtn.setDisable(true);
                            bMenu.setTurnIndicator("Wait. " + name + "'s" + " turn.");
                        }
                    }
                });
            }
            else if (msg.startsWith("DRAW_DOMINO ")) {
                String s= msg.substring(12);
                String[] parts = s.split(" ");
                bMenu.highlightDomino(Integer.parseInt(parts[0]), Color.web(parts[1]));
            }
            else if (msg.equals("DRAW_DOMINOS2")) {
                Platform.runLater (() -> {
                    ArrayList<Integer> dominos = bMenu.getDominos();
                    for (int i = 0; i < dominos.size(); i++) {
                        System.out.println("~DRAW DOMINOS2: " + dominos.get(i));
                        bMenu.getdBoard().drawDomino(bMenu.getdBoard().getTile(i * 2, 4), dominos.get(i), "Horizontal 1->2");
                        bMenu.getdBoard().drawNumber(i*2, 6, dominos.get(i));
                    }
                });
            }
            else if(msg.startsWith("PLACE ")) {
                Platform.runLater(() -> {
                    String str = msg.substring(6);
                    if(str.equals(me.getName())) {
                        selectDominoBtn.setDisable(true);
                        placeDominoBtn.setDisable(false);
                    }
                    else {
                        selectDominoBtn.setDisable(true);
                        placeDominoBtn.setDisable(true);
                    }
                });
                
            }
            else if(msg.equalsIgnoreCase("ADD_SELECT2_LISTENER")) {
                bMenu.addSelectListener();
            }
            else if (msg.startsWith("REMOVE_SELECTED_DOMINO ")) {
                String selected = msg.substring(23);
                ArrayList<Tile> tiles = bMenu.getdBoard().getTiles();
                for (Tile tile : tiles) {
                    if(tile.getDominoNumber() == Integer.parseInt(selected)) {
                        tile.getRect().setFill(Color.WHEAT);
                        tile.getRect().setStroke(Color.WHITE);
                        if( bMenu.getdBoard().getTile(tile.getX(), tile.getY()+1).getRect().getStroke().equals(Color.WHITE)){
                            bMenu.getdBoard().removeNumber(tile.getX(), tile.getY()+1);
                        }
                    }
                }
            }
            else if(msg.equals("DOMINOS_SHIFT_LEFT")) {
                int size = players.size();
                if(players.size() == 2){
                    size = 4;
                }
                
                for (int i = 0; i < size*2-1; i += 2) {
                    bMenu.getdBoard().removeNumber(i, 6);
                }

                Platform.runLater(() -> {
                    int s = players.size();
                    if(players.size() == 2){
                        s = 4;
                    }
                    for (int i = 0; i < s*2-1; i += 2) {
                        bMenu.getdBoard().drawNumber(i, 2, bMenu.getdBoard().getTile(i, 5).getDominoNumber());
                    }
                    
                });
                    for (int i = 0; i < size*2-1; i += 2) {
                        bMenu.getdBoard().getTile(i,0).getRect().setStroke(bMenu.getdBoard().getTile(i, 4).getRect().getStroke());
                        bMenu.getdBoard().getTile(i,0).getRect().setStrokeWidth(3);
                        
                        bMenu.getdBoard().getTile(i,1).getRect().setStroke(bMenu.getdBoard().getTile(i, 4).getRect().getStroke());
                        bMenu.getdBoard().getTile(i,1).getRect().setStrokeWidth(3);


                        bMenu.getdBoard().drawDomino(bMenu.getdBoard().getTile(i,0), bMenu.getdBoard().getTile(i, 4).getDominoNumber(), "horizontal 1->2");
                        

                        bMenu.getdBoard().getTile(i,4).getRect().setFill(Color.WHEAT);
                        bMenu.getdBoard().getTile(i,4).getRect().setStroke(Color.WHITE);
                        bMenu.getdBoard().getTile(i,4).setSelected(false);

                        bMenu.getdBoard().getTile(i,5).getRect().setFill(Color.WHEAT);
                        bMenu.getdBoard().getTile(i,5).getRect().setStroke(Color.WHITE);
                        bMenu.getdBoard().getTile(i,5).setSelected(false);

                        
                    }
            }
            else if(msg.equals("COUNT_POINTS")){
                toServer("POINTS " + me.getName() + " " + bMenu.board.calculatePoints());
            }
            else if(msg.startsWith("WINNER ")){
                String s = msg.substring(7);
                String[] parts = s.split(" ");
                String winner = parts[0];
                int winner_points = Integer.parseInt(parts[1]);
                
                Platform.runLater(()->{
                    Stage st = new Stage();
                    StackPane sp = new StackPane(new Label("Winner: " + winner + "\nPoints: " + winner_points));
                    st.setScene(new Scene(sp, 250, 200));
                    st.initModality(Modality.WINDOW_MODAL);
                    st.initOwner(gMenu.getStage());
                    st.setX(gMenu.getStage().getX() + gMenu.getStage().getWidth() / 8);
                    st.setY(gMenu.getStage().getY() + gMenu.getStage().getHeight() / 8);
                    st.show();
                    st.setResizable(false);
                });
            }
        }
    }

    private class startMenu extends Menu {
        private Stage stage;

        public startMenu(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void init() {
            gameBtn = new Button("Game");
            helpBtn = new Button("Help");

            VBox mainPane = new VBox(gameBtn, helpBtn);
            mainPane.setAlignment(Pos.CENTER);
            gameBtn.setMaxWidth(90);
            helpBtn.setMaxWidth(90);

            super.initScene(new StackPane(mainPane), 320, 160);
            stage.setScene(super.getScene());
        }

        @Override
        public void show() {
            stage.show();
            Platform.runLater(() -> {
                stage.sizeToScene();
                stage.setMinHeight(stage.getHeight());
                stage.setMinWidth(stage.getWidth());
                stage.setMaxHeight(stage.getHeight());
                stage.setMaxWidth(stage.getWidth());
            });
        }

        @Override
        public Stage getStage() {
            return stage;
        }
    }

    private class helpMenu extends Menu {
        Stage pStage;

        public helpMenu(Stage pStage) {
            this.pStage = pStage;
        }

        public void init() {
            help = new Button("Help");
            about = new Button("About Kingdomino");
            helpBack = new Button("Home");
            helpBack.setMaxWidth(117);
            help.setMaxWidth(117);
            helpLabel = new Label();
            VBox helpPane = new VBox(help, about, helpBack, helpLabel);
            helpPane.setAlignment(Pos.CENTER);
            helpPane.setSpacing(5);
            super.initScene(helpPane, 330, 250);
        }

        public void show() {
            super.initStage();
            super.getStage().setTitle("Help");
            super.getStage().setScene(super.getScene());
            super.getStage().initModality(Modality.WINDOW_MODAL);
            super.getStage().initOwner(pStage);
            super.getStage().show();
            super.getStage().setX(pStage.getX() + pStage.getWidth() / 8);
            super.getStage().setY(pStage.getY() + pStage.getHeight() / 8);
            super.getStage().setResizable(false);
        }
    }

    private class aboutMenu extends Menu {
        private Stage pStage;

        public aboutMenu(Stage pStage) {
            this.pStage = pStage;
        }

        public void init() {
            Label label = new Label(
                    "Vins Rantses              dit19196@go.uop.gr\nIlias Alexandropoulos     dit19007@go.uop.gr");
            aboutBack = new Button("Go back");
            VBox aboutPane = new VBox(label, aboutBack);
            aboutPane.setAlignment(Pos.CENTER);
            aboutPane.setSpacing(10);

            super.initScene(aboutPane, 330, 250);
        }

        public void show() {
            super.initStage();
            super.getStage().setTitle("About");
            super.getStage().setScene(super.getScene());
            super.getStage().initModality(Modality.WINDOW_MODAL);
            super.getStage().initOwner(pStage);
            super.getStage().show();
            super.getStage().setX(pStage.getX() + pStage.getWidth() / 8);
            super.getStage().setY(pStage.getY() + pStage.getHeight() / 8);
            super.getStage().setResizable(false);
        }
    }

    private class gameMenu extends Menu {
        private Stage pStage;

        public gameMenu(Stage pStage) {
            this.pStage = pStage;
        }

        @Override
        public void init() {
            joinBtn = new Button("Join Game");
            stopBtn = new Button("Stop Game");
            quitBtn = new Button("Quit");
            joinBtn.setMaxWidth(90);
            stopBtn.setMaxWidth(90);
            quitBtn.setMaxWidth(90);
            VBox gamePane = new VBox(joinBtn, stopBtn, quitBtn);
            gamePane.setAlignment(Pos.CENTER);
            gamePane.setSpacing(5);
            stopBtn.setDisable(true);

            super.initScene(gamePane, 330, 250);
        }

        @Override
        public void show() {
            super.initStage();
            super.getStage().setTitle("Game");
            super.getStage().setScene(super.getScene());
            super.getStage().initModality(Modality.WINDOW_MODAL);
            super.getStage().initOwner(pStage);
            super.getStage().show();
            super.getStage().setX(pStage.getX() + pStage.getWidth() / 8);
            super.getStage().setY(pStage.getY() + pStage.getHeight() / 8);
            super.getStage().setResizable(false);
        }
    }

    private class joinServerMenu extends Menu {
        private Stage pStage;
        private Label serverLabel, usernameLabel;
        private TextField username;

        public joinServerMenu(Stage pStage) {
            this.pStage = pStage;
        }

        public void init() {
            serverLabel = new Label("Server name:");
            servertf = new TextField();
            checkServerBtn = new Button("Check");
            HBox hb1 = new HBox(serverLabel, servertf, checkServerBtn);
            hb1.setSpacing(5);
            hb1.setAlignment(Pos.CENTER);

            usernameLabel = new Label("Username:  ");
            usernameLabel.setVisible(false);
            username = new TextField();
            username.setVisible(false);
            Button dummy = new Button("Check");
            dummy.setVisible(false);
            HBox hb2 = new HBox(usernameLabel, username, dummy);
            hb2.setAlignment(Pos.CENTER);

            cb = new ComboBox<>();
            cb.setPromptText("Colors");
            cb.getItems().addAll("Pink", "Yellow", "Blue", "Green");
            cb.setVisible(false);

            pList.setVisible(false);
            pList.setMaxSize(250, 250);

            join = new Button("Join");
            join.setVisible(false);

            VBox vb = new VBox(hb1, hb2, cb, pList, join);
            vb.setAlignment(Pos.CENTER);
            vb.setSpacing(10);
            vb.setPadding(new Insets(0, 0, 10, 0));
            super.initScene(vb, 460, 370);
        }

        public void show() {
            super.initStage();
            super.getStage().setTitle("Join");
            super.getStage().setScene(super.getScene());
            super.getStage().initModality(Modality.WINDOW_MODAL);
            super.getStage().initOwner(pStage);
            super.getStage().show();
            super.getStage().setX(pStage.getX() + pStage.getWidth() / 8);
            super.getStage().setY(pStage.getY() + pStage.getHeight() / 8);
            super.getStage().setResizable(false);
        }

        public Label getServerLabel() {
            return serverLabel;
        }

        public Label getUsernameLabel() {
            return usernameLabel;
        }

        public TextField getUsername() {
            return username;
        }

        public ComboBox<String> getCb() {
            return cb;
        }

    }

    private class waitingMenu extends Menu {
        Stage pStage;

        public waitingMenu(Stage pStage) {
            this.pStage = pStage;
        }

        public void init() {
            waitingLabel = new Label("Players " + players.size() + "/4");
            startGame = new Button("Start Game");
            VBox vb = new VBox(waitingLabel, startGame);
            vb.setAlignment(Pos.CENTER);
            vb.setSpacing(10);
            vb.setPadding(new Insets(0, 0, 10, 0));
            super.initScene(vb, 320, 200);
        }

        public void show() {
            super.initStage();
            super.getStage().setTitle("Waiting");
            super.getStage().setScene(super.getScene());
            super.getStage().initModality(Modality.WINDOW_MODAL);
            super.getStage().initOwner(pStage);
            super.getStage().show();
            super.getStage().setX(pStage.getX() + pStage.getWidth() / 8);
            super.getStage().setY(pStage.getY() + pStage.getHeight() / 8);
            super.getStage().setResizable(false);
        }
    }

    private class boardMenu extends Menu {
        private Stage pStage;
        private Board board;
        private Stage dStage;
        private Label infoLabel;
        private Label pointsLabel;
        private Label selectedDominoLabel;
        private Label turnIndicator;
        private Board dBoard;
        private int selectedDomino = -1;
        private ComboBox<String> orientation;
        private ArrayList<Integer> dominos;
        private Label playersLabel;
        private Button cantPlace;

        public boardMenu(Stage pStage) {
            this.pStage = pStage;
        }

        public void init() {}

        public void init(String color) {
            
            infoLabel = new Label("Click tile to place selected domino");
            selectedDominoLabel = new Label("No domino selected");

            orientation = new ComboBox<>();
            orientation.setPromptText("Orientation");
            orientation.getItems().setAll("Horizontal 1->2", "Horizontal 1<-2", "Vertical 1->2", "Vertical 1<-2");

            pointsLabel = new Label("Points: 0");

            showDominos = new Button("Show Dominos");
            cantPlace = new Button("Can't Place");
            cantPlace.setDisable(true);
            
            VBox rightPane = new VBox(infoLabel, selectedDominoLabel, pointsLabel, orientation, showDominos, cantPlace);
            rightPane.setAlignment(Pos.CENTER);
            rightPane.setPadding(new Insets(0, 0, 30, 0));
            rightPane.setSpacing(15);

            GridPane boardPane = new GridPane();
            boardPane.setPadding(new Insets(10, 0, 0, 50));
            board = new Board(boardPane, boardMax_X, boardMax_Y);
            board.drawCastle(color);

            HBox mainPain = new HBox(boardPane, rightPane);
            mainPain.setSpacing(50);
            super.initScene(mainPain, 800, 500);
            
            cantPlace.setOnAction(cP->{
                cantPlace.setDisable(true);
                String[] parts = selectedDominoLabel.getText().substring(8).split(",");
                dBoard.selectedDominosRemoveFirst();
                toServer("REMOVE_SELECTED_DOMINO " + Integer.parseInt(parts[0]));
                toServer("END_TURN_PLACE");
                dStage.show();
                selectedDominoLabel.setText(dBoard.getSelectedDomino());
            });
        }

        public void show() {
            super.initStage();
            super.getStage().setTitle("Board");
            super.getStage().setScene(super.getScene());
            super.getStage().initModality(Modality.WINDOW_MODAL);
            super.getStage().initOwner(pStage);
            super.getStage().show();
            super.getStage().setX(pStage.getX() - pStage.getWidth() / 2);
            super.getStage().setY(pStage.getY() + pStage.getHeight() / 5);
            super.getStage().setResizable(false);
        }

        public Board getBoard() {
            return board;
        }
        
        public Board getdBoard() {
            return dBoard;
        }

        public ArrayList<Integer> getDominos() {
            ArrayList<Integer> dominos = new ArrayList<>();
            try (Socket socket = new Socket(server_name, PORT);
                    PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                    Scanner fromServer = new Scanner(socket.getInputStream())) {

                toServer.println("GET_DOMINOS");
                while(fromServer.hasNextLine()) {
                    String line = fromServer.nextLine();
                    if(line.equals("GET_DOMINOS_END")) {
                        break;
                    }
                    dominos.add(Integer.parseInt(line));
                }
                socket.close();
                return dominos;
            } catch (UnknownHostException er) {
                System.out.println(er);
            } catch (IOException er) {
                System.out.println(er);
            }
            return dominos;
        }

        public void showDominos() {
            dStage = new Stage();
            dominos = new ArrayList<>();
            dominos = getDominos();
            GridPane gPane = new GridPane();
            turnIndicator = new Label("");
            turnIndicator.setAlignment(Pos.CENTER);

            playersLabel = new Label("");

            VBox vbLabel = new VBox(playersLabel, turnIndicator);
            vbLabel.setAlignment(Pos.CENTER);
            vbLabel.setSpacing(10);
            HBox hb = new HBox(gPane, vbLabel);
            hb.setSpacing(10);
            gPane.setPadding(new Insets(0, 0, 0, 31));
            dBoard = new Board(gPane, 7, 7);

            for (int i = 0; i < dominos.size(); i++) {
                dBoard.drawDomino(dBoard.getTile(i * 2, 0), dominos.get(i), "Horizontal 1->2");
                dBoard.drawNumber(i*2, 2, dominos.get(i));
                addSelectListener();
            }

            selectDominoBtn = new Button("Select");
            placeDominoBtn = new Button("Place");

            selectDominoBtn.setDisable(true);
            selectDominoBtn.setOnAction((sl) -> {
                if( selectedDomino != -1){
                    dBoard.addSelectedDomino(selectedDomino);
                }
                Platform.runLater(() -> {
                    String s = dBoard.getSelectedDomino();
                    if( s.startsWith("Domino: ")) {
                        selectedDomino = -1;
                        selectedDominoLabel.setText(s);
                        
                        s = dBoard.getLastSelectedDomino();
                        s = s.substring(8);

                        String[] dNumber = s.split(",");

                        int tempSelectedDomino = Integer.parseInt(dNumber[0]);

                        for (Tile t : dBoard.getTiles()) {
                            if(t.getDominoNumber() == tempSelectedDomino && !t.isSelected()) {
                                if(t.getY() <= 1) {
                                    toServer("SELECTED_DOMINO " + s + "," + me.getColor());
                                    t.setSelected(true);
                                    break;
                                }
                                else if(t.getY() >= 4) {
                                    toServer("SELECTED_DOMINO2 " + s + "," + me.getColor());
                                    t.setSelected(true);
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        selectedDominoLabel.setText(s);
                    }
                });
            });

            showDominos.setOnAction((sd) -> {
                dStage.show();
                orientation.setDisable(false);
            });

            placeDominoBtn.setOnAction(e -> {
                dStage.hide();
                cantPlace.setDisable(false);
                orientation.valueProperty().addListener(new ChangeListener<String>() {
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        if(newValue != null) {
                            board.setdStage(dStage);
                            String[] parts = dBoard.getSelectedDomino().substring(8).split(",");
                            addTileListener(board, Integer.parseInt(parts[0]), "place-" + newValue);
                        }
                    }
                });
            });

            dStage.setTitle("Dominos");
            VBox vb = new VBox(hb, selectDominoBtn, placeDominoBtn);
            vb.setSpacing(10);
            vb.setPadding(new Insets(10, 0, 10, 0));
            vb.setAlignment(Pos.CENTER);
            Scene scene = new Scene(vb, 650, 460);
            dStage.setScene(scene);
            dStage.initModality(Modality.WINDOW_MODAL);
            dStage.initOwner(bMenu.getStage());
            dStage.show();
            dStage.setX(bMenu.getStage().getX() + 2 * bMenu.getStage().getWidth() / 3);
            dStage.setY(bMenu.getStage().getY() + bMenu.getStage().getHeight() / 8);
            dStage.setResizable(false);

            dStage.setOnCloseRequest(e->{
                orientation.setDisable(true);
            });
        }

        public void highlightDomino(int dominoNumber, Color c) {
            for (int i = 0; i <= 6 ; i++) {
                for (int j = 0; j <= 6 ; j++) {
                    if(dBoard.getTile(i, j).getDominoNumber() == dominoNumber) {
                        dBoard.getTile(i, j).getRect().setStroke(c);
                        dBoard.getTile(i, j).setSelected(true);
                    }
                }
            }
        }


        public void closeDominos() {
            dStage.close();
        }

        public void setTurnIndicator(String s) {
            turnIndicator.setText(s);
        }

        public void toServer(String s) {
            try (Socket socket = new Socket(server_name, PORT);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                Scanner fromServer = new Scanner(socket.getInputStream())) {
            if(s.startsWith("SELECTED_DOMINO ")) {
                toServer.println(s);
                toServer.println("END_TURN");
                System.out.println("END TURN~");
            }
            else {
                toServer.println(s);
            }
            socket.close();

            } catch (UnknownHostException er) {
                System.out.println(er);
            } catch (IOException er) {
                System.out.println(er);
            }
        }

        public void addTileListener(Board b, int dominoNumber, String mode) {
            if (mode.startsWith("select-")) {
                Color color = Color.web(mode.substring(7));
                for (Tile tile : b.getTiles()) {
                    if (!tile.getRect().getFill().getClass().equals(Color.WHEAT.getClass())) {
                        tile.getRect().setOnMouseClicked(e -> {
                            if (tile.getRect().getStroke().equals(Color.WHITE) && !selectDominoBtn.isDisable() && !tile.isSelected()) {
                                selectedDomino = tile.getDominoNumber();
                                tile.getRect().setStroke(color);
                                if (tile.getY() == 1) {
                                    b.getTile(tile.getX(), tile.getY() - 1).getRect().setStroke(color);
                                } 
                                else if (tile.getY() == 0) {
                                    b.getTile(tile.getX(), tile.getY() + 1).getRect().setStroke(color);
                                }
                                else if (tile.getY() == 4) {
                                    b.getTile(tile.getX(), tile.getY() + 1).getRect().setStroke(color); 
                                }
                                else if (tile.getY() == 5) {
                                    b.getTile(tile.getX(), tile.getY() - 1).getRect().setStroke(color);
                                }
                                for (Tile tile2 : b.getTiles()) {
                                    if (tile2.getX() != tile.getX() && !tile2.isSelected()) {
                                        tile2.getRect().setStroke(Color.WHITE);
                                    }
                                }
                            }
                        });
                    }
                }
            } else if (mode.startsWith("place-")) {
                String orientationstr = mode.substring(6);
                if (orientationstr.toUpperCase().startsWith("HORIZONTAL") || orientationstr.toUpperCase().startsWith("VERTICAL")) {
                    for (Tile tile : b.getTiles()) {
                        if (tile.getRect().getFill().equals(Color.WHEAT)) {
                            tile.getRect().setOnMouseClicked(e -> {
                                if (tile.getRect().getFill().equals(Color.WHEAT) && orientation.getValue() != null) {
                                    Boolean drew = b.drawDomino(tile, dominoNumber, orientationstr);
                                    if (drew) {
                                        cantPlace.setDisable(true);
                                        pointsLabel.setText("Points: "+ board.calculatePoints());
                                        String[] parts = dBoard.getSelectedDomino().substring(8).split(",");
                                        selectedDomino = Integer.parseInt(parts[0]);
                                        selectedDominoLabel.setText(dBoard.getSelectedDomino());
                                        orientation.getSelectionModel().clearSelection();
                                        dBoard.selectedDominosRemoveFirst();
                                        toServer("REMOVE_SELECTED_DOMINO " + dominoNumber);
                                        dStage.show();
                                        toServer("END_TURN_PLACE");
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }

        public void addSelectListener () {
            for (int i = 0; i < dominos.size(); i++) {
                addTileListener(dBoard, dominos.get(i), "select-" + me.getColor());
            }
        }
    }
}