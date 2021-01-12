import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.DoubleToIntFunction;

public class ClientInitGUI extends Application {
    Stage stage;
    Scene scene;
    Group root;
    Button logIn;
    TextField nameField;
    Label instr;

    HBox bottom;
    HBox top;
    VBox container;

    Client client;
    static String address;

    public static void main(String[] args) {
        address = args[0];
        launch();
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        stage.setTitle("awe");

        Thread runClient = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client = new Client(address);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        runClient.start();

        instr = new Label("Please provide a nickname:");
        nameField = new TextField();
        nameField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    try {
                        checkNickname();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        logIn = new Button("Log In");
        logIn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    checkNickname();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        bottom = new HBox();
        bottom.getChildren().addAll(nameField, logIn);
        top = new HBox();
        top.getChildren().add(instr);
        container = new VBox();
        container.getChildren().addAll(top, bottom);

        root = new Group();

        scene = new Scene(root, 300, 200);

        container.layoutXProperty().bind(scene.widthProperty().subtract(container.prefWidth(-1)+220).divide(2));
        container.layoutYProperty().bind(scene.heightProperty().subtract(container.prefHeight(-1)+80).divide(2));

        root.getChildren().add(container);

        stage.setScene(scene);
        stage.show();
    }

    public void checkNickname() throws Exception {
        Boolean accepted;
        Alert invalidNickname = new Alert(Alert.AlertType.WARNING);
        if (nameField.getText().equals(null)||nameField.getText().equals("")) {
            invalidNickname.setHeaderText("This field can't be empty");
            invalidNickname.show();
        } else {

            accepted = client.submitNickname(nameField.getText());

            if (accepted.equals(false)) {
                invalidNickname.setHeaderText("This username has already been taken," +
                        " please provide a different one.");
                invalidNickname.show();
            } else {
                ClientGUI clientGUI = new ClientGUI();
                clientGUI.openWindow(client);
                stage.close();
            }
        }
    }
}

class ClientGUI {
    Client client;


    Stage stage;
    Scene scene;
    Group root;
    VBox superContainer;
    HBox container;


    VBox clientPane;
    Label welcome;
    Label clientHead;
    ListView clientList;
    Button requestFile;
    Label upHead;
    ProgressBar upBar;
    Label downHead;
    ProgressBar downBar;
    Button pause;


    VBox searchPane;
    Label searchHead;
    ListView resultsList;
    HBox searchIn;
    TextField searchField;
    Button search;


    VBox messagePane;
    Label messageHead;
    TextArea messageArea;
    HBox messageIn;
    TextField messageField;
    Button send;
    ComboBox box;


    VBox filePane;

    TabPane tabPane;

    Tab myFiles;
    VBox localPane;
    Label myFilesHead;
    ListView localFilesList;
    Button refreshLocal;

    Tab allFiles;
    VBox publicPane;
    Label allFilesHead;
    ListView allFilesList;
    Button refreshPublic;

    Label fileHead;
    ListView fileList;

