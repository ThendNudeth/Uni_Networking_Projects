
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

//public class DHCP_server {
//    // Setup the address pool where:
//    // addresspool[0] = MACaddress
//    // addresspool[1] = IPaddress
//    // addresspool[2] = inUse
//    ArrayList<ArrayList<String>> addressPool;
//
//    boolean running;
//    private byte[] buf;
//    private DatagramSocket socket;
//
//    public DHCP_server() throws IOException {
//        socket = new DatagramSocket(8001);
//        addressPool = new ArrayList<ArrayList<String>>();
//        buf = new byte[1024];
//    }
//
//    public void printAddressPool() {
//        for (int i = 0; i < addressPool.size(); i++) {
//            System.out.println(addressPool.get(i).get(0) + " "
//                    + addressPool.get(i).get(1)+ " " + addressPool.get(i).get(2));
//        }
//    }
//    public void DHCPinit() throws IOException{
//        // Add some addresses to the pool:
//        for (int i = 0; i < 5; i++) {
//            addToAddressPool();
//        }
//        printAddressPool();
//
//        DatagramPacket packet = new DatagramPacket(buf, buf.length);
//        socket.receive(packet);
//        System.out.println("MESSAGE1 RECEIVED");
//
//        String received
//                = new String(packet.getData(), 0, packet.getLength());
//        System.out.println(received);
//
//        InetAddress address = packet.getAddress();
//        int port = packet.getPort();
//
//        String offeredIP = "";
//        if (received.startsWith("IS_ANYBODY_OUT_THERE?")) {
//            System.out.println("DISCOVER RECEIVED");
//            offeredIP = DHCPOffer(address, port);
//        }
//
//        packet = new DatagramPacket(buf, buf.length);
//        socket.receive(packet);
//        System.out.println("MESSAGE3 RECEIVED");
//        received = new String(packet.getData(), 0, packet.getLength());
//        System.out.println(received);
//
//        if (received.equals("DAS_GOOD")) {
//            DHCPAck(offeredIP);
//        }
//        printAddressPool();
//
//    }
//
//
//    public String DHCPOffer(InetAddress address, int port) throws IOException{
//        // After receiving a discover message,
//        // Send an IP to the DHCPclient from a pool of ip's
//
//        String ipToSend = "";
//        for (int i = 0; i < addressPool.size(); i++) {
//            if (addressPool.get(i).get(2) == "false") {
//                ipToSend = addressPool.get(i).get(1);
//                break;
//
//            }
//        }
//
//        buf = ipToSend.getBytes();
//
//        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
//        System.out.println(address + " and " + port);
//        System.out.println(InetAddress.getLocalHost());
//
//        System.out.println("SENDING OFFER");
//        socket.send(packet);
//        System.out.println("OFFER SENT");
//
//        return ipToSend;
//    }
//
//    public void DHCPAck(String ip) {
//        // Triggers after DHCPAccept. This updates the ip pool
//        // to show that the ip is in use.
//
//        for (int i = 0; i < addressPool.size(); i++) {
//            if (addressPool.get(i).get(1) == ip) {
//                addressPool.get(i).set(2, "true");
//                break;
//            }
//        }
//
//    }
//
//    private String genIP(boolean isInternal) {
//        // Generate a random IP to store in the address pool.
//
//        String ipS = Helpers.genIP(isInternal);
//
//        if (!isUniq(ipS, "ip")) {
//            ipS = genIP(isInternal);
//        }
//        return ipS;
//    }
//
//    private String genMAC() {
//        // Generate a random MAC to store in the address pool.
//
//        String macS = Helpers.genMAC();
//
//        if (!addressPool.isEmpty()) {
//            if (!isUniq(macS, "mac")) {
//                macS = genMAC();
//            }
//        }
//
//        return macS;
//    }
//
//    private boolean isUniq(String testUniq, String type) {
//        boolean uniq = true;
//
//        if (type == "ip") {
//            for (int i = 0; i < addressPool.size(); i++) {
//                if (addressPool.get(i).get(1) == testUniq) {
//                    uniq = false;
//                    break;
//                }
//            }
//
//        } else if (type == "mac") {
//            for (int i = 0; i < addressPool.size(); i++) {
//                if (addressPool.get(i).get(0) == testUniq) {
//                    uniq = false;
//                    break;
//                }
//            }
//        }
//
//        return uniq;
//    }
//
//    private void genAddressPool(int numAdressses) {
//        // Generate a random new address pool for testing
//        // with numAddresses entries.
//
//        for (int i = 0; i < numAdressses; i++) {
//            addToAddressPool();
//        }
//    }
//
//    private void addToAddressPool() {
//        ArrayList<String> toAdd = new ArrayList<>();
//        toAdd.add(genMAC());
//        toAdd.add(genIP(true));
//        toAdd.add("false");
//
//        addressPool.add(toAdd);
//    }
//
//    public static void main(String[] args) throws IOException{
//        DHCP_server server = new DHCP_server();
//        server.DHCPinit();
//    }
//}