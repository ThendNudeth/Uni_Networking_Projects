import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.PublicKey;

public class BasicReceiver {

    Socket rSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    FileOutputStream fos;
    BufferedOutputStream bos;

    InetAddress senderAdress;

    int chunkSize;
    long numChunks;
    long finalChunkSize;
    long fileSize;
    String fileName;

    double currProgress;
    double timeDiff = 0;
    boolean keepDownloading = true;

    DHEncryption dhe;
    PublicKey otherPK;

    public BasicReceiver(InetAddress address, int port) throws IOException, InterruptedException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException {

        dhe = new DHEncryption();

        senderAdress = address;
        System.out.println("BR instantiated");
        boolean flag = false;
        while (!flag) {
            try {
                rSocket = new Socket(address, port);
                flag = true;

            }catch(ConnectException e) {
                flag = false;
            }
        }

        out = new ObjectOutputStream(rSocket.getOutputStream());
        in = new ObjectInputStream(rSocket.getInputStream());

        System.out.println("connected");

//        recvChunkData();
//        recvFile();
    }

    public void recvChunkData() throws IOException, ClassNotFoundException {

        otherPK = (PublicKey)in.readObject();
        System.out.println("otherpk: "+otherPK);
        dhe.setReceiverPublicKey(otherPK);
        System.out.println("mypk: "+dhe.getPublicKey());
        out.writeObject(dhe.getPublicKey());
        out.flush();

        fileName = (String)in.readObject();
        chunkSize = in.readInt();
        numChunks = in.readLong();
        finalChunkSize = in.readLong();
        fileSize = chunkSize*numChunks + finalChunkSize;

        System.out.println("_________________");
        System.out.println("filename: "+fileName);
        System.out.println("chunkSize: "+chunkSize);
        System.out.println("numChunks: "+numChunks);
        System.out.println("finalChunkSize: "+finalChunkSize);
        System.out.println("" +
                "" +
                "fileSize: "+fileSize);
        System.out.println("_________________");
    }

    public void recvFile() throws IOException, InterruptedException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        try{
            long i = numChunks;
            double numRecvd = 0;
            currProgress = 0;

            String newPath = fileName.substring(11, (fileName.length() - 4));
            String extension = fileName.substring(fileName.lastIndexOf('.'));

            System.out.println(newPath);
            System.out.println(extension);

            newPath = "./../Files/output_"+newPath+extension;

            fos = new FileOutputStream(newPath);
            bos = new BufferedOutputStream(fos);

            double startTime = System.currentTimeMillis();
            while(i > 1) {
                recvChunk(chunkSize);
                i--;
                numRecvd++;
                currProgress = numRecvd/(double)numChunks;
                String printNum = String.format("%.1f", getProgress()*100);
                System.out.println("progress: "+printNum+"%");
            }
            recvChunk((int)finalChunkSize);
            double stopTime = System.currentTimeMillis();
            timeDiff = stopTime-startTime;
            System.out.println("down: "+timeDiff);
            currProgress = 1;
            System.out.println("progress: 100%");
        }catch(SocketException e){
            discMe();
        }
    }

    public void recvChunk(int chunkSize) throws IOException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {

        byte[] recvByte, decrypted;


        TCPChunk thisChunk = (TCPChunk)in.readObject();
        recvByte = thisChunk.getData();

        decrypted = dhe.decryptMessage(recvByte);


            out.writeBoolean(keepDownloading);
            out.flush();

            bos.write(decrypted, 0, decrypted.length);
            bos.flush();


    }

    public double getProgress() {
        return currProgress;
    }

    public void pauseDownload() {
        keepDownloading = false;
    }

    public void resumeDownload() throws IOException {
        keepDownloading = true;
        out.writeBoolean(true);
        out.flush();
    }

    public void discMe() throws IOException {
        out.close();
        in.close();
        rSocket.close();
    }
}
