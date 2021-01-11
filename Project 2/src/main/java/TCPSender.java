import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPSender{
    ServerSocket serverSocket;
    Socket socket;
    OutputStream outputStream;
    File file;
    int len;


    public TCPSender() throws IOException {
        serverSocket = new ServerSocket(8002);
        socket = serverSocket.accept();
        outputStream = socket.getOutputStream();

    }

    public void setFile(File file) {
        this.file = file;
    }

    public void run() throws IOException {
        //setup file
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        //Read Filename into array
        byte [] readName;
        readName = file.getName().getBytes();

        //Read File Contents into contents array
        byte[] readFile;
        len = (int)file.length();
        bis.read(readFile = new byte[(int)len]);

        //write to outputstream
        //Send through length of filename
        outputStream.write(file.getName().length());
        outputStream.flush();

        //Send through filename
        outputStream.write(readName);
        outputStream.flush();

        outputStream.write(len);
        outputStream.flush();

        outputStream.write(readFile);
        outputStream.flush();

        socket.close();
        serverSocket.close();

    }

    public static void main(String[] args) throws Exception {

        ServerSocket serverSock = new ServerSocket(8002);
        Socket socket = serverSock.accept();
        OutputStream os = socket.getOutputStream();
        long len;

        //setup file
        File thisFile = new File("one-liners.txt");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream("one-liners.txt"));

        //Read File Contents into contents array
        byte[] readFile;
        len = thisFile.length();
        bis.read(readFile = new byte[(int)len]);

        //write to outputstream
        os.write(readFile);
        os.flush();

        socket.close();
        serverSock.close();
    }
}
