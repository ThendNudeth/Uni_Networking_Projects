import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerVOIP{

    String currentStatusMessages = "";
    static int startHostNumber = 0;
    ArrayList<String[]> activeClients;
    ArrayList<ClientConnection> activeClientsConnections;
    boolean hasUpdated = false;
    String serverIP;

    ServerVOIP(String serverIP) throws IOException {
        activeClientsConnections = new ArrayList<>();
        activeClients = new ArrayList<>();
        this.serverIP = serverIP;
    }

    private static class ClientConnection implements Runnable {

        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String myID;
        private String myIP;
        private ServerVOIP serverMain;
        boolean stopFlag = false;

        ClientConnection(Socket socket, ServerVOIP serverMain) throws IOException {
            System.out.println("new Client attempting to connect");
            serverMain.addToCurrentStatusMessage("new Client attempting to connect");
            this.socket = socket;
            this.serverMain = serverMain;

            setupStreams();
            setupClientInfo();
            addToActiveList();
            makeIPKnown();
            //serverMain.recvVNFromClient();
            //serverMain.sendVNToClient();

            System.out.println("client connected with hostnumber: "+ myID);
            System.out.println("client connected with IP: "+ myIP);
            serverMain.addToCurrentStatusMessage("client connected with hostnumber: "+ myID);
            serverMain.addToCurrentStatusMessage("client connected with IP: "+ myIP);

            serverMain.updateActiveListsOnClients();
        }

        public void setupStreams() throws IOException {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }

        public void setupClientInfo () {
            serverMain.incrementHostNumber();
            myID = serverMain.getHostNumberString();
            //myIP = "230.255.255."+myID;
            myIP = socket.getInetAddress().getHostAddress();
        }

        public void addToActiveList() {
            String toAdd[] = {myID, myIP};
            serverMain.activeClients.add(toAdd);
            serverMain.printConnectedClients();
            serverMain.activeClientsConnections.add(this);
        }

        public void makeIPKnown() throws IOException {
            out.writeObject(("first_"+myID+"|"+myIP+"&"));
            out.flush();
        }

        public void selectClientAndReqOpenComms(String fullMessage) throws IOException {

            String toHostNumber = fullMessage.substring(fullMessage.indexOf("_")+1,
                    fullMessage.indexOf("|"));
            String fromClientIP = fullMessage.substring(fullMessage.indexOf("|")+1,
                    fullMessage.indexOf("&"));

            System.out.println("toHostNumber: "+ toHostNumber);
            System.out.println("fromClientIP: "+ fromClientIP);
            serverMain.addToCurrentStatusMessage("toHostNumber: "+ toHostNumber);
            serverMain.addToCurrentStatusMessage("fromClientIP: "+ fromClientIP);

            if(serverMain.isActiveClient(toHostNumber)) {
                sendIsActive(toHostNumber, fromClientIP);
                chooseNotifyMethod(fullMessage, toHostNumber, fromClientIP);

            } else {
                sendIsInactive(toHostNumber);
            }
        }

        public void selectClientAndOpenConf(String fullMessage) throws IOException {

            String toHostNumber = fullMessage.substring(fullMessage.indexOf("_")+1,
                    fullMessage.indexOf("|"));
            String fromClientIP = fullMessage.substring(fullMessage.indexOf("|")+1,
                    fullMessage.indexOf("&"));

            System.out.println("toHostNumber: "+ toHostNumber);
            System.out.println("fromClientIP: "+ fromClientIP);
            serverMain.addToCurrentStatusMessage("toHostNumber: "+ toHostNumber);
            serverMain.addToCurrentStatusMessage("fromClientIP: "+ fromClientIP);

            if(serverMain.isActiveClient(toHostNumber)) {
                sendIsActive(toHostNumber, fromClientIP);
                chooseNotifyMethod(fullMessage, toHostNumber, fromClientIP);

            } else {
                sendIsInactive(toHostNumber);
            }
        }

        public void sendIsActive(String toHostNumber, String fromClientIP) throws IOException {
            out.writeObject("active");
            out.flush();
            System.out.println("active client: "+toHostNumber +" selected at: "+ fromClientIP);
            serverMain.addToCurrentStatusMessage("active client: "+toHostNumber +" selected at: "+ fromClientIP);
        }

        public void chooseNotifyMethod(String fullMessage, String toHostNumber, String fromClientIP) throws IOException {
            if(fullMessage.startsWith("opencomms_")) {
                serverMain.notifyClientIncomingConnection(serverMain.getClientByHostNum(toHostNumber), myID, fromClientIP);
                serverMain.addToCurrentStatusMessage("I have notified "+toHostNumber + " that " + myID +
                        " wants to have a word on: "+ fromClientIP);
                System.out.println("I have notified "+toHostNumber + " that " + myID +
                        " wants to have a word on: "+ fromClientIP);

            }else if(fullMessage.startsWith("returnIP_")) {
                serverMain.notifyClientOutgoingConnection(serverMain.getClientByHostNum(toHostNumber), myID, fromClientIP);
                serverMain.addToCurrentStatusMessage("I have notified "+toHostNumber + " that " + myID +
                        " wants to return its IP: "+ fromClientIP);
                System.out.println("I have notified "+toHostNumber + " that " + myID +
                        " wants to return its IP: "+ fromClientIP);

            }else if(fullMessage.startsWith("openconf_")) {
                serverMain.notifyConfIn(serverMain.getClientByHostNum(toHostNumber), myID, fromClientIP);


            }else if(fullMessage.startsWith("returnconf_")) {
                serverMain.notifyConfOut(serverMain.getClientByHostNum(toHostNumber), myID, fromClientIP);


            }
        }

        public void sendIsInactive(String toHostNumber) throws IOException {
            out.writeObject("inactive");
            out.flush();
            serverMain.addToCurrentStatusMessage("inactive client: "+toHostNumber +" selected, cannot open communication");
            System.out.println("inactive client: "+toHostNumber +" selected, cannot open communication");

        }

        public void recvCmdMessage() throws IOException, ClassNotFoundException {
//            byte[] buf = new byte[1024];
            Object obj = in.readObject();
            Object obj1 = obj;
            String possibleMsg = obj.toString();
            VoiceNote possibleVN = null;
            try {
                possibleVN = (VoiceNote) obj1;
            } catch (ClassCastException e) {

            }


            classifyMessage(possibleMsg, possibleVN);

        }

        public void classifyMessage(String recvd, VoiceNote voiceNote) throws IOException {
            if(recvd.startsWith("quit")) {
                serverMain.addToCurrentStatusMessage("client : "+ myID+ " has disconnected");
                System.out.println("client : "+ myID+ " has disconnected");
                disconnect();

            } else if(recvd.startsWith("opencomms_")) {
                selectClientAndReqOpenComms(recvd);

            }else if(recvd.startsWith("returnIP_")) {
                selectClientAndReqOpenComms(recvd);

            } else if(recvd.startsWith("openconf_")) {
                selectClientAndOpenConf(recvd);

            }else if(recvd.startsWith("returnconf_")) {
                selectClientAndOpenConf(recvd);

            }else if(recvd.startsWith("text_")) {
                String toSend = "text_"+myID+"|"+
                        myIP +"&"+ recvd.substring(recvd.indexOf("|") +1,
                        recvd.indexOf("&"))+"%";
                System.out.println("toSend: "+toSend);

                for (int i = 0; i < serverMain.activeClientsConnections.size(); i++) {
                    serverMain.activeClientsConnections.get(i).out.writeObject(toSend);
                    serverMain.activeClientsConnections.get(i).out.flush();
                }

            }else if(recvd.equals("")) {
                System.out.println("client disconnected");
                disconnect();
            } else {
                ClientConnection temp = serverMain.getClientByHostNum(voiceNote.toHost);
                temp.out.writeObject(voiceNote);
                temp.out.flush();

            }
        }

        @Override
        public void run() {
            try {
                while(!stopFlag) {
                    recvCmdMessage();
                }
            } catch (IOException e) {

            } catch (ClassNotFoundException e) {
                
            } finally {
                disconnect();
            }

        }

        public void disconnect(){
            try {

                serverMain.activeClients.remove(serverMain.getIndexOfClient(myID));
                serverMain.activeClientsConnections.remove(serverMain.getClientByHostNum(myID));
                serverMain.updateActiveListsOnClients();
                serverMain.printConnectedClients();

                stopFlag = true;

                out.close();
                in.close();
                socket.close();

            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }
    /*** end of client connection class,start of server main thread methods***/

    public void recvVNFromClient() throws IOException {
        DatagramSocket dgSocket = new DatagramSocket(8100);
        byte[] buf = new byte[128];
        DatagramPacket vnTest = new DatagramPacket(buf, buf.length);
        dgSocket.receive(vnTest);

        System.out.println(new String(buf));
    }

    public void sendVNToClient() throws IOException {
        DatagramSocket vnSocket = new DatagramSocket();
        byte[] buf = new byte[128];
        buf = "testalpha".getBytes();
        DatagramPacket vnTest = new DatagramPacket(buf, buf.length,
                InetAddress.getByName(serverIP), 8100);
        vnSocket.send(vnTest);
    }

    public void addToCurrentStatusMessage (String toAppend) {
        currentStatusMessages = currentStatusMessages + toAppend + "\n";
    }

    public String getCurrentStatusMessage () {
        return currentStatusMessages;
    }

    public boolean isActiveClient(String targetHostNumber) {
        boolean isActive = false;
        for (int  i = 0; i < activeClients.size(); i++ ) {
            if (activeClients.get(i)[0].equals(targetHostNumber)) {
                isActive = true;
            }
        }
        if (isActive) {
            return true;
        } else {
            return false;
        }
    }

    public void updateActiveListsOnClients() throws IOException {
        hasUpdated = true;
        for (int i = 0; i < activeClientsConnections.size(); i++) {
            activeClientsConnections.get(i).out.writeObject("update_");
            activeClientsConnections.get(i).out.flush();

            for (int j = 0; j < activeClients.size(); j++) {
                activeClientsConnections.get(i).out.writeObject(("entry_"
                        +activeClients.get(j)[0]+"|"+activeClients.get(j)[1]+"&"));
                activeClientsConnections.get(i).out.flush();
            }
        }
    }

    public String returnHostNum(String recvd) {
        return recvd.substring(recvd.indexOf("_")+1,
                recvd.indexOf("|"));
    }

    public void incrementHostNumber() {
        startHostNumber++;
    }

    public String getHostNumberString() {
        return startHostNumber + "";
    }

    public void printConnectedClients () {
        for (int i = 0; i < activeClients.size(); i++) {
            System.out.println("Active Client Host Num: "+ activeClients.get(i)[0]
                + " Active Client IP: " + activeClients.get(i)[1]);
            addToCurrentStatusMessage("Active Client Host Num: "+ activeClients.get(i)[0]
                    + " Active Client IP: " + activeClients.get(i)[1]);
        }
    }

    public int getIndexOfClient (String targetHostNumber) {
        int returnIndex = -1;
        for (int  i = 0; i < activeClients.size(); i++ ) {
            if (activeClients.get(i)[0].equals(targetHostNumber)) {
                returnIndex = i;
            }
        }
        return returnIndex;
    }

    public ClientConnection getClientByHostNum(String hostNumber) {
        for(int i = 0 ; i < activeClientsConnections.size(); i++) {
            if(activeClientsConnections.get(i).myID.equals(hostNumber)) {
                return activeClientsConnections.get(i);
            }
        }
        return null;
    }

    public void notifyClientIncomingConnection(ClientConnection notifyThisClient, String fromHostNum, String fromClientIP) throws IOException {
        notifyThisClient.out.writeObject(("incoming_"+ fromHostNum+ "|"+fromClientIP+"&"));
        notifyThisClient.out.flush();
    }

    public void notifyClientOutgoingConnection(ClientConnection notifyThisClient, String fromHostNum, String fromClientIP) throws IOException {
        notifyThisClient.out.writeObject(("returnIP_"+ fromHostNum+ "|"+fromClientIP+"&"));
        notifyThisClient.out.flush();
    }

    public void notifyConfIn(ClientConnection notifyThisClient, String fromHostNum, String fromClientIP) throws IOException {
        notifyThisClient.out.writeObject(("openconf_"+ fromHostNum+ "|"+fromClientIP+"&"));
        notifyThisClient.out.flush();
    }

    public void notifyConfOut(ClientConnection notifyThisClient, String fromHostNum, String fromClientIP) throws IOException {
        notifyThisClient.out.writeObject(("returnconf_"+ fromHostNum+ "|"+fromClientIP+"&"));
        notifyThisClient.out.flush();
    }

    public void discServer () {
        for (int i = 0; i < activeClientsConnections.size(); i++) {
                activeClientsConnections.get(i).disconnect();
        }
        activeClients.clear();
        System.exit(0);
    }
    public void handleConnections(ServerVOIP newServer) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(8000)) {
            while (true) {
                pool.execute(new ClientConnection(listener.accept(), newServer));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ServerVOIP newServer = new ServerVOIP("127.0.0.1");

        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(8000)) {
            while (true) {
                pool.execute(new ClientConnection(listener.accept(), newServer));
            }
        }
    }
}
