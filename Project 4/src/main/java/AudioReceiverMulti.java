import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class AudioReceiverMulti extends Voip{
//    SourceDataLine sourceDataLine;
    AudioFormat audioFormat;
    InetAddress sendToIp;

    MulticastSocket mSocket;
    final int test[] = new int[2];

    byte[] buff;

    AudioReceiverMulti() throws IOException {
        mSocket = new MulticastSocket(8002);
        buff = new byte[8000];
        audioFormat = getAudioFormat();
    }

    public void setCallEnd() throws IOException {
        mSocket.leaveGroup(sendToIp);
        mSocket.disconnect();
        mSocket.close();

    }

    /**
     * Receive audio data and write to the sourceDataLine.
     * @throws IOException
     * @throws LineUnavailableException
     */
    void Start(String ip) throws IOException, LineUnavailableException {
        sendToIp = InetAddress.getByName(ip);
        mSocket.joinGroup(sendToIp);


        boolean lastPacket = false;
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

        SourceDataLine sourceDataLine0 = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine0.open(audioFormat);
        sourceDataLine0.start();

        SourceDataLine sourceDataLine1 = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine1.open(audioFormat);
        sourceDataLine1.start();

        SourceDataLine sourceDataLine2 = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine2.open(audioFormat);
        sourceDataLine2.start();

        SourceDataLine sourceDataLine3 = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine3.open(audioFormat);
        sourceDataLine3.start();


        test[0] = 0;
        test[1] = 0;


        while (!lastPacket) {

            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            mSocket.receive(packet);

            byte[] audioData = packet.getData();
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);

            AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat,
                    audioData.length/audioFormat.getFrameSize());

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    int bytesRead = 0;
                    //Keep looping until the input read method returns -1 for empty stream.
                    while(true){
                        try {
                            if (!((bytesRead = audioInputStream.read(audioData, 0, audioData.length)) != -1)) break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(bytesRead > 0){
                            //Write data to the internal buffer of the data line where it will be delivered to the speaker.
                            if (test[0] == 0) {
                                sourceDataLine0.write(audioData, 0, audioData.length);
                                test[0] = 1;
                            } else if (test[0] == 1) {
                                sourceDataLine1.write(audioData, 0, audioData.length);
                                test[0] = 2;
                            }else if (test[0] == 2) {
                                sourceDataLine2.write(audioData, 0, audioData.length);
                                test[0] = 3;
                            }else if (test[0] == 3) {
                                sourceDataLine3.write(audioData, 0, audioData.length);
                                test[0] = 0;
                            }

                            if (test[1] == 1) {
                                System.out.println("want to leave");
                                try {
                                    mSocket.leaveGroup(sendToIp);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                    //Block and wait for internal buffer of the data line to empty.
//                    sourceDataLine0.drain();
//                    sourceDataLine1.drain();
//                    sourceDataLine2.drain();
//                    sourceDataLine3.drain();

                }
            });
            t.start();

        }
        //
    }
}
