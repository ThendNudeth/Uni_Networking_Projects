import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NAT_box{
    PrintWriter writer;
    {
        try {
            writer = new PrintWriter("IPaddresses.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    {
        try {
            writer = new PrintWriter("MACaddresses.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    /***
     * // Setup the address pool where:
     *     // addresspool[0] = MACaddress
     *     // addresspool[1] = internal IPaddress
     *     // addresspool[2] = inUse
     *     ***/
    ArrayList<ArrayList<String>> addressPool;
    ArrayList<ClientConnection> allClientsConnected;
    static NatTable natTableClass;
    String myIP;
    {
        try {
            myIP = Helpers.genIP(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    String myMAC;

    {
        try {
            myMAC = Helpers.genMAC();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramSocket dgsocket;

    public NAT_box(long seconds) throws SocketException {
        addressPool = new ArrayList<ArrayList<String>>();
        /***/
        long timeoutSeconds = seconds;
        natTableClass = new NatTable(timeoutSeconds);
        natTableClass.startTime(timeoutSeconds);
        allClientsConnected = new ArrayList<ClientConnection>();
        dgsocket = new DatagramSocket(8001);
        System.out.println("NAT_IP: "+myIP);
        System.out.println("NAT_MAC: "+myMAC);
    }

    private static class ClientConnection implements Runnable{
        private Socket socket;
        private OutputStream outputStream;
        private InputStream inputStream;
        private InetAddress address;
        private NAT_box mybox;
        private String clientIP;
        private byte[] buf;

        ClientConnection(NAT_box mybox, Socket socket) throws IOException {
            mybox.allClientsConnected.add(this);
            mybox.DHCPinit();
            mybox.ARPReply();
            System.out.println("CLIENT CONNECTED");

            this.mybox = mybox;
            this.socket = socket;

            buf = new byte[1024];

            address = socket.getInetAddress();
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        }

        @Override
        public void run() {
            //TODO: need a better end condition
            while(true) {
                try {
                    recvMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /*** message convention: <toIP><fromIP><fromInternalExternal><Message>***/
        public void recvMessage() throws IOException{
            byte[] read = new byte[1024];
            try{
                inputStream.read(read);

                String recvMessage = new String(read);
                if(recvMessage.startsWith("disc")){
                    //mybox.printAddressPool();
                    for(int i = 0; i < mybox.addressPool.size(); i++) {
                        if(mybox.addressPool.get(i).get(1).equals(clientIP.substring(0, clientIP.indexOf(":")))) {
                            mybox.addressPool.get(i).set(2, "false");
                        }
                    }
                    for(int i = 0; i < mybox.natTableClass.natTable.size(); i++) {
                        if(mybox.natTableClass.natTable.get(i)[0].equals(clientIP)) {
                            mybox.natTableClass.natTable.remove(i);
                        }
                    }
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                    mybox.printAddressPool();
                    System.out.println("disconnected :"+clientIP);
                }else if(recvMessage.startsWith("assignip:")){
                    String ip = recvMessage.substring(recvMessage.indexOf(":")+1, recvMessage.indexOf("|"));
                    //System.out.println("ip:"+ip);
                    clientIP = ip;
                }else if(recvMessage.startsWith("ack")) {
                    System.out.println("packet routed");
                } else {
                    String toIpString = extractToIP(recvMessage);
                    String fromIpString = extractFromIP(recvMessage);
                    String fromInternalExternal = extractInternalExternal(recvMessage);
                    String messageString = extractMessage(recvMessage);
                    String toInternalExternal = classifyIP(toIpString);

                    System.out.println("received: "+recvMessage);
                    System.out.println("toIP: "+toIpString);
                    System.out.println("fromIP: "+fromIpString);
                    System.out.println("fromInternalExternal: "+fromInternalExternal);
                    System.out.println("message: "+messageString);
                    System.out.println("toInternalExternal: "+toInternalExternal);

                    sendToClient(toIpString, fromIpString, fromInternalExternal, messageString);
                }

            }catch (Exception e) {
                outputStream.close();
                inputStream.close();
                socket.close();
            }
        }

        public void sendToClient(String toIP, String fromIP, String fIE, String msg) throws IOException {
            byte[] sendBuf;
            ClientConnection temp;
            String toIpString = toIP;
            String fromIpString = fromIP;
            String fromInternalExternal = fIE;
            String messageString = msg;
            String toInternalExternal = classifyIP(toIpString);


            sendBuf = (toIpString+"|"+fromIpString+
                    "|"+fromInternalExternal+"|"+messageString).getBytes();

            if(fromInternalExternal.equals("internal")&&toInternalExternal.equals("internal")){
                //internal-->internal: route packet with no modification
                if((temp = mybox.locateClientConnection(toIpString))!=null){
                    System.out.println(new String(sendBuf));
                    temp.outputStream.write(sendBuf);
                    temp.outputStream.flush();
                }

            }else if(fromInternalExternal.equals("internal")&&toInternalExternal.equals("external")){
                //internal-->external:
                if(mybox.natTableClass.locateExternalEntry(toIpString).equals("notfound")) {
                    mybox.natTableClass.enterEntry(fromIpString, toIpString);
                }
                if((temp = mybox.locateExternalClientConnection(toIpString))!=null){
                    sendBuf = (toIpString+"|"+(mybox.myIP
                            +fromIpString.substring(fromIpString.indexOf(":")))+
                            "|"+fromInternalExternal+"|"+messageString).getBytes();
                    System.out.println(new String(sendBuf));
                    temp.outputStream.write(sendBuf);
                    temp.outputStream.flush();
                }

            }else if(fromInternalExternal.equals("external")&&toInternalExternal.equals("internal")){
                //external-->internal:
                String toPort = toIpString.substring(toIpString.indexOf(":")+1);
                String translatedToIP = mybox.natTableClass.locateByPortEntry(toPort);
                //String translatedToIP = mybox.natTableClass.locateInternalEntry(fromIpString);
                System.out.println("translated IP: "+translatedToIP);
                if(!translatedToIP.equals("notfound")) {
                    //System.out.println("found translation");
                    if((temp = mybox.locateClientConnection(translatedToIP))!=null){
                        System.out.println(new String(sendBuf));
                        temp.outputStream.write(sendBuf);
                        temp.outputStream.flush();
                    }
                }else {
                    outputStream.write(new String("paquet was not routed").getBytes());
                }

            }else if(fromInternalExternal.equals("external")&&toInternalExternal.equals("external")){
                //external-->external:
                outputStream.write(new String("paquet was not routed").getBytes());
            }
        }

        public String classifyIP(String toIpString) {
            if(mybox.isInternalClient(toIpString)) {
                return "internal";
            }else if(mybox.myIP.equals(toIpString.substring(0, toIpString.indexOf(":")))) {
                return "internal";
            } else {
                return "external";
            }
        }

        public String extractToIP(String fullRecvd) throws IOException{
            String parts[] = fullRecvd.split("\\|");
            return parts[0];
        }

        public String extractFromIP(String fullRecvd) throws IOException{
            String parts[] = fullRecvd.split("\\|");
            return parts[1];
        }

        public String extractInternalExternal(String fullRecvd) {
            String parts[] = fullRecvd.split("\\|");
            return parts[2];
        }


        public String extractMessage(String fullRecvd) {
            String parts[] = fullRecvd.split("\\|");
            return parts[3];
        }
    }
    /***end of Runnable class***/

    private boolean isUniq(String testUniq, String type) {
        boolean uniq = true;

        if (type.equals("ip")) {
            for (int i = 0; i < addressPool.size(); i++) {
                if (addressPool.get(i).get(1).equals(testUniq)) {
                    uniq = false;
                    break;
                }
            }

        } else if (type.equals("mac")) {
            for (int i = 0; i < addressPool.size(); i++) {
                if (addressPool.get(i).get(0).equals(testUniq)) {
                    uniq = false;
                    break;
                }
            }
        }

        return uniq;
    }

    public boolean isInternalClient(String ip) {
        String test = ip.substring(0, ip.indexOf(":"));
        for (int i = 0; i < addressPool.size(); i++) {
            if (addressPool.get(i).get(1).equals(test) &&
                    addressPool.get(i).get(2).equals("true")) {
                return true;
            }
        }
        return false;
    }

    public void ARPReply() throws IOException{
        System.out.println("ARP_START");
        byte [] localbuf = new byte[1024];

        DatagramPacket packet = new DatagramPacket(localbuf, localbuf.length);
        dgsocket.receive(packet);
        System.out.println("ARP RECEIVED");

        String received
                = new String(packet.getData(), 0, packet.getLength());

        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        //System.out.println("received: "+received);

        if (received.startsWith("WHATS_YOUR_DIGITS?")) {
            System.out.println("DISCOVER RECEIVED");
            sendMessage(myIP+"?"+myMAC, address, port);
        }
    }

    public void DHCPinit() throws IOException{
        System.out.println("DHCP_INIT");
        byte [] localbuf = new byte[1024];

        DatagramPacket packet = new DatagramPacket(localbuf, localbuf.length);
        dgsocket.receive(packet);
        //System.out.println("MESSAGE1 RECEIVED");

        String received
                = new String(packet.getData(), 0, packet.getLength());
        //System.out.println(received);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        String offeredIP = "";

        if (received.startsWith("IS_ANYBODY_OUT_THERE?")) {
            System.out.println("DISCOVER RECEIVED");
            if (received.contains("?192.168")) {
                sendMessage("MOVE_ALONG", address, port);

            } else {
                offeredIP = DHCPOffer(address, port);
                System.out.println("Offered ip: "+offeredIP);
            }

        }

        packet = new DatagramPacket(localbuf, localbuf.length);
        dgsocket.receive(packet);
        //System.out.println("MESSAGE3 RECEIVED");
        received = new String(packet.getData(), 0, packet.getLength());
        //System.out.println(received);

        if (received.equals("DAS_GOOD")) {
            DHCPAck(offeredIP);
            System.out.println("OFFERED IP ACKNOWLEDGED");
        }
        printAddressPool();
//       Helpers.printMACs();
    }

    public void sendMessage(String message, InetAddress address, int port) throws IOException {
        byte [] localbuf;
        localbuf = message.getBytes();

        DatagramPacket packet = new DatagramPacket(localbuf, localbuf.length, address, port);

        //System.out.println("SENDING MESSAGE");
        dgsocket.send(packet);
        //System.out.println("MESSAGE SENT: "+ new String(localbuf));
    }

    public String DHCPOffer(InetAddress address, int port) throws IOException{
        // After receiving a discover message,
        // Send an IP to the DHCPclient from a pool of ip's
        System.out.println("OFFER IP");
        String ipToSend = "";
        Boolean found = false;
        for (int i = 0; i < addressPool.size(); i++) {
            if (addressPool.get(i).get(2).equals("false")) {
                ipToSend = addressPool.get(i).get(1);
                ipToSend = ipToSend + ":" + addressPool.get(i).get(3);
                found = true;
                break;
            }
        }
        if (found) {
            sendMessage(ipToSend, address, port);
        } else {
            sendMessage("FULL_CAPACITY", address, port);
        }


        return ipToSend;
    }

    public void DHCPAck(String ip) {
        // Triggers after DHCPAccept. This updates the ip pool
        // to show that the ip is in use.
        System.out.println("OFFER ACCEPTED UPDATE ADDRESSPOOL");
        for (int i = 0; i < addressPool.size(); i++) {
            if (addressPool.get(i).get(1).equals(ip.substring(0, ip.indexOf(":")))) {
                addressPool.get(i).set(2, "true");
                break;
            }
        }

    }

    /***    // addresspool[0] = MACaddress
     *     // addresspool[1] = internal IPaddress
     *     // addresspool[2] = inUse
     *     // addresspool[3] = port ***/
    private void addToAddressPool() throws IOException{
        ArrayList<String> toAdd = new ArrayList<>();
        toAdd.add(Helpers.genMAC());
        toAdd.add(Helpers.genIP(true));
        toAdd.add("false");
        toAdd.add(genPort());

        addressPool.add(toAdd);
    }

    public String genPort() {
        String portS = "";
        boolean exists = false;
        int port = (int)(Math.round(Math.random()*3000));
        portS = Integer.toString(port);

        for (int i = 0; i < addressPool.size(); i++) {
            if (addressPool.get(i).get(3).equals(portS)) {
                exists = true;
                break;
            }
        }

        if (exists) {
            genPort();
        }

        return portS;
    }
    public void printAddressPool() {
        for (int i = 0; i < addressPool.size(); i++) {
            System.out.println(addressPool.get(i).get(0) + " "
                    + addressPool.get(i).get(1)+ " " + addressPool.get(i).get(2)
                    + " " + addressPool.get(i).get(3));
        }
    }


    public ClientConnection locateClientConnection(String toIP){
        ClientConnection returnClient = null;
        for (int i = 0; i < allClientsConnected.size(); i++) {
            if(allClientsConnected.get(i).clientIP.equals(toIP)) {
                returnClient = allClientsConnected.get(i);
            }
        }
        return returnClient;
    }

    public ClientConnection locateExternalClientConnection(String toExternalIP){
        ClientConnection returnClient = null;
        //String toExternalIP =natTableClass.locateExternalEntry(fromInternalIP);
        //System.out.println("toExternalIP: "+toExternalIP);
        for (int i = 0; i < allClientsConnected.size(); i++) {
            if(allClientsConnected.get(i).clientIP.equals(toExternalIP)) {
                returnClient = allClientsConnected.get(i);
            }
        }
        return returnClient;
    }


    public static void main(String[] args) throws IOException{
        NAT_box newBox = new NAT_box(Long.parseLong(args[0]));
        //gen 3 random addresses
        if(!(args.length == 2)) {
            System.out.println("usage: @param timeoutseconds, @param num addresses to gen");
            System.exit(0);
        }
        for (int i = 0; i < Integer.parseInt(args[1]); i++) {
            newBox.addToAddressPool();
        }

        newBox.printAddressPool();


        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(8000)) {
            while (true) {
                pool.execute(new ClientConnection(newBox, listener.accept()));
            }
        }
    }
}
