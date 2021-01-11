import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class ReceiverGUI extends Application implements EventHandler<ActionEvent> {
    Stage stage;
    Scene scene;
    BorderPane borderPane;
    VBox fieldPane;
    HBox radioPane;

    Button startListening;
    RadioButton tcpRb;
    RadioButton udpRb;
    ToggleGroup group;

    TextField address_in;
    TextField fname_field;
    Label address_lbl;
    Label selectLbl;
    Label methodLbl;
    ProgressBar bar;

    double currNumPacketsReceived;
    double totalNumPackets;


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
        fname_field = new TextField();
        address_lbl = new Label("IP Address to receive from:");
        selectLbl = new Label("Save as:");
        methodLbl = new Label("Please choose method of data transfer:");

        startListening = new Button("Start Listening");
        startListening.setOnAction(this);
        tcpRb = new RadioButton("tcp");
        udpRb = new RadioButton("rBudp");
        group = new ToggleGroup();
        tcpRb.setToggleGroup(group);
        udpRb.setToggleGroup(group);

        radioPane.getChildren().addAll(tcpRb, udpRb);
        fieldPane.getChildren().addAll(address_lbl, address_in, selectLbl, fname_field, methodLbl, radioPane, startListening);

        bar = new ProgressBar(0.0);
        fieldPane.getChildren().add(bar);

        borderPane.setCenter(fieldPane);


        scene = new Scene(borderPane, 400, 200);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handle(ActionEvent event) {
        if (event.getSource()== startListening) {
            if (udpRb.isSelected()) {

                String filename = fname_field.getText();
                Receiver receiver = null;
                try {
                    receiver = new Receiver(filename, address_in.getText());
                    totalNumPackets = receiver.getTotalNumPackets();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Receiver finalReceiver = receiver;
                Task taskNew = new Task<Void>() {
                    @Override public Void call() {
                        finalReceiver.start();

                        boolean flag = false;
                        System.out.println("starting to send packets");
                        long startTime = System.nanoTime();
                        while(!flag){
                            flag = finalReceiver.recvPackets();
                            currNumPacketsReceived = finalReceiver.getNumPacketsReceived();
                            new Thread(new Runnable() {
                                @Override public void run() {
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            bar.setProgress(currNumPacketsReceived / totalNumPackets);
                                        }
                                    });
                                }
                            }).start();
                        }
                        System.out.println("done receiving packets!");
                        finalReceiver.afterRecvPackets();
                        bar.setProgress(100);
                        long endTime = System.nanoTime();
                        long takenTime = endTime - startTime;
                        System.out.println("UDP elapsed (millisecond)time from send to write: "+(endTime-startTime)/1000000);
                        double recvdPackets = finalReceiver.getTotalNumPackets();
                        System.out.println("transfer rate: "+recvdPackets/((double)takenTime)+" bytes/millisecs");

                        try {
                            finalReceiver.out.close();
                            finalReceiver.in.close();
                            finalReceiver.tcpSocket.close();
                            finalReceiver.socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                new Thread(taskNew).start();

            } else if (tcpRb.isSelected()) {
                String filename = fname_field.getText();
                TCPReceiver tcpReceiver = null;
                try {
                    tcpReceiver = new TCPReceiver(address_in.getText());
                    tcpReceiver.setFileName(filename);
                    tcpReceiver.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No File selected.");
                alert.setHeaderText("No file selected to send.");
                alert.setContentText("Please use the the button that says \n" +
                        "\"Select File\" to select a file to send.");

                alert.showAndWait();
            }

        }
    }
}
