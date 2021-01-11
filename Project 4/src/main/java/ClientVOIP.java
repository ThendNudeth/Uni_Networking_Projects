import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;


public class ClientVOIP extends Thread{
    Scanner scanner;
    Socket socket;
    InetAddress serverAddress;
    static String myIP;
    static String myID;
    ObjectOutputStream out;
    ObjectInputStream in;
    static boolean hasUpdated = false;
    static boolean msgUpdated = false;
    String messageBoard = "";
    ArrayList<String[]> activeClients;
    ArrayList<String> openCallsToHosts;
    static boolean selectClientStatusReturned = false;
    VeryBasicMultiClient newVBMC;
    boolean stopFlag = false;
    static VoiceNote myLastVoiceNote;

    public ClientVOIP(String serverIP) throws IOException {
        activeClients = new ArrayList<>();
        openCallsToHosts = new ArrayList<>();

        setupSocketsAndStreams(serverIP);
        //uploadVN();
        //recvVnFromServer();

    }

    public void endCall() throws IOException {
        //newVBMC.endCall();
        newVBMC.sender.mSocket.disconnect();
        newVBMC.receiver.mSocket.disconnect();
        newVBMC.sender.mSocket.close();
        newVBMC.receiver.mSocket.close();
//        newVBMC.t0.interrupt();
//        newVBMC.t1.interrupt();
        newVBMC.sender = null;
        newVBMC.receiver = null;
    }

    public void setupSocketsAndStreams(String serverIP) throws IOException {
        serverAddress = InetAddress.getByName(serverIP);
        socket = new Socket(serverAddress, 8000);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        out.flush();

        scanner = new Scanner(System.in);
        System.out.println("connected to server");
    }

    public void uploadVN(VoiceNote voicenote, String toHost) throws IOException {
        voicenote.fromHost = myID;
        voicenote.toHost = toHost;

        out.writeObject(voicenote);
        out.flush();
    }




    public void sendCmdMessage() throws IOException {
        String toSendMessage = scanner.nextLine();
        if(toSendMessage.startsWith("quit")){
            writeToSendCmdMessage("quit");
            disconnect();
            System.exit(0);

        } else if(toSendMessage.startsWith("opencomms_")) {
            processOpenComms(toSendMessage);

        }else if(toSendMessage.startsWith("openconf_")) {
            processOpenConf(toSendMessage);

        } else if(toSendMessage.startsWith("text_")) {
            writeToSendCmdMessage(toSendMessage);
        }
    }


    public void writeToSendCmdMessage(String toSendMessage) throws IOException {
        byte[] send = toSendMessage.getBytes();
        out.writeObject(toSendMessage);
        out.flush();
    }

    public void processOpenComms(String toSendMessage) throws IOException {
        String hostNum = toSendMessage.substring(toSendMessage.indexOf("_")+1, toSendMessage.indexOf("|"));
        String fromClientIP = getmyIP();
        //activeClients.add(new String[]{hostNum, fromClientIP});

        System.out.println("select hostNum: "+hostNum);
        System.out.println("fromClientIP : "+fromClientIP);

        selectClientAndReqOpenComms(hostNum, fromClientIP);
        System.out.println("waiting for status");

        while ((selectClientStatusReturned+"").equals("false")) {

        }
        System.out.println("I have outgoing");

        selectClientStatusReturned = false;

    }

    public void processOpenConf(String toSendMessage) throws IOException {
        String hostNum = toSendMessage.substring(toSendMessage.indexOf("_")+1, toSendMessage.indexOf("|"));
        String fromClientIP = getmyIP();
        //activeClients.add(new String[]{hostNum, fromClientIP});

        System.out.println("select hostNum: "+hostNum);
        System.out.println("fromClientIP : "+fromClientIP);

        selectClientAndReqOpenConference(hostNum, fromClientIP);
        System.out.println("waiting for status");

        while ((selectClientStatusReturned+"").equals("false")) {

        }
        System.out.println("I have outgoing");

        selectClientStatusReturned = false;

    }

    public void selectClientAndReqOpenComms(String hostNumber, String fromClientPort) throws IOException {
        out.writeObject(("opencomms_"+ hostNumber+"|"+fromClientPort+"&"));
        out.flush();
    }

