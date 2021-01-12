import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.PublicKey;
import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class BasicSender {

    ServerSocket serverSocket;
    Socket sSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    BufferedInputStream bis;
    FileInputStream fis;

    File toSend;
    int chunkSize;
    long fileSize;
    long numChunks;
    long finalChunkSize;
    String filename;

    double currProgress = 0;
    double timeDiff = 0;
    boolean keepSending = true;
    boolean kill = false;

    DHEncryption dhe;
    PublicKey otherPK;

    public BasicSender(String filename, int chunkSize, int port) throws IOException, ClassNotFoundException, InterruptedException {

        System.out.println("BS instantiated");

        dhe = new DHEncryption();

        this.chunkSize = chunkSize;
        this.filename = filename;

        serverSocket = new ServerSocket(port);
        sSocket = serverSocket.accept();
        out = new ObjectOutputStream(sSocket.getOutputStream());
        in =  new ObjectInputStream(sSocket.getInputStream());
        System.out.println("connected");
        System.out.println("_________________");

        toSend = new File(filename);
        fileSize = toSend.length();

        numChunks = calcNumChunks();

//        sendChunkData();
//        sendFile();
    }

    public long calcNumChunks() {
        long count, fileSizeTemp;
        count = 0;
        fileSizeTemp = fileSize;

        while(fileSizeTemp > 0) {
            if(fileSizeTemp<=chunkSize) {
                finalChunkSize = fileSizeTemp;
            }
            fileSizeTemp = fileSizeTemp - chunkSize;
            count++;
        }

        return count;
    }

    public void sendChunkData() throws IOException, ClassNotFoundException {

        //public key
        System.out.println(dhe.getPublicKey());
        out.writeObject(dhe.getPublicKey());
        out.flush();
        otherPK = (PublicKey) in.readObject();
        System.out.println("otherpk: "+otherPK);
        dhe.setReceiverPublicKey(otherPK);
        //filename
        out.writeObject(filename);
        out.flush();
        //chunksize
        out.writeInt(chunkSize);
        out.flush();
        //numchunks
        out.writeLong(numChunks);
        out.flush();
        //finalchunksize
        out.writeLong(finalChunkSize);
        out.flush();
    }

    public void sendFile() throws IOException, InterruptedException, ClassNotFoundException {
        try{
            long i = numChunks;
            double numSent = 0;

            fis = new FileInputStream(toSend);
            bis = new BufferedInputStream(fis);

            double startTime = System.currentTimeMillis();
            while(i > 1) {
                byte[] sendArray =  new byte[chunkSize];
                if(keepSending) {

                    sendChunk(sendArray);
                    i--;
                    numSent++;
                    currProgress = numSent/(double)numChunks;
                    String printNum = String.format("%.1f", getProgress()*100);
                    System.out.println("progress: "+printNum+"%");
                }else {
                    System.out.println("paused");
                    while(!keepSending) {
                        //wait for resume
                        keepSending = in.readBoolean();

                    }
                    System.out.println("resumed");
                }
            }
            byte[] sendArray =  new byte[(int)finalChunkSize];
            sendChunk(sendArray);
            double stopTime = System.currentTimeMillis();
            timeDiff = stopTime-startTime;
            System.out.println("up: "+timeDiff);
            currProgress = 1;
            System.out.println("progress: 100%");
        }catch(SocketException e) {
            discMe();
        }
    }

    public void sendChunk(byte[] sendArray) throws IOException, InterruptedException, ClassNotFoundException {
        byte[] encrypted;

        bis.read(sendArray, 0, sendArray.length);

        encrypted = dhe.encryptMessage(sendArray);

        TCPChunk thisChunk = new TCPChunk(TCPObject.CHUNK, encrypted, sendArray.length);

        out.writeObject(thisChunk);
        out.flush();

        keepSending = in.readBoolean();
    }

    public double getProgress() {
        return currProgress;
    }

    public void discMe() throws IOException {
        out.close();
        in.close();
        sSocket.close();
        serverSocket.close();
    }
}