    public void openWindow(Client client) throws Exception {
        this.client = client;

        Thread runClient = new Thread(new Runnable() {
            @Override
            public void run() {
                client.run();
            }
        });
        runClient.start();

        stage = new Stage();
        root = new Group();
        scene = new Scene(root, 900, 200);
        superContainer = new VBox();
        container = new HBox();

        clientPane = new VBox();
        welcome = new Label("Welcome, "+client.myName);
        welcome.setFont(new Font(16));
        clientHead = new Label("List of active clients:");
        clientList = new ListView();
        requestFile = new Button("Request File");
        upHead = new Label("Upload Progress:");
        upBar = new ProgressBar();
        upBar.setProgress(0.0);
        downHead = new Label("Download Progress:");
        downBar = new ProgressBar();
        downBar.setProgress(0.0);
        pause = new Button("Pause");
        pause.setDisable(true);
        clientPane.getChildren().addAll(clientHead, clientList, requestFile, upHead, upBar, downHead, downBar, pause);
        clientPane.setVgrow(clientList, Priority.ALWAYS);

        searchPane = new VBox();
        searchHead = new Label("Your search results:");
        resultsList = new ListView();
        searchIn = new HBox();
        searchField = new TextField();
        search = new Button("search");
        search.setMinWidth(80);
        searchIn.getChildren().addAll(searchField, search);
        searchIn.setHgrow(searchField, Priority.ALWAYS);
        searchPane.getChildren().addAll(searchHead, resultsList, searchIn);
        searchPane.setVgrow(resultsList, Priority.ALWAYS);

        messagePane = new VBox();
        messageHead = new Label("Messsages:");
        messageArea = new TextArea();
        messageIn = new HBox();
        messageField = new TextField();
        send = new Button("Send to");
        send.setMinWidth(80);
        box = new ComboBox();
        box.setMinWidth(80);
        messageIn.getChildren().addAll(messageField, send, box);
        messageIn.setHgrow(messageField, Priority.ALWAYS);
        messagePane.getChildren().addAll(messageHead, messageArea, messageIn);
        messagePane.setVgrow(messageArea, Priority.ALWAYS);

        tabPane = new TabPane();

        localPane = new VBox();
        myFiles = new Tab("My files:", localPane);
        myFilesHead = new Label("Files currently in local folder:");
        localFilesList = new ListView();
        refreshLocal = new Button("Refresh");
        localPane.getChildren().addAll(myFilesHead, localFilesList, refreshLocal);

        publicPane = new VBox();
        allFiles = new Tab("Available Files", publicPane);
        allFilesHead = new Label("Files currently available to download:");
        allFilesList = new ListView();
        refreshPublic = new Button("Refresh");
        publicPane.getChildren().addAll(allFilesHead, allFilesList, refreshPublic);

        tabPane.getTabs().addAll(myFiles, allFiles);

        filePane = new VBox();
        fileHead = new Label("My files:");
        fileList = new ListView();
        filePane.getChildren().addAll(tabPane);
//        filePane.setVgrow(fileList, Priority.ALWAYS);

        container.getChildren().addAll(clientPane, searchPane, messagePane, filePane);
        container.prefHeightProperty().bind(scene.heightProperty());

        superContainer.getChildren().addAll(welcome, container);
        superContainer.prefHeightProperty().bind(scene.heightProperty());

        root.getChildren().add(superContainer);

        clientPane.prefWidthProperty().bind((scene.widthProperty().subtract(messagePane.widthProperty())).divide(3));
        searchPane.prefWidthProperty().bind((scene.widthProperty().subtract(messagePane.widthProperty())).divide(3));
        filePane.prefWidthProperty().bind((scene.widthProperty().subtract(messagePane.widthProperty())).divide(3));
        messagePane.prefWidthProperty().bind(scene.widthProperty().divide(3));
        stage.setScene(scene);
        stage.show();

        /**
         * Action listeners:
         */
        ObservableList bufResultList = FXCollections.observableArrayList();
        send.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (box.getSelectionModel().getSelectedItem().equals("all")) {
                    sendMessage("");
                } else {
                    sendMessage((String) box.getSelectionModel().getSelectedItem());
                }
                messageField.setText("");
            }
        });
        messageField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    if (box.getSelectionModel().getSelectedItem().equals("all")) {
                        sendMessage("");
                    } else {
                        sendMessage((String) box.getSelectionModel().getSelectedItem());
                    }
                    messageField.setText("");
                }
            }
        });
        search.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                bufResultList.clear();
                client.resultWall.clear();
                search();
            }
        });
        requestFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (resultsList.getSelectionModel().isEmpty()) {
                    Alert noSelection = new Alert(Alert.AlertType.WARNING);
                    noSelection.setHeaderText("Please select a file to request");
                    noSelection.show();
                } else {
                    String selected = resultsList.getSelectionModel().getSelectedItem().toString();
                    String[] splitted = selected.split("\\|", 0);

                    String userWithFile = splitted[0];
                    String requestedFile = splitted[1];

                    if(requestedFile.equals("no results found")) {
                        Alert noSelection = new Alert(Alert.AlertType.WARNING);
                        noSelection.setHeaderText("this user had no files matching your search");
                        noSelection.show();
                    } else {
                        System.out.println("requested: "+requestedFile+" from "+userWithFile);

                        try {
                            //client.reqDownload(userWithFile, requestedFile);
                            client.preRequestExchange(userWithFile, requestedFile);
                        } catch (IOException | ClassNotFoundException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        searchField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    bufResultList.clear();
                    client.resultWall.clear();
                    search();
                }
            }
        });

        ObservableList bufLocalFList = FXCollections.observableArrayList();
        refreshLocal.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ArrayList localFList = client.refreshFiles();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < localFList.size(); i++) {
                            bufLocalFList.add(localFList.get(i));
                        }
                        localFilesList.setItems(bufLocalFList);
                    }
                });
            }
        });

        refreshPublic.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {


            }
        });

        pause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (pause.getText().equals("Pause")) {
                    client.basicReceiver.pauseDownload();
                    pause.setText("Resume");
                } else if (pause.getText().equals("Resume")){
                    try {
                        client.basicReceiver.resumeDownload();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(client.basicReceiver.keepDownloading);
                    pause.setText("Pause");
                }


            }
        });

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {

                try {
                    client.disconnectMe();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        /**
         * Change listeners:
         */

        ObservableList bufClientList = FXCollections.observableArrayList();
        ObservableList dropDownList = FXCollections.observableArrayList();

        Thread changeListener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (client.running) {
                    if ((client.clientUpdated + "").equals("true")) {
                        client.clientUpdated = false;

                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                bufClientList.clear();
                                dropDownList.clear();
                                for (int i = 0; i < client.clients.size() ; i++) {
                                    bufClientList.add(client.clients.get(i));
                                    if (!client.clients.get(i).equals(client.myName)) {
                                        dropDownList.add(client.clients.get(i));
                                    }
                                }
                                dropDownList.add("all");
                                clientList.setItems(bufClientList);
                                box.setItems(dropDownList);
                                box.getSelectionModel().select(dropDownList.size()-1);
                            }
                        });
                    }

                    if ((client.messagesUpdated+ "").equals("true")) {
                        client.messagesUpdated = false;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                messageArea.setText(client.messageWall);
                            }
                        });
                    }

                    if ((client.searchResultsUpdated+ "").equals("true")) {
                        client.searchResultsUpdated = false;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                bufResultList.clear();
                                for(int i = 0; i < client.resultWall.size(); i++) {
                                    bufResultList.add(client.resultWall.get(i));
                                }
                                resultsList.setItems(bufResultList);
                            }
                        });
                    }


                }
            }
        });

        Thread progressListener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (client.running) {
                    if ((client.sending+"").equals("true")) {
                        double prog = client.basicSender.currProgress;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        upBar.setProgress(prog);
                                    }
                                });
                                if (prog == 1) {
                                    client.sending = false;

                                }
                            }
                        }).start();

                    }
                    if ((client.receiving+"").equals("true")) {
                        requestFile.setDisable(true);
                        pause.setDisable(false);
                        double prog = client.basicReceiver.currProgress;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        downBar.setProgress(prog);
                                    }
                                });
                                if (prog == 1) {
                                    client.receiving = false;
                                    requestFile.setDisable(false);
                                    pause.setDisable(true);
                                }
                            }
                        }).start();

                    }
                }

            }
        });

        changeListener.start();
        progressListener.start();
    }

    public void sendMessage(String receiver) {
        String message = messageField.getText();
        try {
            client.sendMessage(message, receiver);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void search() {
        //TODO: if empty, search for all files (adjust tolerance)
        if (searchField.getText().equals("")) {
            Alert empty = new Alert(Alert.AlertType.WARNING);
            empty.setHeaderText("Please provide a search term");
            empty.show();
        } else {
            String searchTerm = searchField.getText();
            try {
                client.searchFor(searchTerm);
            }catch (IOException e) {

            }
        }

    }
}
