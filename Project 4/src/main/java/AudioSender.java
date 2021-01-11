import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class AudioSender extends Voip{
    /**
     * The line from which audio data is captured.
     */
    TargetDataLine line;
    /**
     * The address to which to send the audio data.
     */
    InetAddress sendToIp;

    DatagramSocket dSocket;

    byte[] buff;

    /**
     * A boolean that ends a while loop. See "Start()".
     */
    boolean keepSending;

    AudioSender() throws IOException {

        dSocket = new DatagramSocket();
        buff = new byte[4000];
    }

    /**
     * Capture the sound and send it.
     */
    void start(String ip){
        try {
            sendToIp = InetAddress.getByName(ip);
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();   // start capturing

            System.out.println("Start capturing...");

            keepSending = true;
            while (keepSending) {
                int bytesRead = line.read(buff, 0, buff.length);
                if (bytesRead > 0) {
                    DatagramPacket packet = new DatagramPacket(buff,
                            buff.length, sendToIp, 8001);
                    dSocket.send(packet);
//                    System.out.println("sent");
                }
            }

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
