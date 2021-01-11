import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

public class VoiceNoteRecorder extends Voip{
    /**
     * The line from which audio data is captured.
     */
    TargetDataLine line;

    /**
     * The address to which to send the audio data.
     */
    InetAddress sendToIp;

    Socket tSocket;
//    byte[] audioData;
    byte[] tempBuff;
    boolean keepRecording;
    boolean signalPlayback;

    AudioFormat format;
    SourceDataLine sourceDataLine;

    VoiceNoteRecorder() throws IOException {
        sendToIp = InetAddress.getLocalHost();
        tempBuff = new byte[1024];
        signalPlayback = false;


    }

    byte[] recordVoiceNote() throws LineUnavailableException, IOException {
        format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();   // start capturing

        System.out.println("Start capturing...");
        byte[] audioData = new byte[0];

        keepRecording = true;
        while (keepRecording) {
            int bytesRead = line.read(tempBuff, 0, tempBuff.length);
            if (bytesRead > 0) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(audioData);
                outputStream.write(tempBuff);
                audioData = new byte[audioData.length+tempBuff.length];
                audioData = outputStream.toByteArray();
//                    System.out.println("sent");
            }
        }
        System.out.println("finished recording.");
        return audioData;
    }

    void sendVoiceNote() {

    }

    void playVoiceNote(byte[] audioData) throws LineUnavailableException, IOException{
        format = getAudioFormat();
        InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);

        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format,
                audioData.length/format.getFrameSize());
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(format);
        sourceDataLine.start();

        int bytesRead;
        //Keep looping until the input read method returns -1 for empty stream.
        while((bytesRead = audioInputStream.read(audioData, 0, audioData.length)) != -1){
            if(bytesRead > 0){
                //Write data to the internal buffer of the data line where it will be delivered to the speaker.
                sourceDataLine.write(audioData, 0, bytesRead);
            }
        }
    }
    /**
     * Closes the target data line to finish capturing and recording
     */
    void finish() {
        line.stop();
        line.close();
        System.out.println("Finished");
    }

    public static void main(String[] args) throws IOException, LineUnavailableException, InterruptedException {
        byte[] recording;
        VoiceNoteRecorder voiceNoteRecorder = new VoiceNoteRecorder();
        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {

                }
                voiceNoteRecorder.keepRecording = false;
                voiceNoteRecorder.signalPlayback = true;
            }
        });
        timer.start();
        recording = voiceNoteRecorder.recordVoiceNote();
        while (voiceNoteRecorder.signalPlayback) {
            voiceNoteRecorder.playVoiceNote(recording);
            voiceNoteRecorder.signalPlayback = false;
        }


    }
}
