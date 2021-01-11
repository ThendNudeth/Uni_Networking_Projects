import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

public class VeryBasicClient {
    public void runMe(String toIP) {

        Thread t0 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioReceiver receiver = new AudioReceiver();
                    receiver.Start();
                } catch (IOException e) {

                } catch (LineUnavailableException e) {

                }
            }
        });
        t0.start();
        String ip = toIP;
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioSender sender = new AudioSender();
                    sender.start(ip);
                } catch (IOException e) {

                }
            }
        });
        t1.start();
    }

    public static void main(String[] args) {
        VeryBasicClient vbc = new VeryBasicClient();
        vbc.runMe(args[0]);
    }
}
