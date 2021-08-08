package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("audioProgramMain.fxml")));
        primaryStage.setTitle("Audio Compressor");
        primaryStage.setScene(new Scene(root, 600, 300));
        root.getChildrenUnmodifiable().get(3).setDisable(true);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
