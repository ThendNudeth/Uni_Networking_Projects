import javax.sound.sampled.LineUnavailableException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class VoiceNoteReceiver {
    ServerSocket listener;
    Socket tSocket;

    VoiceNoteReceiver() throws IOException {
        listener = new ServerSocket(8000);
    }

    /**
     * The initial TCP connection which synchronises comms.
     * @throws IOException
     */
    void Connect() throws IOException{
        boolean connected = false;
        while (!connected) {
            tSocket = listener.accept();
//            PrintWriter out = new PrintWriter(tSocket.getOutputStream(), true);
            System.out.println("Connected!");
            connected = true;

        }
    }

    byte[] Receive() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(tSocket.getInputStream());
        VoiceNote voiceNote = (VoiceNote) objectInputStream.readObject();
        return voiceNote.getAudioData();

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, LineUnavailableException {
        VoiceNoteRecorder player = new VoiceNoteRecorder();
        VoiceNoteReceiver receiver = new VoiceNoteReceiver();
        receiver.Connect();
        byte[] recording = receiver.Receive();
        player.playVoiceNote(recording);
    }

}
