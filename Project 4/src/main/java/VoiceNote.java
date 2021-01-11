import java.io.Serializable;

public class VoiceNote implements Serializable {
    byte[] audioData;
    String fromHost;
    String toHost;

    VoiceNote(byte[] audioData, String fromHost, String toHost) {
        this.audioData = audioData;
        this.fromHost = fromHost;
        this.toHost = toHost;
    }

    byte[] getAudioData() {
        return this.audioData;
    }

    String getFromHost() {
        return this.fromHost;
    }

    String getToHost() {
        return  this.toHost;
    }

}
