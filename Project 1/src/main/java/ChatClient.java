import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ChatClient {

    String serverAddress;
    String message;

    String myName;
    Socket socket;

    Scanner in;
    PrintWriter out;

    public ChatClient() {

    }
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String tryConnect() throws IOException{
            String line = in.nextLine();
            //System.out.println(line+"tryConnect");
            String notice = line;
            return notice;
    }
    public String recvName() {
        String name = in.nextLine();
        //System.out.println(name);
        return name;
    }

    public void closeSocket(){
        try{
            out.close();
            in.close();
            socket.close();
        }catch(Exception e){

        }
    }

    public void sendName(String name) {
        out.println(name);
    }

    public String recvMessage() {
        String line = "";
//        if(in.hasNextLine()){
//            line= in.nextLine();
//        }
        try{
            line = in.nextLine();
        }catch (NoSuchElementException e){
            closeSocket();
            System.exit(0);
        }
        myName = initGUI.getName();
        //process whispers
        if(line.startsWith("WHISPER@")){
            String toUser;
            String fromUser;
            toUser = line.substring(8, line.lastIndexOf('@'));
            fromUser = line.substring(line.lastIndexOf('@')+1, line.indexOf(' '));
            String content = line.substring(line.indexOf(' ')+1);
            if(myName.equals(toUser)){
                message = "(whisper from "+fromUser+"):"+ content+"\n";
            }else if(myName.equals(fromUser)) {
                message = "(whisper to "+toUser+"):"+ content+"\n";
            } else {
                return "empty";
            }
        } else {
            message = line;
        }
        return message;
    }

    public void sendMessage(String msg_to_send) {
        out.println(msg_to_send);
    }

    public void run() throws IOException {
        try {
            socket = new Socket(serverAddress, 8000);
            System.out.println("attempting to connect to server.");

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new Scanner(socket.getInputStream());

        } finally {

        }
    }

}


