import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Extension;

public class TCPReceiver {
    Socket socket;
    InputStream inputStream;
    FileOutputStream fileOutputStream;
    String fileName;
    String fileExtension;
    int bytesRead;
    InetAddress address;


    public TCPReceiver(String addressStr) throws IOException {
        bytesRead = 0;
        address = InetAddress.getByName(addressStr);
        socket = new Socket(address, 8002);
        inputStream = socket.getInputStream();
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void run() throws IOException {
        //Read the file extension
        long startTime = System.nanoTime();
        byte recvdfilenamelen = 0;
        byte[] recvdName = new byte[inputStream.read()];
        inputStream.read(recvdName);

        String name = new String(recvdName);
        System.out.println(name);
        int i = name.length()-1;
        while (name.charAt(i)!='.') {
            i--;
        }

        fileExtension = name.substring(i);
        System.out.println(fileExtension);

        fileOutputStream = new FileOutputStream(fileName+""+fileExtension);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        //Read the length of the file sent and assign it to a byte array.
        byte[] recvdbytes = new byte[inputStream.read()];

        while((bytesRead=inputStream.read(recvdbytes))!=-1)
            bufferedOutputStream.write(recvdbytes, 0, bytesRead);

        bufferedOutputStream.flush();
        long endTime = System.nanoTime();
        System.out.println("TCP elapsed (millisecond)time from send to write: "+(endTime-startTime)/1000000);
        socket.close();
        System.out.println("Receiver closed.");
    }


    public static void main(String[] args) throws Exception{
        int bytesRead = 0;
        Socket socket = new Socket("127.0.0.1", 8002);
        InputStream is = socket.getInputStream();

        FileOutputStream fos = new FileOutputStream("outputTCP.txt");
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] recvdbytes = new byte[50000];

        while((bytesRead=is.read(recvdbytes))!=-1)
            bos.write(recvdbytes, 0, bytesRead);

        bos.flush();
        socket.close();

    }
}