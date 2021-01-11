import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioReceiver extends Voip{
    SourceDataLine sourceDataLine;
    DatagramSocket dSocket;
    AudioFormat audioFormat;

    byte[] buff;

    AudioReceiver() throws IOException {
        dSocket = new DatagramSocket(8001);
        buff = new byte[4000];
        audioFormat = getAudioFormat();
    }

    /**
     * Receive audio data and write to the sourceDataLine.
     * @throws IOException
     * @throws LineUnavailableException
     */
    void Start() throws IOException, LineUnavailableException {
        boolean lastPacket = false;
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
        while (!lastPacket) {

            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            dSocket.receive(packet);

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
                            sourceDataLine.write(audioData, 0, audioData.length);
                        }
                    }
                    //Block and wait for internal buffer of the data line to empty.
                    sourceDataLine.drain();
                }
            });
            t.start();

        }
    }
}
