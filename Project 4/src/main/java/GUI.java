//import javafx.application.Application;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.input.KeyCode;
//import javafx.scene.input.KeyEvent;
//import javafx.scene.layout.BorderPane;
//import javafx.stage.Stage;
//
//import java.io.IOException;
//
//import static javafx.application.Application.launch;
//
//public class GUI extends Application {
//    Stage stage;
//    Scene scene;
//    Button startStop;
//    BorderPane border;
//
//    Boolean started = false;
//
//    AudioSender sender;
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) throws IOException {
//        sender = new AudioSender();
//        /**
//         * Set the stage
//         */
//        stage = primaryStage;
//        stage.setTitle("AWE, HULYKIT?");
//
//        /**
//         * Initialise the nodes.
//         */
//        startStop = new Button("Start");
//        startStop.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                try {
//                    handleEvent();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        border = new BorderPane();
//
//        /**
//         * Set the layout.
//         */
//        border.setCenter(startStop);
//
//        /**
//         * Set the scene.
//         */
//        scene = new Scene(border, 500, 500);
//        stage.setScene(scene);
//        stage.show();
//    }
//
//    void handleEvent() throws InterruptedException {
//        System.out.println("hey");
//        if (started) {
//            sender.keepSending = false;
//        } else {
//            sender.start();
//            started = true;
//        }
//    }
//}
