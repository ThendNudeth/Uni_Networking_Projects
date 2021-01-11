import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.sound.sampled.LineUnavailableException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class InitClientGUI extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Stage stage = primaryStage;
        ClientGUI clientGUI = new ClientGUI();

        Label l1 = new Label("Please insert the Server's IP address:");
        TextField ipTf = new TextField();


        VBox holder = new VBox();
        holder.getChildren().addAll(l1, ipTf);
        BorderPane border = new BorderPane();
        border.setCenter(holder);
        Scene scene = new Scene(border, 300, 50);
        ipTf.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    String input = ipTf.getText();
                    Pattern pattern = Pattern.compile("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b");
                    if (!pattern.matcher(input).matches()) {
                        Alert invalidIP = new Alert(Alert.AlertType.WARNING);
                        invalidIP.setContentText("Not an IP address.");
                        invalidIP.show();
                    } else {
                        try {
                            clientGUI.openWindow(input);
                            stage.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        stage.setScene(scene);
        stage.show();
    }
}

class ClientGUI {
    Stage stage;
    Scene scene;

    VBox clientPane;
    VBox messagePane;
    VBox buttonPane;

    BorderPane border;

    HBox headerPane;
    HBox sendPane;

    Label messHeader;
    Label clientHeader;
    Label header;
    Label vnHeader;
    Label chHeader;

    TextArea messageFeed;
    TextField yourMsg;
    ListView clientList;
    ListView voiceNotes;
    ListView channels;

    Button disconnect;
    Button call;
    Button addToCall;
    Button send;
    Button sendVN;
    Button recordVN;

    byte[] voiceNote;

    ClientVOIP client;

    void openWindow(String ip) throws Exception {
        System.out.println(ip);
        try {
            Thread runClient = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        client = new ClientVOIP(ip);

                        Thread t = new Thread(client);
                        t.start();

                        while (true) {
                            client.sendCmdMessage();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            runClient.start();
            final byte[][] newVN = new byte[1][1];
            VoiceNoteRecorder voiceNoteRecorder = new VoiceNoteRecorder();

            stage = new Stage();
            stage.setTitle("AWEAWE");

            ObservableList<String> list = FXCollections.observableArrayList();

            messHeader = new Label("Messages:");
            clientHeader = new Label("Currently Online:");
            header = new Label("Welcome, <Sannie>");
            vnHeader = new Label("Available voice Notes");
            chHeader = new Label("Current Channels");

            yourMsg = new TextField();
            send = new Button("send");
            send.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    //TODO: Send text message.
                    try {
                        client.writeToSendCmdMessage("text_"+client.myID + "|" + yourMsg.getText() + "&");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            send.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    //TODO: Send text message.
                }
            });

            messageFeed = new TextArea();
            messageFeed.setEditable(false);
            messageFeed.setMaxHeight(Double.MAX_VALUE);
            messageFeed.setPrefWidth(200);

            voiceNotes = new ListView();
            voiceNotes.setPrefHeight(200);
            final VoiceNote[] vn = new VoiceNote[1];
            voiceNotes.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if(voiceNotes.getSelectionModel().getSelectedItem()==null) {
//                        Alert noSelection = new Alert(Alert.AlertType.WARNING);
//                        noSelection.setTitle("Invalid Selection");
//                        noSelection.setHeaderText("You clicked an empty field retard.");
//                        noSelection.setContentText("Can not Select null value.");
//                        noSelection.show();
//                    } else {
                        // TODO: Request the voicenote from the server and play it.
//                        if (voiceNote!=null) {
                            Thread player = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        vn[0] = client.myLastVoiceNote;
                                        System.out.println(vn[0]);
                                        newVN[0] = vn[0].getAudioData();
                                        voiceNoteRecorder.playVoiceNote(newVN[0]);
                                    } catch (LineUnavailableException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            player.start();

//                        }

                    }
                }
            });

            clientList = new ListView();
            clientList.setPrefHeight(200);
