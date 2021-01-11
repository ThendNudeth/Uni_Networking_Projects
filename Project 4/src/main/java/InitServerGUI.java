import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;

public class InitServerGUI extends Application {
    Stage stage;
    Scene scene;
    Button startServ;
    Label message;
    HBox messagePane;
    BorderPane border;

    static String ip;

    public static void main(String[] args) {
        ip = args[0];
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        System.out.println("AWEAWE");
        stage.setTitle("Servererer");

        startServ = new Button("Start Server");
        startServ.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // TODO: Tie in backend.
                ServerGUI sg = new ServerGUI();
                try {
                    System.out.println(ip);
                    sg.OpenWindow(ip);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stage.close();


            }
        });
        message = new Label("Would you like to setup a VOIP server?");
        message.setFont(new Font(20));
        message.setWrapText(true);
        messagePane = new HBox();
//        messagePane.setStyle("-fx-background-color: #336699;");
        messagePane.getChildren().add(message);
        border = new BorderPane();
        border.setStyle("-fx-background-color: #127899;");
        border.setCenter(startServ);
        border.setTop(messagePane);

        scene = new Scene(border, 350, 150);
        stage.setScene(scene);
        stage.show();
    }
}

class ServerGUI {
    Stage stage;
    Scene scene;

    VBox clientPane;
    VBox activityPane;
    BorderPane border;

    HBox headerPane;

    Label actHeader;
    Label clientHeader;
    TextArea activityFeed;
    ListView clientList;

    ServerVOIP server;

    ServerGUI() {

    }

    void OpenWindow(String ip) throws IOException {
        Thread runServer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new ServerVOIP(ip);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    server.handleConnections(server);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        runServer.start();

        stage = new Stage();
        stage.setTitle("Server Information");
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                server.discServer();
                stage.close();
            }
        });

        actHeader = new Label("Server activity:");
        clientHeader = new Label("Clients connected:");
        activityFeed = new TextArea();
        activityFeed.setEditable(false);
        activityFeed.setMaxHeight(Double.MAX_VALUE);
        activityFeed.setPrefWidth(350);
        clientList = new ListView();

        clientPane = new VBox();
        clientPane.setVgrow(clientList, Priority.ALWAYS);
        clientPane.getChildren().addAll(clientHeader, clientList);
        activityPane = new VBox();
        activityPane.setVgrow(activityFeed, Priority.ALWAYS);
        activityPane.getChildren().addAll(actHeader, activityFeed);
        border = new BorderPane();
        border.setLeft(activityPane);
        border.setRight(clientPane);

        scene = new Scene(border, 500, 300);
        stage.setScene(scene);
        stage.show();

        ObservableList bufClientList = FXCollections.observableArrayList();
        ObservableList statusMessages = FXCollections.observableArrayList();
        statusMessages.add("");
        final String[] tempStr = new String[1];
        ArrayList<String[]> newList = new ArrayList<String[]>();
        Thread changeListener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if ((server.hasUpdated+"").equals("true")) {
                        System.out.println("aweawe");
                        server.hasUpdated = false;
                        newList.clear();
                        for (int i = 0; i < server.activeClients.size(); i++) {
                            newList.add(server.activeClients.get(i));
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                bufClientList.clear();
                                for (int i = 0; i < newList.size(); i++) {
                                    bufClientList.add(newList.get(i)[0]);
                                }
                                clientList.setItems(bufClientList);

                            }
                        });
                    }
                    if (statusMessages.get(0).equals(server.currentStatusMessages)) {

                    } else {
                        tempStr[0] = server.currentStatusMessages;
                        statusMessages.clear();
                        statusMessages.add(tempStr[0]);
                        activityFeed.setText((String) statusMessages.get(0));
                    }
                }
            }
        });
        changeListener.start();

    }
}