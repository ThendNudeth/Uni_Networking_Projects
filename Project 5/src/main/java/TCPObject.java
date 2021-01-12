import java.io.File;
import java.io.Serializable;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class TCPObject implements Serializable {
    private String type;
    public static final String MESSAGE_TYPE = "message";
    public static final String SEARCH_TYPE = "search";
    public static final String SEARCHRESULTS_TYPE = "searchresults";
    public static final String LOGIN_TYPE = "login";
    public static final String CLIENT_CHANGE_TYPE = "client_change";
    public static final String DOWNLOAD_REQ = "download_req";
    public static final String CHUNK = "chunk";
    public static final String DHEXCHANGE = "dh_exchange";
    public static final String DISC_TYPE = "Fok_jou";
    public static final String KILL_TYPE = "kill";
    public static final String BOOL_TYPE = "bool";

    TCPObject(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}

class TCPChunk extends TCPObject {
    private byte[] data;
    private int chunkSize;

    TCPChunk(String type) {
        super(type);
    }

    TCPChunk(String type, byte[] data, int chunkSize) {
        super(type);
        this.data = data;
        this.chunkSize = chunkSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}

class TCPDownloadReq extends TCPObject {
    private byte[] key;
    private String downloadingUser;
    private String uploadingUser;
    private String fileName;
    private String address = "";

    TCPDownloadReq(String type) {
        super (type);
    }

    TCPDownloadReq(String type, byte[] key, String downloadingUser,
                   String uploadingUser, String fileName) {
        super (type);
        this.key = key;
        this.downloadingUser = downloadingUser;
        this.uploadingUser = uploadingUser;
        this.fileName = fileName;
    }

    public byte[] getKey() {
        return key;
    }

    public String getDownloadingUser() {
        return downloadingUser;
    }

    public String getUploadingUser() {
        return uploadingUser;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }
}

class TCPEncryption extends TCPObject {
    PublicKey publicKey;
    String toUser;
    String fromUser;
    int phase;
    String userWithFile;
    String fileName;

    TCPEncryption(String type) {super(type);}

    TCPEncryption(String type, PublicKey publicKey, String toUser, String fromUser,
                  int phase, String userWithFile, String fileName) {
        super(type);
        this.publicKey = publicKey;
        this.toUser = toUser;
        this.fromUser = fromUser;
        this.phase = phase;
        this.userWithFile = userWithFile;
        this.fileName = fileName;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public String getUserWithFile() {
        return userWithFile;
    }

    public void setUserWithFile(String userWithFile) {
        this.userWithFile = userWithFile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

class TCPMessage extends TCPObject {
    private String message;
    private String sender;
    private String receiver;
    private String clientIP = "";

    TCPMessage(String type) {
        super(type);
    }

    TCPMessage(String type, String message) {
        super(type);
        this.message = message;
    }

    TCPMessage(String type, String message, String sender, String receiver) {
        super(type);
        this.message = message;
        this.sender = sender;
        this.receiver = receiver;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }
}

class TCPSearch extends TCPObject {
    private String searchTerm;
    private String searcherName;

    TCPSearch(String type) {
        super(type);
    }

    TCPSearch(String type, String searchTerm, String searcherName) {
        super(type);
        this.searchTerm = searchTerm;
        this.searcherName = searcherName;
    }
    public String getSearchTerm() {
        return searchTerm;
    }

    public String getSearcherName() {return searcherName;}

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}

class TCPSearchResults extends TCPObject {
    private String searchTerm;
    private String searcherName;
    private String[] results;
    private String senderName;

    TCPSearchResults(String type) {
        super(type);
    }

    TCPSearchResults(String type, String searchTerm, String searcherName, String senderName, String[] results) {
        super(type);
        this.searchTerm = searchTerm;
        this.searcherName = searcherName;
        this.results = results;
        this.senderName = senderName;
    }
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSearcherName() {return searcherName;}

    public String getSenderName() {return senderName;}

    public String[] getResults() {
        return results;
    }

    public void setResults(String[] results) {
        this.results = results;
    }
}

class TCPArrayList extends TCPObject {
    ArrayList list;
    String disconnected;

    TCPArrayList(String type, ArrayList list) {
        super(type);
        this.list = list;
    }

    public void setDisconnected(String disconnected) {
        this.disconnected = disconnected;
    }

    public String getDisconnected() {
        return disconnected;
    }

    public ArrayList getList() {
        return list;
    }
}
