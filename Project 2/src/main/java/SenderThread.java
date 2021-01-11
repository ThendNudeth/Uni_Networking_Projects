import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class SenderThread extends Thread {

    public DatagramSocket socket;
    public ServerSocket serverSocket;
    public Socket tcpSocket;
    protected BufferedReader in = null;
    public boolean moreWriting = true;
    ObjectInputStream tcpIn;
    ObjectOutputStream tcpOut;
    static FileInputStream fis;
    int startSequence;
    int endSequence;
    InetAddress address;
    int port;
    int numMissing;
    File file = new File("");

    static List<byte[]> senderMassiveList;

    public SenderThread() throws IOException {
        this("SenderThread");
    }

    public void setAddress(String addressStr) throws UnknownHostException {
        this.address = InetAddress.getByName(addressStr);
    }
    public void setFile(File file) throws IOException {
        this.file = file;
    }

    public double getNumPackets() {
        double fsize = file.length();
        return (fsize/576);
    }
    public SenderThread(String name) throws IOException {
        super(name);
        socket = new DatagramSocket(8000);
        serverSocket = new ServerSocket(8001);
    }

    //started from sender
    public void run() {
        try {
            tcpSocket = serverSocket.accept();
            tcpOut = new ObjectOutputStream(tcpSocket.getOutputStream());
            tcpIn = new ObjectInputStream(tcpSocket.getInputStream());

            //TODO: send this packet throughtAbsolutePath());
            in = new BufferedReader(new FileReader(file));
            fis = new FileInputStream(file);
            //fis = new FileInputStream("one-liners.txt");
        } catch (FileNotFoundException e) {
            System.err.println("filenotfound");
        } catch (IOException e) {
            System.out.println("exception at start");
            e.printStackTrace();
        }
        senderMassiveList = new ArrayList<byte[]>();
        startSequence = 2;
        endSequence = startSequence;

        //send numpackets and fileextension
        try {
            System.out.println("starting to send file:");
            byte[] fileSeqBuffer = ByteBuffer.allocate(4).putInt(0).array();
            byte[] fileNameBuffer = file.getName().getBytes();
            byte[] finalFile = new byte[4+fileNameBuffer.length];
            ByteBuffer concatFile = ByteBuffer.allocate(4+fileNameBuffer.length);
            concatFile.put(fileSeqBuffer);
            concatFile.put(fileNameBuffer);
            finalFile = concatFile.array();
            senderMassiveList.add(finalFile);

            byte[] numPacketsSeqBuffer = ByteBuffer.allocate(4).putInt(1).array();
            byte[] numPacketsBuffer = ByteBuffer.allocate(8).putDouble(getNumPackets()).array();
            byte[] finalNum = new byte[12];
            ByteBuffer concatNumPackets = ByteBuffer.allocate(4+8);
            concatNumPackets.put(numPacketsSeqBuffer);
            concatNumPackets.put(numPacketsBuffer);
            finalNum = concatNumPackets.array();
            senderMassiveList.add(finalNum);

            tcpOut.writeObject(finalFile);
            tcpOut.flush();
            tcpOut.reset();
            tcpOut.writeObject(finalNum);
            tcpOut.flush();
            tcpOut.reset();
        } catch (IOException e) {
            System.out.println("exception in writing first two signal packets");
            e.printStackTrace();
        }

        while (moreWriting) {
            try {
                //this is mostly housekeeping, make it easier to keep track of lengths and data
                List <Byte> preBufList = new ArrayList<Byte>();
                List <Byte> seqBufList = new ArrayList<Byte>();
                List <Byte> bufBufList = new ArrayList<Byte>();

                byte[] seqBuf = new byte[4];
                byte[] buf = new byte[576];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                //allocate first 4 bytes to represent an int
                seqBuf = ByteBuffer.allocate(4).putInt(endSequence).array();
                seqBufList.add(seqBuf[0]);
                seqBufList.add(seqBuf[1]);
                seqBufList.add(seqBuf[2]);
                seqBufList.add(seqBuf[3]);

                //read in the data from the file into the buffer list
                readNextByteBuf(preBufList);

                int maxlen=0;
                //add sequence numbers to final buffer list
                for(int j = 0; j < 4; j++){
                    bufBufList.add(seqBufList.get(j));
                    maxlen++;
                }
                //add data to the final buffer list
                for (int j = 0; j < preBufList.size(); j++){
                    bufBufList.add(preBufList.get(j));
                    maxlen++;
                }
                //construct a bytebuffer from the final buffer list
                ByteBuffer newConcat = ByteBuffer.allocate(maxlen);
                for(int j = 0; j < bufBufList.size(); j++){
                    newConcat.put(bufBufList.get(j));
                }

                //this is the byte array that will be sent as a packet
                byte[] newBuf = newConcat.array();
                senderMassiveList.add(newBuf);
                tcpOut.writeInt(endSequence);
                tcpOut.reset();
                endSequence++;


                // send the response to the client at "address" and "port"
                address = packet.getAddress();
                port = packet.getPort();
                packet = new DatagramPacket(newBuf, newBuf.length, address, port);
                socket.send(packet);

                //if a non max length packet has been sent, then this
                //while loop will end after this iteration
                if(packet.getLength() < 576){
                    moreWriting = false;
                }

            } catch (Exception e) {
                System.out.println("exception in moreWriting");
                e.printStackTrace();
            }
        }
        sendMissing();
        while(numMissing>0){
            sendMissing();
        }
        System.out.println("done sending!");
    }

    public void sendMissing(){
        boolean flag =false;
        List<Integer> missingList = new ArrayList<Integer>();
        //missing convention: recv how many missing, then recv list
        try{
            numMissing = 0;
            numMissing = tcpIn.readInt();

            if(numMissing > 0){
                Object temp = tcpIn.readObject();
                missingList = (List<Integer>) temp;
                int test=0;
                for(int i = 0 ; i < missingList.size(); i++){
                    int thisPacketIndexInMassiveList = missingList.get(i);
                    byte[] toSend = senderMassiveList.get(thisPacketIndexInMassiveList);
                    byte[] buf = new byte[576];

                    flag=false;
                    socket.setSoTimeout(2000);

                    if(flag){

                    } else{
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.receive(packet);

                        DatagramPacket missingPacket = new DatagramPacket(toSend, toSend.length, address, port);
                        socket.send(missingPacket);
                        test++;
                    }

                }
            } else {
                return;
            }
        }catch(SocketTimeoutException se){
            numMissing = 0;
            flag = true;
        }catch(Exception e){
            System.out.println("exception in send missing");
            System.out.println(e);
        }
    }

    //read the data from the file into the byte list
    public void readNextByteBuf(List<Byte> preBufList) throws Exception {
        //read data into this buf(required by fileinputstream)
        byte[] resultBuf = new byte[572];
        int i = 0;
        int j=0;
        try{
                i = fis.read();
                resultBuf[0] = (byte)i;
                //if i == then EOF has been reached
                while(i != -1) {
                    //first byte already read, fill the rest of the buffer
                    i = fis.read(resultBuf, 1, 571);
                    //if i < 571, then we reached EOF this iteration and a smaller buffer is needed
                    if(i < 571){
                        //add only changed bytes to byte list
                        for(int k = 0; k < i+1; k++){
                            preBufList.add(resultBuf[k]/*smallerResultBuf[k]*/);
                        }
                        //not EOF, add contents of whole array to the byte list
                    }else if (i == 571){
                        while(j < 572){
                            preBufList.add(resultBuf[j]);
                            j++;
                        }
                    }
                    //System.out.println("sout i: "+(i+1));
                    return;
                }

        }catch(Exception e){
            moreWriting = false;
            System.out.println("exception in readnextbyte");
            System.out.println(e);
        }

        return;
    }
}