    public void selectClientAndReqOpenConference(String hostNumber, String fromClientPort) throws IOException {
        out.writeObject(("openconf_"+ hostNumber+"|"+fromClientPort+"&"));
        out.flush();
    }

    public void recvdCmdMessage() throws IOException, LineUnavailableException, InterruptedException, ClassNotFoundException {
        Object obj = in.readObject();
        Object obj1 = obj;
        String possibleMsg = obj.toString();
        VoiceNote possibleVN = null;
        try {
            possibleVN = (VoiceNote) obj1;
        } catch (ClassCastException e) {

        }


        classifyRecvdCmdMessage(possibleMsg, possibleVN);
    }

    public void classifyRecvdCmdMessage(String recvd, VoiceNote voiceNote) throws IOException, LineUnavailableException, InterruptedException {
        System.out.println("recvd: "+recvd);
        if (recvd.startsWith("incoming_")) {
            processIncomingReq(recvd);

        } else if (recvd.startsWith("returnIP_")) {
            processIPReturn(recvd);

        }else if (recvd.startsWith("openconf_")) {
            System.out.println("myID testing here: "+myID);
            processConf(recvd);
        }
        else if (recvd.startsWith("returnconf_")) {
            processIPReturnConf(recvd);

        }else if (recvd.startsWith("active")) {
            System.out.println("Selected client is active");
            selectClientStatusReturned = true;

        } else if (recvd.startsWith("inactive")) {
            System.out.println("Selected client is inactive");
            selectClientStatusReturned = true;

        } else if (recvd.startsWith("first_")) {
            myIP = returnFromClientIP(recvd);
            myID = returnHostNum(recvd);
            System.out.println("myIP from server: "+ myIP);
            System.out.println("myID from server: "+ myID);
        }else if(recvd.startsWith("update_")) {
            activeClients.clear();

        }else if(recvd.startsWith("entry_")) {
            updateActiveClientList(recvd);
            hasUpdated = true;

        }else if(recvd.startsWith("text_")) {
            displayText(recvd);

        }else {
            myLastVoiceNote = voiceNote;
        }

    }

    public VoiceNote returnMyLastVN () {
        return myLastVoiceNote;
    }

    //text_<myHostnum>|content&
    //text_<hostnum>|<ip>&content%

    public void displayText(String recvd) {
        msgUpdated = true;
        String fromHostNum = returnHostNum(recvd);
        String fromClientIP = returnFromClientIP(recvd);
        String content = extractTextContent(recvd);


        messageBoard = messageBoard + fromClientIP + ": " + content + "\n";
        System.out.println("Message from: "+ fromClientIP);
        System.out.println("<"+fromHostNum+">: "+content);
    }

    public void processIncomingReq(String recvd) throws IOException {
        System.out.println("I have incoming");

        String hostNum = returnHostNum(recvd);
        System.out.println("incoming host num: "+ hostNum);
        String returnFromClientIP = returnFromClientIP(recvd);
        System.out.println("incoming host IP: "+ returnFromClientIP);

        returnIPCmdString(hostNum, returnFromClientIP);
        System.out.println("IP string returned");

        VeryBasicClient newVBC = new VeryBasicClient();
        newVBC.runMe(returnFromClientIP+"");

//        VeryBasicMultiClient newVBMC = new VeryBasicMultiClient();
//        //newVBMC.runMe(returnFromClientIP+"");
//        newVBMC.runMe("225.4.5.6");
    }

    public void processIPReturn(String recvd) throws InterruptedException {
        System.out.println("client returnedIP");

        String hostNum = returnHostNum(recvd);
        System.out.println("returned host num: "+ hostNum);
        String fromClientIP = returnFromClientIP(recvd);
        System.out.println("returned host IP: "+ fromClientIP);
        System.out.println("runMe with myIP: "+ getmyIP());

        openCallsToHosts.add(hostNum);

        VeryBasicClient newVBC = new VeryBasicClient();
        newVBC.runMe(fromClientIP+"");

//        VeryBasicMultiClient newVBMC = new VeryBasicMultiClient();
//        //newVBMC.runMe(getmyIP()+"");
//        //newVBMC.runMe(fromClientIP+"");
//        newVBMC.runMe("225.4.5.6");
    }

