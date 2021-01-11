import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SimpleAudioPlayer {
    static String filePath;
    static File audioFile;
    AudioInputStream audioInputStream;
    AudioFormat audioFormat;

    Clip clip;

    SourceDataLine sourceDataLine;

    // There are two ways to handle audio input streams:
    // Clip or SourceDataLine.
    public SimpleAudioPlayer() throws IOException,
            UnsupportedAudioFileException {

        // Create AudioInputStream object which converts an audio file into a
        // stream.
        audioFile = new File(filePath);
        audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        audioFormat = audioInputStream.getFormat();

    }

    public static void main(String args[]) throws IOException,
            UnsupportedAudioFileException, LineUnavailableException {
        boolean playViaClip = false;

        filePath = "Pizza Time.wav";
        SimpleAudioPlayer audioPlayer = new SimpleAudioPlayer();

        if (playViaClip) {
            audioPlayer.playClip();
            while (true) {

            }
        } else {
            audioPlayer.playLine();
        }


    }

    public void playLine() throws IOException,
            LineUnavailableException {
        sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();

        int bufSize = 1024;
        byte[] bytesbuf = new byte[bufSize];
        int bytesRead;

        while ((bytesRead = audioInputStream.read(bytesbuf)) != -1) {
//            System.out.println(bytesRead);
            sourceDataLine.write(bytesbuf, 0, bytesRead);
        }

        // The thing still works without this bottom part,
        // it's just tidying up.
        sourceDataLine.drain();
        sourceDataLine.close();
        audioInputStream.close();
    }

    public void recordAudio() {

    }

    // You can ignore this method. We won't be implementing this.
    public void playClip() throws IOException,
            LineUnavailableException {

        // Create clip reference, open it with the data from audioInputStream
        // and loop it continuously. (Clip.LOOP_CONTINUOUSLY = -1)
        clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        clip.loop(Clip.LOOP_CONTINUOUSLY);

        clip.start();
    }
}
