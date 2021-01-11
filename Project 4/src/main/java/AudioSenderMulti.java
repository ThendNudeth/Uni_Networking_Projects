import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;

public class AudioSenderMulti extends Voip{
    /**
     * The line from which audio data is captured.
     */
    TargetDataLine line;
    /**
     * The address to which to send the audio data.
     */
    InetAddress sendToIp;

    boolean test = false;

    MulticastSocket mSocket;

    byte[] buff;

    /**
     * A boolean that ends a while loop. See "Start()".
     */
    boolean keepSending;

    AudioSenderMulti() throws IOException {

        mSocket = new MulticastSocket(8002);

        buff = new byte[8000];
    }

    public void setCallEnd() throws IOException {
        mSocket.leaveGroup(sendToIp);
        mSocket.disconnect();
        mSocket.close();
        test = true;
    }

    /**
     * Capture the sound and send it.
     */
    void start(String ip) throws IOException {
        try {
            sendToIp = InetAddress.getByName(ip);
            mSocket.setLoopbackMode(true);
            mSocket.joinGroup(sendToIp);


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
                            buff.length, sendToIp, 8002);
                    mSocket.send(packet);
//                    System.out.println("sent");
                }
                if(test) {
                    mSocket.leaveGroup(sendToIp);
                    break;
                }
            }

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {

        }finally {
            //mSocket.leaveGroup(sendToIp);
        }
    }
}