    public void processConf(String recvd) throws IOException {
        System.out.println("I have incoming");

        String hostNum = returnHostNum(recvd);
        System.out.println("incoming host num: "+ hostNum);
        String returnFromClientIP = returnFromClientIP(recvd);
        System.out.println("incoming host IP: "+ returnFromClientIP);

        returnConfIPCmdString(hostNum, returnFromClientIP);
        System.out.println("IP string returned");

        openCallsToHosts.add(hostNum);

//        VeryBasicClient newVBC = new VeryBasicClient();
//        newVBC.runMe(returnFromClientIP+"");

        newVBMC = new VeryBasicMultiClient();
        //newVBMC.runMe(returnFromClientIP+"");
        newVBMC.runMe("225.4.5.6");
    }

    public void processIPReturnConf(String recvd) {
        System.out.println("client returnedIP");

        String hostNum = returnHostNum(recvd);
        System.out.println("returned host num: "+ hostNum);
        String fromClientIP = returnFromClientIP(recvd);
        System.out.println("returned host IP: "+ fromClientIP);
        System.out.println("runMe with myIP: "+ getmyIP());

//        VeryBasicClient newVBC = new VeryBasicClient();
//        newVBC.runMe(fromClientIP+"");

        newVBMC = new VeryBasicMultiClient();
        //newVBMC.runMe(getmyIP()+"");
        //newVBMC.runMe(fromClientIP+"");
        newVBMC.runMe("225.4.5.6");
    }



    public String returnHostNum(String recvd) {
        return recvd.substring(recvd.indexOf("_")+1,
                recvd.indexOf("|"));
    }

    public String returnFromClientIP(String recvd) {
        return recvd.substring(recvd.indexOf("|")+1,
                recvd.indexOf("&"));
    }

    public String extractTextContent(String recvd) {
        return recvd.substring(recvd.indexOf("&")+1,
                recvd.indexOf("%"));
    }

    public void returnIPCmdString(String hostNumber, String fromClientIP) throws IOException {
        String myIP = getmyIP();
        System.out.println("fCIP: "+fromClientIP);
        System.out.println("myIP: "+myIP);

        //activeClients.add(new String[]{hostNumber, myIP});

        out.writeObject(("returnIP_"+ hostNumber+"|"+myIP+"&"));
        out.flush();
    }

    public void returnConfIPCmdString(String hostNumber, String fromClientIP) throws IOException {
        String myIP = getmyIP();
        System.out.println("fCIP: "+fromClientIP);
        System.out.println("myIP: "+myIP);

        //activeClients.add(new String[]{hostNumber, myIP});

        out.writeObject(("returnconf_"+ hostNumber+"|"+myIP+"&"));
        out.flush();
    }

    public String getmyIP() {
        return myIP;
    }

    public String getmyID() {
        return myID;
    }

    public boolean hasOpenCall(String hostName) {
        return openCallsToHosts.contains(hostName);
    }

    public void updateActiveClientList (String recvd) {
        String toAdd[] = new String[2];
        toAdd[0] = returnHostNum(recvd);
        toAdd[1] = returnFromClientIP(recvd);

        activeClients.add(toAdd);
        printActiveClientList();
    }

    public void printActiveClientList () {
        for (int i = 0; i < activeClients.size(); i++) {
            System.out.println("Active Client Host Num: "+ activeClients.get(i)[0]
                    + " Active Client IP: " + activeClients.get(i)[1]);

        }
    }

    public void disconnect() {
        try {
            System.out.println("try to disc");
            out.writeObject("quit");
            out.flush();

            stopFlag = true;

            out.close();
            in.close();
            socket.close();

            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        while(!stopFlag) {
            try {
                recvdCmdMessage();
            } catch (IOException | LineUnavailableException | InterruptedException | ClassNotFoundException e) {

            }
        }
        this.interrupt();
    }

    public static void main(String[] args) throws IOException{
        ClientVOIP newClient = new ClientVOIP("127.0.0.1");

        Thread t = new Thread(newClient);
        t.start();

        while(true) {
            newClient.sendCmdMessage();
        }
    }
}
