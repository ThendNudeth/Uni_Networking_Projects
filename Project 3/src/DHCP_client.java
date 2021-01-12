import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

//public class DHCP_client {
//    String macAddress;
//    String ipAddress;
//    DatagramSocket socket;
//    private byte[] buf;
//    ArrayList<ArrayList<String>> addressPool;
//
//    public DHCP_client() throws IOException{
//        socket = new DatagramSocket();
//        buf = new byte[1024];
//    }
//
////    private String genMAC() {
////        // Generate a random MAC to store in the address pool.
////
////        String macS = "";
////        int macI;
////
////        for (int i = 0; i < 6; i++) {
////            macI = (int)(Math.random()*255);
////            if (i == 0) {
////                macS = macS + Integer.toHexString(macI);
////            }else {
////                macS = macS + ":" + Integer.toHexString(macI);
////            }
////        }
////        if (!addressPool.isEmpty()) {
////            if (!isUniq(macS, "mac")) {
////                macS = genMAC();
////            }
////        }
////        return macS;
////    }
//
//    private void DHCPDiscover() throws IOException{
//        // This is a broadcast message to all entities on the network
//        // to be dropped by all except the natbox, which runs the DHCP server
//        // and initiates DHCPOffer.
//        broadcast("IS_ANYBODY_OUT_THERE?", InetAddress.getByName("127.0.0.1"));
//
//    }
//
//    private void broadcast(String bcMessage, InetAddress address) throws IOException {
//        socket = new DatagramSocket();
////        socket.setBroadcast(true);
//
//        buf = bcMessage.getBytes();
//
//        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8001);
//        socket.send(packet);
//        System.out.println("DISCOVER SENT.");
//
//    }
//
//    private void DHCPAccept() throws IOException{
//        // Respond to the DHCPServer that the ip is fine.
//        DatagramPacket packet = new DatagramPacket(buf, buf.length);
//
//        System.out.println("RECEIVING MESSAGE");
//        socket.receive(packet);
//        System.out.println("MESSAGE2 RECEIVED");
//
//        String received
//                = new String(packet.getData(), 0, packet.getLength());
//        ipAddress = received;
//        System.out.println(received);
//
//        InetAddress address = packet.getAddress();
//        int port = packet.getPort();
//        buf = "DAS_GOOD".getBytes();
//        packet = new DatagramPacket(buf, buf.length, address, port);
//        socket.send(packet);
//        System.out.println("Accept sent");
//
//    }
//
//    public static void main(String[] args) throws IOException{
//        DHCP_client client = new DHCP_client();
//        client.DHCPDiscover();
//        client.DHCPAccept();
//    }
//}