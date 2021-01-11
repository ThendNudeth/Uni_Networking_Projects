import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Receiver extends Thread{
    static DatagramSocket socket;
    static Socket tcpSocket;
    static ObjectOutputStream out;
    static ObjectInputStream in;
    static FileOutputStream fos;
    InetAddress address;

    static boolean hasNewSeqNums = false;
    static boolean interrupt = false;
    int dropThis=0;

    static List<Integer> recvdNums;
    static List<Integer>  seqNums;
    static List<Integer> missingNums;

    static List<byte[]> receiverMassiveList;

    public static byte[] filenamebytes;
    public static byte[] numPackets;
    int totalNumPackets;

    String filename;


    //continually checks for new TCP sent sequence numbers from the server, adds them to a local list
    @Override
    public void run() {
        try {
            while(!interrupt){
                int temp = readSeq();
                seqNums.add(temp);;
                hasNewSeqNums = true;
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    //reads the sequence numbers over TCP from the server
    public int readSeq(){
        int newNum = 0;
        try {
            newNum = in.readInt();
        } catch (IOException e) {
        }
        return newNum;
    }


    public void sortRecvdPackets(){

        Comparator<byte[]> cmpSeq = new Comparator<byte[]>() {
            @Override
            public int compare(byte[] leftWholeByte, byte[] rightWholeByte) {
                byte[] intLeft = new byte[4];
                byte[] intRight = new byte[4];

                for(int i =0; i < 4; i++){
                    intLeft[i] = leftWholeByte[i];
                    intRight[i] = rightWholeByte[i];
                }
                ByteBuffer leftWrapper = ByteBuffer.wrap(intLeft);
                ByteBuffer rightWrapper = ByteBuffer.wrap(intRight);
                int leftNum = leftWrapper.getInt();
                int rightNum = rightWrapper.getInt();
                return Integer.valueOf(leftNum).compareTo(Integer.valueOf(rightNum));
            }
        };

        receiverMassiveList.sort(cmpSeq);
    }

    //tests whether the numbers received from the server matches the sequence numbers containe din the packets we have receiced
    public void checkMissing(){
        boolean hasMissing = false;
        boolean foundThisNum;
        hasNewSeqNums = false;

        missingNums.clear();

        //for each number in seqNums list received from server,
        // test whether it is in the list of recvdnumbers we extracted
        // from the packets
        for(int j = 0; j < seqNums.size(); j++) {
            if(recvdNums.contains(seqNums.get(j))){
                foundThisNum = true;
            } else{
                foundThisNum = false;
            }
            //add any numbers not found to a list containing missing sequence numbers
            if(!foundThisNum){
                missingNums.add(seqNums.get(j));
                hasMissing = true;
            }else if(foundThisNum){
            }
        }
        System.out.println("packetloss: "+((double)missingNums.size())/((double)seqNums.size())*100.0+"%");
        try{
            if(hasMissing) {
                //send convention, send how many missing int, then send list of missing
                int numberMissing = missingNums.size();
                out.writeInt(numberMissing);
                out.writeObject(missingNums);
                for(int j = 0; j < numberMissing; j++){
                    byte[] aMissingPacketBuf = new byte[576];
                    byte[] buf = new byte[576];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8000);
                    socket.send(packet);

                    DatagramPacket newPacket = new DatagramPacket(aMissingPacketBuf, aMissingPacketBuf.length, address, 8000);
                    socket.receive(newPacket);

                    byte[] newBuf = new byte[newPacket.getLength()];
                    byte[] intBuf = new byte[4];

                    for(int k = 0; k < 4 ; k++){
                        intBuf[k] = aMissingPacketBuf[k];
                    }
                    for(int k = 0; k < newPacket.getLength() ; k++){
                        newBuf[k] = aMissingPacketBuf[k];
                    }

                    ByteBuffer intWrapper = ByteBuffer.wrap(intBuf);
                    int thisPacketNum = intWrapper.getInt();

                    receiverMassiveList.add(newBuf);
                    recvdNums.add(thisPacketNum);
                }
            }else{
                //send 0
                int numberMissing = 0;
                out.writeInt(0);
            }
        }catch(Exception e){
            System.out.println("exception in hasmissing after if");
            System.out.println(e);
        }
    }

    public int getNumPacketsReceived() {
        return recvdNums.size();
    }

    //the heart method of the client, runs continually to receive packets with data
    // from the server
    public  boolean recvPackets(){
        boolean hasSenderEndedTransmission = false;
        int dropped = 0;
            if(dropThis==-1){
                try {
                    dropped++;
                    byte[] buf = new byte[576];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8000);

                    //send request
                    socket.send(packet);

                    // get response
                    packet = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(2000);
                    socket.receive(packet);
                    //System.out.println(dropped);
                } catch (IOException e) {
                    System.out.println("Exception in dropping a packet");
                    e.printStackTrace();
                }
            }else{
                try{
                    byte[] buf = new byte[576];

                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8000);

                    //send request
                    socket.send(packet);

                    // get response
                    packet = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(2000);
                    socket.receive(packet);

                    //pack response into a byte of dynamic size
                    byte[] newBuf = new byte[packet.getLength()];
                    for(int j = 0; j < packet.getLength() ; j++){
                        newBuf[j] = buf[j];
                    }

                    //separate data and sequence nums, extract sequence number for this packet
                    byte[] intbuf = new byte[4];
                    intbuf[0] = newBuf[0];
                    intbuf[1] = newBuf[1];
                    intbuf[2] = newBuf[2];
                    intbuf[3] = newBuf[3];
                    ByteBuffer intWrapper = ByteBuffer.wrap(intbuf);
                    int thisPacketNum = intWrapper.getInt();
                    recvdNums.add(thisPacketNum);

                    //wrap data in bytebuffer for ease of use, put the data part of the packet into the wrapper
                    byte[] databuf = new byte[packet.getLength()-4];
                    ByteBuffer dataWrapper = ByteBuffer.wrap(databuf);
                    dataWrapper.put(packet.getData(), 4, packet.getLength()-4);

                    //this will be the buffer that is written to file
                    byte[] finalData = databuf;

                    //instead of writing to file immediately, put into massive list and check
                    receiverMassiveList.add(newBuf);

                    //if a non max size package is recv, this is the last package.
                    // while loop will end after this iteration
                    if(packet.getLength()<576 ){
                        hasSenderEndedTransmission =true;
                        return true;
                    }
                }catch(SocketTimeoutException se){
                    return true;
                    //break;
                }catch(Exception e){
                    System.out.println("exception in recv packets loop");
                    System.out.println(e);
                }
            }
            dropThis++;
        return false;
    }

    public void afterRecvPackets(){
        checkMissing();
        while(missingNums.size()>0){
            checkMissing();
        }
        System.out.println("checked for missing packets");
        sortRecvdPackets();
        System.out.println("packets sorted");
        writeToFile();
        System.out.println("done writing!");
        interrupt = true;
        //sleep so sockets close at reasonable times and dont cause exceptions
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public  void writeToFile(){
        System.out.println("writing to File");
        int current = 2;
        while(current < receiverMassiveList.size()){
            //write to file
                try{
                    byte[] printBuf = new byte[(receiverMassiveList.get(current).length-4)];
                    ByteBuffer dataWrapper = ByteBuffer.wrap(printBuf);
                    dataWrapper.put(receiverMassiveList.get(current), 4, (receiverMassiveList.get(current).length-4));
                    printBuf = dataWrapper.array();
                    fos.write(printBuf);
                    current++;
                }catch(Exception e) {
                    System.out.println("exception in write to file");
                    System.out.println(e);
                }
        }

    }

    //connect to server and setup sockets
    public  void connect()throws Exception{
        socket = new DatagramSocket();
        tcpSocket = new Socket(address, 8001);
        out = new ObjectOutputStream(tcpSocket.getOutputStream());
        in = new ObjectInputStream(tcpSocket.getInputStream());
    }

    public double getTotalNumPackets() {
        byte[] databuf = new byte[8];
        databuf[0] = numPackets[4];
        databuf[1] = numPackets[5];
        databuf[2] = numPackets[6];
        databuf[3] = numPackets[7];
        databuf[4] = numPackets[8];
        databuf[5] = numPackets[9];
        databuf[6] = numPackets[10];
        databuf[7] = numPackets[11];
        ByteBuffer dataWrapper = ByteBuffer.wrap(databuf);
        return dataWrapper.getDouble();
    }

    public Receiver(String filename, String addressStr) throws Exception {
        address = InetAddress.getByName(addressStr);
        socket = new DatagramSocket();
        tcpSocket = new Socket(address, 8001);
        out = new ObjectOutputStream(tcpSocket.getOutputStream());
        in = new ObjectInputStream(tcpSocket.getInputStream());

        recvdNums = new ArrayList<Integer>();
        seqNums = new ArrayList<Integer>();
        missingNums = new ArrayList<Integer>();
        receiverMassiveList = new ArrayList<byte[]>();



        //send numpackets and fileextension
        try {
            filenamebytes = (byte[])in.readObject();
            receiverMassiveList.add(filenamebytes);
            numPackets = (byte[])in.readObject();
            receiverMassiveList.add(numPackets);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //separate data and sequence nums, extract sequence number for this packet
        byte[] intbuf = new byte[4];
        intbuf[0] = filenamebytes[0];
        intbuf[1] = filenamebytes[1];
        intbuf[2] = filenamebytes[2];
        intbuf[3] = filenamebytes[3];
        ByteBuffer intWrapper = ByteBuffer.wrap(intbuf);
        int seqnum = intWrapper.getInt();

        byte[] data = new byte[filenamebytes.length-4];
        for (int i = 0; i < data.length ; i++) {
            data[i] = filenamebytes[i+4];
        }
        String filenameExtension = "";
        if (seqnum==0) {
            this.filename = new String(data);
            System.out.println(this.filename);

            int i = this.filename.length()-1;
            while (this.filename.charAt(i)!='.') {
                i--;
            }
            filenameExtension = this.filename.substring(i);
        }

        fos = new FileOutputStream(filename+""+filenameExtension);

    }
}
