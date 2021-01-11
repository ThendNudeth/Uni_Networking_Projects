import java.io.*;

public class Sender {

    public Sender() {

    }
    public static void main(String[] args) throws IOException {
        Sender sender = new Sender();
        sender.startThread();
    }

    public void startThread() throws IOException {
        new SenderThread().start();
    }
}