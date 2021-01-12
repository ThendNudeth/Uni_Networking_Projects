import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client extends Thread{
    private boolean isInternal;
    private Scanner scanner;
    InetAddress address;
    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;

    //Addresses:
    String myMAC;
    String natMAC;
    String natIP = "";
    String myIP;
    String workingIP;
    String workingPort;


    DatagramSocket dgsocket;
    private byte[] buf;

    public Client(String ternality) throws IOException {
        myMAC = Helpers.genMAC();

        if (ternality.equals("in")) {
            isInternal = true;
        } else if (ternality.equals("ex")) {
            isInternal = false;
        }

        myIP = Helpers.genIP(isInternal)+":3001";
        workingIP = myIP;
        if(!isInternal){System.out.println("myIP: "+workingIP);}

        dgsocket = new DatagramSocket();
        buf = new byte[1024];

        scanner = new Scanner(System.in);

        address = InetAddress.getLocalHost();
        socket = new Socket(address, 8000);

        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();

        System.out.println("connected");
    }

    public void recvMessage() throws IOException{
        byte[] recvBytes = new byte[1024];
        inputStream.read(recvBytes);
        String recvd = new String(recvBytes);
        if(recvd.startsWith("paquet was not routed")){
            System.out.println("paquet was not routed");
        } else if(recvd.contains(".")){
            System.out.println("recvd: "+ recvd);
            String toIpString = extractToIP(recvd);
            String fromIpString = extractFromIP(recvd);
            String fromInternalExternal = extractInternalExternal(recvd);
            String messageString = extractRecvdMessage(recvd);

            displayMessage(fromIpString, messageString);
            acknowledge_packet();
        }
    }

    public void sendMessage() throws IOException{
        String toSendMessage = scanner.nextLine();
        if(toSendMessage.equals("quit")){
            disconnect();
        }
        byte[] send = setupMessage(toSendMessage);
        if ((new String(send)).equals("invalid")){
            System.out.println("Invalid message format: usage <IP><|><message>");
            sendMessage();
        } else {
            outputStream.write(send);
        }
    }

    public void displayMessage(String from, String msg){
        System.out.println("FROM: <"+from+"> : "+msg);
    }

    /*** message convention: <toIP><fromIP><fromInternExternal><Message>***/
    public byte[] setupMessage(String toSendMessage) throws IOException {
        String toIpString = extractIP(toSendMessage);
        String fromIpString = workingIP;
        String messageString = extractMessage(toSendMessage);
        if(validateIP(toIpString)) {
            String myInternalExternal = "";
            if(isInternal) {
                myInternalExternal = "internal";
            } else {
                myInternalExternal = "external";
            }

            return (toIpString+"|"+fromIpString+"|"+
                    myInternalExternal+"|"+messageString).getBytes();
        } else {
            return "invalid".getBytes();
        }
    }

    private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    public static boolean validateIP(final String ip) {
        return true;
        //return PATTERN.matcher(ip).matches();
    }

    /*** <start>for processing messages to be sent ***/
    public String extractIP(String full) throws IOException{
        String returnString = "";
        try {
            returnString = full.substring(0, full.indexOf('|'));
        }catch (Exception e) {
            System.out.println("INVALID IP");
            return "invalid";
        }
        return returnString;
    }

    public String extractMessage(String full) {
        return full.substring(full.indexOf('|')+1);
    }
    /*** </end> for processing messages to be sent ***/

    /*** <start> for processing messages that has been received ***/
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

    public String extractRecvdMessage(String fullRecvd) {
        String parts[] = fullRecvd.split("\\|");
        return parts[3];
    }
    /*** </end> for processing messages that has been received ***/

    private void ARPRequest() throws IOException{
        broadcast("WHATS_YOUR_DIGITS?", InetAddress.getByName("127.0.0.1"));
    }

    private void ARPReceive() throws IOException{
        byte[] localbuf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(localbuf, localbuf.length);

        System.out.println("RECEIVING MESSAGE");
        dgsocket.receive(packet);
        System.out.println("ARP RECEIVED");

        String received
                = new String(packet.getData(), 0, packet.getLength());

        System.out.println("NAT adds received: " + received);

        natIP = received.substring(0, received.indexOf("?"));
        //System.out.println(received.indexOf("?"));
        System.out.println("natIP: "+natIP);
        natMAC = received.substring(received.indexOf("?")+1);
        System.out.println("natMAC: "+natMAC);

    }

    private void DHCPDiscover() throws IOException{
        // This is a broadcast message to all entities on the network
        // to be dropped by all except the natbox, which runs the DHCP server
        // and initiates DHCPOffer.
        broadcast("IS_ANYBODY_OUT_THERE?"+myIP, InetAddress.getByName("127.0.0.1"));

    }

    private void broadcast(String bcMessage, InetAddress address) throws IOException {
        dgsocket = new DatagramSocket();
//        dgsocket.setBroadcast(true);

        buf = bcMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8001);
        dgsocket.send(packet);
        //System.out.println("DISCOVER SENT: "+new String(buf));
        System.out.println("DISCOVER SENT");

    }

    private void DHCPRequest() throws IOException{
        // Respond to the DHCPServer that the ip is fine.
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        System.out.println("RECEIVING RESPONSE");
        dgsocket.receive(packet);
        System.out.println("RESPONSE RECEIVED");

        String received
                = new String(packet.getData(), 0, packet.getLength());

        System.out.println("Offer received: "+received);

        if (received.equals("MOVE_ALONG")) {
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            buf = "_PASS_".getBytes();
            packet = new DatagramPacket(buf, buf.length, address, port);
            dgsocket.send(packet);
            System.out.println("Accept sent");

        } else if (received.equals("FULL_CAPACITY")) {
            System.out.println("Unfortunately all IP's are in use at the moment." +
                    "Please contact your network administrator.");
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            buf = "DAS_BAD".getBytes();
            packet = new DatagramPacket(buf, buf.length, address, port);
            dgsocket.send(packet);
            System.out.println("Accept sent");
            disconnect();
        } else {
            workingIP = received;
            workingPort = received.substring(received.indexOf(":")+1);
            System.out.println("Offer accepted: "+workingIP);
            System.out.println("Offer accepted:"+workingPort);

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            buf = "DAS_GOOD".getBytes();
            packet = new DatagramPacket(buf, buf.length, address, port);
            dgsocket.send(packet);
            System.out.println("ACCEPT OFFER SENT");
        }
    }

    public void disconnect(){
        try {
            outputStream.write(("disc").getBytes());
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            socket.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public void run(){
        while(true) {
            try {
                recvMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendIP(String ip) throws IOException {
        byte[] send = ("assignip:"+ip+"|").getBytes();
        outputStream.write(send);
    }

    public void acknowledge_packet() throws IOException {
        outputStream.write(("ack").getBytes());
        outputStream.flush();
        System.out.println("ack sent");
    }

    public static void main(String[] args) throws IOException{
        Scanner in = new Scanner(System.in);
        String ternality = "";
        System.out.println("Is this client (in)ternal or (ex)ternal?");
        while (!(ternality.equals("in")||ternality.equals("ex"))) {
            System.out.println("Please type in either 'ex' or 'in'.");
            ternality = in.nextLine();
        }
        Client newClient = new Client(ternality);
        newClient.DHCPDiscover();
        newClient.DHCPRequest();
        newClient.ARPRequest();
        newClient.ARPReceive();
        newClient.sendIP(newClient.workingIP);

        Thread t = new Thread(newClient);
        t.start();

        while(true) {
            newClient.sendMessage();
        }
    }
}
