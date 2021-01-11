//import javax.sound.sampled.LineUnavailableException;
//import java.io.IOException;
//import java.io.ObjectOutputStream;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.util.Scanner;
//
//public class VoiceNoteSender {
//    Socket tSocket;
//    InetAddress sendToIp;
//    VoiceNoteRecorder recorder;
//
//    VoiceNoteSender() throws IOException{
//        sendToIp = InetAddress.getLocalHost();
//        tSocket = new Socket(sendToIp, 8000);
//        recorder = new VoiceNoteRecorder();
//    }
//
//    /**
//     * The initial TCP connection which synchronises comms.
//     * @return True if connection successful.
//     * @throws IOException
//     */
//    boolean Connect() throws IOException{
//        Scanner in = new Scanner(tSocket.getInputStream());
//        String message = in.nextLine();
//        System.out.println("connection status: "+message);
//        if (message.equals("Connected!")) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    void Send() throws IOException, LineUnavailableException {
//        Thread timer = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//
//                }
//                recorder.keepRecording = false;
//                recorder.signalPlayback = true;
//            }
//        });
//        timer.start();
//        byte[] recording = recorder.recordVoiceNote();
//        VoiceNote voiceNote = new VoiceNote(recording);
//        ObjectOutputStream objectOutputStream = new ObjectOutputStream(tSocket.getOutputStream());
//        objectOutputStream.writeObject(voiceNote);
//    }
//
//    public static void main(String[] args) throws IOException, LineUnavailableException{
//        VoiceNoteSender sender = new VoiceNoteSender();
//        sender.Send();
//
//
//
//    }
//}
