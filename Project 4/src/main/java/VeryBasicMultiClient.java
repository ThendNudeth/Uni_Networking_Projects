import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

public class VeryBasicMultiClient {
    AudioReceiverMulti receiver;
    AudioSenderMulti sender;
    Thread t0;
    Thread t1;
    public void runMe(String toIP) {
        String ip = toIP;
        t0 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receiver = new AudioReceiverMulti();
                    System.out.println("vbmc rip : "+ ip);
                    receiver.Start(ip);
                } catch (IOException e) {

                } catch (LineUnavailableException e) {

                }
            }
        });
        t0.start();

        t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sender = new AudioSenderMulti();
                    System.out.println("vbmc sip : "+ ip);
                    sender.start(ip);
                } catch (IOException e) {

                }
            }
        });
        t1.start();
    }

    public void endCall() throws IOException {
        receiver.setCallEnd();
        sender.setCallEnd();
//        t0.interrupt();
//        t1.interrupt();
    }
}

