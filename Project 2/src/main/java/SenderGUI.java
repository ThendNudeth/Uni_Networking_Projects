import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;


public class SenderGUI extends Application implements EventHandler<ActionEvent> {
    Stage stage;
    Scene scene;
    BorderPane borderPane;
    VBox fieldPane;
    HBox radioPane;

    Button startSend;
    Button selectFile;
    RadioButton tcpRb;
    RadioButton udpRb;
    ToggleGroup group;

    TextField address_in;
    TextField directory;
    Label address_lbl;
    Label selectLbl;
    Label methodLbl;
    FileChooser fileChooser;
    File selectedFile;


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = new Stage();
        stage.setTitle("Help me Obi-Wan Kenobi, you're my only hope.");
        fieldPane = new VBox();
        radioPane = new HBox();
        borderPane = new BorderPane();
        address_in = new TextField("127.0.0.1");
        directory = new TextField();
        address_lbl = new Label("IP Address to send to:");
        selectLbl = new Label("Please select a file:");
        methodLbl = new Label("Please choose method of data transfer:");
        fileChooser = new FileChooser();


        startSend = new Button("Send");
        startSend.setOnAction(this);
        selectFile = new Button("Select file");
        selectFile.setOnAction(this);
        tcpRb = new RadioButton("tcp");
        udpRb = new RadioButton("rBudp");
        group = new ToggleGroup();
        tcpRb.setToggleGroup(group);
        udpRb.setToggleGroup(group);

        radioPane.getChildren().addAll(tcpRb, udpRb);
        fieldPane.getChildren().addAll(address_lbl, address_in, selectLbl, selectFile, directory, methodLbl, radioPane,  startSend);

        borderPane.setCenter(fieldPane);

        scene = new Scene(borderPane, 400, 200);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handle(ActionEvent event) {
        if (event.getSource()==startSend) {

                //TODO: Do a barrel roll.
                if (directory.getText().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("No File selected.");
                    alert.setHeaderText("No file selected to send.");
                    alert.setContentText("Please use the the button that says \n" +
                            "\"Select File\" to select a file to send.");

                    alert.showAndWait();
                } else {
                    if (udpRb.isSelected()) {
                        Task task = new Task<Void>() {
                            @Override
                            public Void call() throws Exception {
                                SenderThread senderThread = new SenderThread();
                                senderThread.setAddress(address_in.getText());
                                senderThread.setFile(selectedFile);
                                senderThread.start();
                                System.out.println("udp done!");
                                return null;
                            }
                        };
                        new Thread(task).start();
                    } else if (tcpRb.isSelected()) {
                        Task task = new Task() {
                            @Override
                            protected Object call() throws Exception {
                                TCPSender tcpSender = new TCPSender();
                                tcpSender.setFile(selectedFile);
                                tcpSender.run();
                                System.out.println("tcp done!");
                                return null;
                            }
                        };
                        new Thread(task).start();
                    }

                }


        }
        if (event.getSource()==selectFile) {
            selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                directory.setText(selectedFile.getAbsolutePath());
            }

        }
    }
}
