import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SimpleAudioCatcher {
    static final long captureTime = 2000;
    static String inputFilePath = "Pizza Time.wav";
    File inputFile;

    File outputFile = new File("out.wav");
    AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

    AudioFormat audioFormat;
    TargetDataLine targetDataLine;

    public SimpleAudioCatcher() throws IOException,
            UnsupportedAudioFileException {
        inputFile = new File(inputFilePath);
        /* The line of code below is atypical, normally we would setup
        the format like below:

        float sampleRate = 16000;
        int sampleSizeInBits = 8;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
                                             channels, signed, bigEndian);
         */
        audioFormat = AudioSystem.getAudioInputStream(inputFile).getFormat();
    }

    public void start() throws LineUnavailableException, IOException {

        targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
        targetDataLine.open(audioFormat);
        targetDataLine.start();

        System.out.println("Start capturing...");

        AudioInputStream audioInputStream = new AudioInputStream(targetDataLine);
        AudioSystem.write(audioInputStream, fileType, outputFile);

    }

    public void end() {
        targetDataLine.stop();
        targetDataLine.close();
        System.out.println("Finished");
    }

    public static void main(String[] args) throws IOException,
            UnsupportedAudioFileException, LineUnavailableException {
        SimpleAudioCatcher catcher = new SimpleAudioCatcher();

        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(captureTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                catcher.end();

            }
        });
        timer.start();
        catcher.start();



    }
}
