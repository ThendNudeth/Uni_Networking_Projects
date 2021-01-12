import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;

public class initGUI extends Application implements EventHandler<ActionEvent> {

    Stage stage;
    Scene scene;

    static String myName;
    String notice;
    static String server_add;

    Button connect;
    Button cancel;

    TextField name_in;
    TextField ip_in;
    Label lbln;
    Label lblip;

    static boolean flag = false;

    BorderPane border;
    VBox namePane;
    VBox ipPane;

    int name_code;
    Alert alert;
    static ChatClient cc;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Set the stage
        stage = primaryStage;
        stage.setTitle("Well, not even last night's storm could wake you.");

        cc = new ChatClient();


        //Initialise the nodes
        connect = new Button("Connect");
        connect.setOnAction(this);
        cancel = new Button("Cancel");
        cancel.setOnAction(this);

        name_in = new TextField();
        name_in.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            //TODO: Sanitization required.
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    handleEvent();
                }
            }
        });
        ip_in = new TextField();
        lbln = new Label("Choose a nickname:");
        lblip = new Label("Please provide an ip address:");

        border = new BorderPane();
        namePane = new VBox();
        ipPane = new VBox();

        //Set the layout
        namePane.getChildren().addAll(lbln, name_in, connect);
        ipPane.getChildren().addAll(lblip, ip_in, cancel);
        border.setLeft(namePane);
        border.setRight(ipPane);
        //Set the scene
        scene = new Scene(border, 440, 90);
        stage.setScene(scene);
        stage.show();

        Thread nameListener = new Thread(new Runnable() {

            public void run() {
                //notice = "";
                while (true) {
                    if ((flag+"").equals("true")) {
                        try {
                            notice = cc.tryConnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (notice.startsWith("INVALID_NAME")) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    alert = new Alert(Alert.AlertType.WARNING);
                                    alert.setTitle("Invalid name");
                                    alert.setHeaderText("This name has already been taken.");
                                    alert.showAndWait();
                                }
                            });

                        } else if (notice.startsWith("NAME_ACCEPTED")) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    chatGUI cg = new chatGUI(myName);
                                    cg.openWindow();
                                    stage.close();
                                }
                            });
                            System.out.println("connect");
                            break;
                        }
                    }

                }
            }
        });
        nameListener.start();
    }

    public static String getName() {
        return myName;
    }

    public static ChatClient getCc() {
        return cc;
    }


    @Override
    public void handle(ActionEvent event) {
        //TODO: Sanitization required.
        if (event.getSource()== connect) {
            handleEvent();
        }
        if (event.getSource()== cancel) {
            System.out.println("cancel");
            stage.close();
        }
    }

    private void handleEvent() {
        myName = name_in.getText();
        server_add = ip_in.getText();
        if (myName.isEmpty() || myName == null) {
            alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid name");
            alert.setHeaderText("Your name cannot be empty.");
            alert.showAndWait();
        } else if (server_add.isEmpty() || server_add == null) {
            alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Server Address");
            alert.setHeaderText("You ommitted the Server address.");
            alert.showAndWait();
        }else {
            cc.setServerAddress(server_add);
            try {
                cc.run();

            } catch (IOException e) {
                e.printStackTrace();
            }
            cc.sendName(myName);
            flag = true;
        }
    }
}
