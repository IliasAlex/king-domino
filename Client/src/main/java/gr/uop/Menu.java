package gr.uop;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

abstract class Menu {
    private Stage stage;
    private Scene scene;

    public abstract void init();

    public abstract void show();

    public void close() {
        stage.close();
    }

    public Stage getStage() {
        return stage;
    }

    public Scene getScene() {
        return scene;
    }

    public void initStage() {
        stage = new Stage();
    }

    public void initScene(Parent pane, double width, double height) {
        scene = new Scene(pane, width, height);
    }
}