//            clientList.getItems().addAll("hey", "Djy");
            clientList.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    /**
                     * Placeholder code:
                     */
                    if(clientList.getSelectionModel().getSelectedItem()==null) {
                        Alert noSelection = new Alert(Alert.AlertType.WARNING);
                        noSelection.setTitle("Invalid Selection");
                        noSelection.setHeaderText("You clicked an empty field retard.");
                        noSelection.setContentText("Can not Select null value.");
                        noSelection.show();
                    } else {
                        if (clientList.getSelectionModel().getSelectedItem().equals("hey")) {
                            list.clear();
                            list.addAll("vn1", "vn2");
                        }
                        if (clientList.getSelectionModel().getSelectedItem().equals("Djy")) {
                            list.clear();
                            list.addAll("vn3", "vn4");
                        }
                        voiceNotes.setItems(list);
                    }


                    /**
                     * What it should actually do:
                     * Continuously check the server for an up-to-date list of active clients.
                     */

                }
            });

            channels = new ListView();
            channels.setPrefHeight(200);
            channels.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {

                }
            });

            recordVN = new Button("Record Voice Note");
            sendVN = new Button("Send Voice Note");
            sendVN.setVisible(false);
            final Boolean[] recording = {false};
            final Boolean[] vnExists = {false};

            FileInputStream input = new FileInputStream("call.png");
            Image image = new Image(input);
            ImageView imageView = new ImageView(image);
            final Boolean[] inCall = {false};
            call =  new Button("Call", imageView);
            call.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {

                    if(clientList.getSelectionModel().getSelectedItem()==null) {
                        Alert noSelection = new Alert(Alert.AlertType.WARNING);
                        noSelection.setTitle("Call failed");
                        noSelection.setHeaderText("Can not make call.");
                        noSelection.setContentText("Recipient not specified.");
                        noSelection.show();
                    } else {
                        if (!inCall[0]) {
                            if (clientList.getSelectionModel().getSelectedItem().equals(client.myID)) {

                                Alert noSelfcall = new Alert(Alert.AlertType.WARNING);
                                noSelfcall.setHeaderText("You can't call yourself");
                                noSelfcall.show();
                            } else {
                                call.setText("End Call");
                                System.out.println("Selected: "+clientList.getSelectionModel().getSelectedItem()+
                                        "Mine: "+client.myID);
                                String hostToCall = (String) clientList.getSelectionModel().getSelectedItem();
                                try {
                                    inCall[0] = true;
                                    addToCall.setVisible(true);
                                    client.processOpenConf("_"+hostToCall+"|");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            //TODO: RequestCall(clientList.getSelectionModel().getSelectedItem())
                        } else {
                            addToCall.setVisible(false);
                            call.setText("Call");
                            try {
                                client.endCall();
                                inCall[0] = false;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //TODO: RequestCallEnd

                        }
                    }
                }
            });
            addToCall = new Button("Add to Call");
            addToCall.setVisible(false);
            addToCall.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (client.hasOpenCall((String) clientList.getSelectionModel().getSelectedItem())) {
                        Alert alreadyInCall = new Alert(Alert.AlertType.WARNING);
                        alreadyInCall.setHeaderText("You are already in a call with this person");
                        alreadyInCall.show();
                    } else {
                        String hostToCall = (String) clientList.getSelectionModel().getSelectedItem();
                        try {
                            inCall[0] = true;
                            addToCall.setVisible(true);
                            client.processOpenConf("_"+hostToCall+"|");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            disconnect = new Button("Disconnect");
            disconnect.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // TODO: Cleanly disconnect all comms.
                    client.disconnect();
                    stage.close();
                }
            });

            sendPane = new HBox();
            sendPane.getChildren().addAll(yourMsg, send);
            headerPane = new HBox();
            headerPane.getChildren().addAll(header);

            clientPane = new VBox();
            clientPane.setVgrow(clientList, Priority.ALWAYS);
            clientPane.getChildren().addAll(clientHeader, clientList, vnHeader, voiceNotes, chHeader, channels);

            messagePane = new VBox();
            messagePane.setVgrow(messageFeed, Priority.ALWAYS);
            messagePane.getChildren().addAll(messHeader, messageFeed, sendPane);

            buttonPane = new VBox();
//        buttonPane.setPrefWidth(200);
            buttonPane.getChildren().addAll(disconnect, call, addToCall, recordVN, sendVN);

            border = new BorderPane();
            border.setTop(headerPane);
            border.setLeft(buttonPane);
            border.setCenter(clientPane);
            border.setRight(messagePane);

            recordVN.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (!recording[0]) {
                        recording[0] = true;
                        sendVN.setVisible(false);
                        recordVN.setText("Stop recording");
                        //Start recording a voiceNote.
                        Thread recorder = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    voiceNote = voiceNoteRecorder.recordVoiceNote();
                                } catch (LineUnavailableException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        recorder.start();

                    } else {
                        recordVN.setText("Record New Voice Note");
                        vnExists[0] = true;
                        recording[0] = false;
                        sendVN.setVisible(true);
                        //Stop recording a voiceNote.
                        voiceNoteRecorder.keepRecording = false;
                    }

                }
            });

            sendVN.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if(vnExists[0]) {
                        //TODO: Request to send the voice note.
                        VoiceNote vnToSend = new VoiceNote(voiceNote, client.myID , (String) clientList.getSelectionModel().getSelectedItem());
                        try {
                            client.uploadVN(vnToSend,(String) clientList.getSelectionModel().getSelectedItem());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Alert noVN = new Alert(Alert.AlertType.WARNING);
                        noVN.setTitle("No Voice Note");
                        noVN.setHeaderText("Can not send Voice Note");
                        noVN.setContentText("You haven't recorded a Voice Note, or already sent it.");
                    }
                }
            });

            scene = new Scene(border, 500, 300);
            stage.setScene(scene);
            stage.show();

            ObservableList bufClientList = FXCollections.observableArrayList();
            ArrayList<String[]> newList = new ArrayList<String[]>();

            Thread changeListener = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
//                        System.out.println(client.hasUpdated);
                        if ((client.hasUpdated+"").equals("true")) {
//                            System.out.println("aweawe");
                            client.hasUpdated = false;
                            newList.clear();
                            for (int i = 0; i < client.activeClients.size(); i++) {
                                newList.add(client.activeClients.get(i));
                            }
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    bufClientList.clear();
                                    for (int i = 0; i < newList.size(); i++) {
                                        bufClientList.add(newList.get(i)[0]);
                                        System.out.println(client.activeClients.get(i)[0]);
                                    }
                                    clientList.setItems(bufClientList);
                                }
                            });
                        }
                        if ((client.msgUpdated+"").equals("true")) {
                            client.msgUpdated = false;
                            Platform.runLater(new Runnable() {
                               @Override
                               public void run() {

                                   messageFeed.setText(client.messageBoard);
                               }
                           });
                        }


                    }
                }
            });
            changeListener.start();

        } catch (ConnectException e) {
            System.out.println("wrong server.");
            Alert badConn = new Alert(Alert.AlertType.WARNING);
            badConn.setHeaderText("Could not connect to server.");
            badConn.show();
        }
    }
}
