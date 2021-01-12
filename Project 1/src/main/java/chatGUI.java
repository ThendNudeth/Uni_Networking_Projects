import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;

public class chatGUI implements EventHandler<ActionEvent> {

    Stage stage;
    Scene scene;

    ChatClient cc;

    Button disc;
    Button send;

    TextArea msgField;
    TextField yourMsg;
    Label welcomeLbl;
    Label msgLbl;
    Label clientLbl;

    String whisp_name;
    String name;
    String message_to_send;
    String toDisp;

    BorderPane border;
    HBox inputPane;
    VBox msgPane;
    VBox buttonPane;
    VBox clientPane;
    HBox header;

    ListView clientList;

    public chatGUI(String myName) {
        name = myName;
    }

    public void openWindow() {
        //set the stage
        stage = new Stage();
        stage.setTitle("Chat room");

        cc = initGUI.getCc();

        //Initialise the nodes
        msgField = new TextArea();
        msgField.setEditable(false);
        msgField.setMaxHeight(Double.MAX_VALUE);
        msgField.setMaxWidth(Double.MAX_VALUE);
        msgField.setPrefWidth(300);
        yourMsg = new TextField();
        yourMsg.setMaxWidth(Double.MAX_VALUE);
        yourMsg.setPrefWidth(msgField.getPrefWidth());
        clientList = new ListView();
        clientList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("clicked on " + clientList.getSelectionModel().getSelectedItem());


                if (clientList.getSelectionModel().getSelectedItem()!=null) {
                    whisp_name = clientList.getSelectionModel().getSelectedItem().toString();

                    Alert whispDlg = new Alert(Alert.AlertType.CONFIRMATION);
                    whispDlg.setTitle("Confirm whisper");
                    whispDlg.setHeaderText("Would you like to whisper to "+clientList.getSelectionModel().getSelectedItem()+"?");

                    Optional<ButtonType> result = whispDlg.showAndWait();
                    if (result.get() == ButtonType.OK){
                        // ... user chose OK
                        yourMsg.setText("@"+whisp_name+" ");
                    } else {
                        // ... user chose CANCEL or closed the dialog
                    }
                }
                clientList.getSelectionModel().clearSelection();
            }
        });

        //yourMsg = new TextField();
        yourMsg.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    message_to_send = yourMsg.getText();
                    if(yourMsg.getText().startsWith("WHISPER@")) {
                        message_to_send = cc.myName+" please dont try to imitate someone";
                    }
                    //if this message is a whisper then process here
                    if(yourMsg.getText().startsWith("@")) {
                        String preformat = yourMsg.getText();
                        String toUser = preformat.substring(1, preformat.indexOf(' '));
                        String fromUser = name;
                        String content = preformat.substring((preformat.indexOf(' ')+1));
                        String postformat = "WHISPER@" + toUser+"@"+fromUser+" "+content;
                        message_to_send = postformat;
                        msgField.appendText("(whisper to "+toUser+"): "+content+"\n");
                    }
                    cc.sendMessage(message_to_send);
                    yourMsg.setText("");
                }
            }
        });

        welcomeLbl = new Label("Welcome, " + name);
        msgLbl = new Label("Messages:");
        clientLbl = new Label("Currently in chatroom:");

        send = new Button("Send");
        send.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                message_to_send = yourMsg.getText();
                //if this message is a whisper then process here
                if(yourMsg.getText().startsWith("WHISPER@")) {
                    message_to_send = cc.myName+" please dont try to imitate someone";
                }
                if(yourMsg.getText().startsWith("@")) {
                    String preformat = yourMsg.getText();
                    String toUser = preformat.substring(1, preformat.indexOf(' '));
                    String fromUser = name;
                    String content = preformat.substring((preformat.indexOf(' ')+1));
                    String postformat = "WHISPER@" + toUser+"@"+fromUser+" "+content;
                    message_to_send = postformat;
                    msgField.appendText("(whisper to "+toUser+"): "+content+"\n");
                }
                cc.sendMessage(message_to_send);
                yourMsg.setText("");
            }
        });
        disc = new Button("Disconnect");
        disc.setOnAction(this);

        //Set the layout
        border = new BorderPane();
        inputPane = new HBox();
        inputPane.getChildren().addAll(yourMsg, send);
        msgPane = new VBox();
        msgPane.setVgrow(msgField, Priority.ALWAYS);
        msgPane.setMinWidth(300);
        msgPane.getChildren().addAll(msgLbl, msgField, inputPane);
        buttonPane = new VBox();
        buttonPane.getChildren().addAll(disc);
        clientPane = new VBox();
        clientPane.getChildren().addAll(clientLbl, clientList);
        clientPane.setVgrow(clientList, Priority.ALWAYS);
        header = new HBox();
        header.getChildren().add(welcomeLbl);

        border.setTop(header);
        border.setLeft(buttonPane);
        border.setCenter(clientPane);
        border.setRight(msgPane);

        //Set the scene
        scene = new Scene(border, 500, 400);
        stage.setScene(scene);
        stage.show();

        Thread msgListener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //System.out.println("msgListener starts");
                    ObservableList<String> buffer_list = FXCollections.observableArrayList();
                    buffer_list.removeAll();

                    String recvd_msg = cc.recvMessage();
                    //System.out.println(recvd_msg+"recvmsg");

                    if (recvd_msg.equals("_UPDATE_START")) {
                        //System.out.println("update started");
                        while (!recvd_msg.equals("_END_UPDATE")) {
                            recvd_msg = cc.recvName();
                            if (recvd_msg.startsWith("_UPDATE")) {
                                buffer_list.add(recvd_msg.substring(7));
                            }
                        }
                        //System.out.println("update ended");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                clientList.setItems(buffer_list);
                                buffer_list.addListener(new InvalidationListener() {
                                    @Override
                                    public void invalidated(Observable observable) {

                                    }
                                });
                            }
                        });
                    } else {
                        String finalRecvd_msg = recvd_msg;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                if (finalRecvd_msg.startsWith("MESSAGE")) {
                                    toDisp = "" + finalRecvd_msg;
                                    toDisp = toDisp.substring(8)+"\n";
                                    msgField.appendText(toDisp);
                                }else if(finalRecvd_msg.startsWith("(whisper from")) {
                                    msgField.appendText(finalRecvd_msg);
                                } else if (finalRecvd_msg.startsWith("empty")) {
                                }
                            }
                        });
                    }

                }

            }
        });
        msgListener.start();
    }
    public String getMessageToSend() {
        return message_to_send;
    }
    @Override
    public void handle(ActionEvent event) {

        if (event.getSource()== disc) {
            System.out.println("disc");
            cc.closeSocket();
            stage.close();
        }
    }
}