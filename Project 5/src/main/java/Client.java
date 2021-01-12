import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable{
    private Set<String> files = new HashSet<>();
    Socket socket;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;

    BasicReceiver basicReceiver;
    BasicSender basicSender;

    DHEncryption dhe;

    TCPObject objectReceived;
    TCPMessage messageToSend;
    TCPMessage messageReceived;
    TCPSearch searchTermSent;
    TCPSearch searchTermReceived;
    TCPSearchResults searchResults;
    TCPArrayList listReceived;

    String myName;
    String myAddress;
    static int nextPort = 8101;
    String messageWall = "";
    ArrayList<String> resultWall = new ArrayList<>();
    ArrayList<String> clients = new ArrayList<>();

    Boolean loginPhaseCompleted = false;
    Boolean clientUpdated = false;
    Boolean messagesUpdated = false;
    Boolean searchResultsUpdated = false;
    Boolean receiving = false;
    Boolean sending = false;
    Boolean running = true;

    Client(String address) throws IOException {
        this.socket = new Socket(address, 8000);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());

    }

    public static void main(String[] args) throws Exception {
        Client client = new Client("127.0.0.1");
        client.run();
    }

    public ArrayList<String> getClients() {
        return this.clients;
    }

    Boolean submitNickname(String nickname) throws IOException, ClassNotFoundException {
        Boolean accepted = false;

        messageToSend = new TCPMessage(TCPObject.LOGIN_TYPE, nickname);
        outputStream.writeObject(messageToSend);

        objectReceived = (TCPObject) inputStream.readObject();

        if (objectReceived.getType().equals(TCPObject.LOGIN_TYPE)) {
            messageReceived = (TCPMessage) objectReceived;
            if (messageReceived.getMessage().equals("accepted")) {
                myName = nickname;
                myAddress = messageReceived.getClientIP();
                accepted = true;
                loginPhaseCompleted = true;
            } else if (messageReceived.getMessage().equals("not accepted")) {
                accepted = false;
            }

        }
        return accepted;
    }

    void sendMessage(String message, String receiver) throws IOException {
        messageToSend = new TCPMessage(TCPObject.MESSAGE_TYPE, message, myName, receiver);
        outputStream.writeObject(messageToSend);
    }

    void searchFor(String searchTerm) throws IOException {
        searchTermSent = new TCPSearch(TCPObject.SEARCH_TYPE, searchTerm, myName);
        outputStream.writeObject(searchTermSent);
        outputStream.flush();
    }

    void preRequestExchange(String userWithFile, String requestedFile) throws InterruptedException, IOException, ClassNotFoundException {
        dhe = new DHEncryption();
        TCPEncryption newKeyExchange = new TCPEncryption(TCPObject.DHEXCHANGE,
                dhe.getPublicKey(), userWithFile, myName, 1, userWithFile, requestedFile);

        outputStream.writeObject(newKeyExchange);
        outputStream.flush();

    }

    void sendKeyBack(TCPEncryption thisEnc) throws IOException {
        dhe = new DHEncryption();
        dhe.setReceiverPublicKey(thisEnc.getPublicKey());

        TCPEncryption newKeyExchange = new TCPEncryption(TCPObject.DHEXCHANGE,
                dhe.getPublicKey(), thisEnc.getFromUser(), myName, 2,
                thisEnc.userWithFile, thisEnc.fileName);

        outputStream.writeObject(newKeyExchange);
        outputStream.flush();
    }

    void reqDownload(String userWithFile, String requestedFile) throws IOException, ClassNotFoundException, InterruptedException {
        Keys newKey = new Keys();
        byte[] myKeyThisReq = newKey.genNewKey();
        System.out.println("key: "+myKeyThisReq);

        byte[] encrypted = dhe.encryptMessage(myKeyThisReq);
        TCPDownloadReq newReq= new TCPDownloadReq(TCPObject.DOWNLOAD_REQ, encrypted,
                userWithFile, myName, requestedFile);
        newReq.setAddress(myAddress);

        outputStream.writeObject(newReq);
        outputStream.flush();

        checkSentKey(newReq, myKeyThisReq, myKeyThisReq.length);
    }

    public ArrayList<String> refreshFiles() {
        String[] filesArr = new File("./../Files/").list();
        ArrayList<String> files = new ArrayList<String>();
        for (int i = 0; i < filesArr.length; i++) {
            files.add(i, filesArr[i]);
        }
        return files;
    }

    public void checkSentKey(TCPDownloadReq thisReq, byte[] keyISent, int len) throws IOException, ClassNotFoundException, InterruptedException {
        //I want to download the file, checking the key i sent with the request
        System.out.println("try tentative checkSentKey");
        ServerSocket ss = new ServerSocket(8101);
        Socket testKey;
        testKey = ss.accept();

        ObjectOutputStream ous = new ObjectOutputStream(testKey.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(testKey.getInputStream());
        System.out.println("opened tentative communications");


        byte[] key = new byte[len];
        ois.read(key, 0, key.length);

        System.out.println("check if i sent this key: "+key);

        if(Arrays.equals(key, keyISent)) {
            System.out.println("correct key");
            ous.writeBoolean(true);
            ous.flush();

            String raw = (String)ois.readObject();
            String downloadFromAddress = raw.split("\\|")[0];
            String downloadFromPort = raw.split("\\|")[1];
            nextPort = Integer.parseInt(downloadFromPort);
            System.out.println("port"+nextPort);

            ous.close();
            ois.close();
            testKey.close();
            ss.close();

            startBasicReceiver(downloadFromAddress, nextPort);
            System.out.println("BR started");
        }else {
            System.out.println("incorrect key");
            ous.writeInt(1);
        }
    }

    public void checkKey(TCPDownloadReq thisReq) throws IOException {
        //I own the file, checking the key i received with the download request
        System.out.println(thisReq.getAddress());
        Socket testKey = new Socket(thisReq.getAddress(), 8101);
        ObjectOutputStream ous = new ObjectOutputStream(testKey.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(testKey.getInputStream());
        System.out.println("opened tentative communications");

        byte[] decrypted = dhe.decryptMessage(thisReq.getKey());

        ous.write(decrypted, 0, decrypted.length);
        ous.flush();

        if(ois.readBoolean()) {
            System.out.println("recvd confirmation");
            nextPort++;
            ous.writeObject(myAddress+"|"+nextPort);
            ous.flush();

            System.out.println("port"+nextPort);
            startBasicSender(thisReq, nextPort);

            System.out.println("BS started");
            ous.close();
            ois.close();
            testKey.close();
        }
    }

    public void startBasicReceiver(String downloadFromAddress, int port) {

        Thread t1 = new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    basicReceiver = new BasicReceiver(InetAddress.getByName(downloadFromAddress), port);
                    receiving = true;
                    basicReceiver.recvChunkData();
                    basicReceiver.recvFile();
                }catch (IOException | InterruptedException | ClassNotFoundException e){
                    System.out.println(e);
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();

    }
     public void startBasicSender(TCPDownloadReq thisReq, int port) {

         Thread t0= new Thread(new Runnable(){
             @Override
             public void run() {
                 try{
                     basicSender = new BasicSender(("./../Files/"+thisReq.getFileName()), 102400, port);
                     sending = true;
                     basicSender.sendChunkData();
                     basicSender.sendFile();
                 }catch (IOException e){
                     System.out.println(e);
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 } catch (ClassNotFoundException e) {
                     e.printStackTrace();
                 }
             }
         });
         t0.start();
    }

    public void disconnectMe() throws IOException {
        if (sending) {
            basicSender.keepSending = false;
            basicSender.discMe();
        }
        if (receiving) {
            basicReceiver.keepDownloading = false;
            basicReceiver.discMe();
        }
        messageToSend = new TCPMessage(TCPObject.DISC_TYPE, myName);
        outputStream.writeObject(messageToSend);
    }

    @Override
    public void run() {
        while (running) {
            if (loginPhaseCompleted) {

                try {
                    /**
                     * Here we receive an object.
                     */
                    objectReceived = (TCPObject) inputStream.readObject();
                    System.out.println("objectReceived");

                    /**
                     * Here we decide what to do with it. Bunch of if's /if-else's.
                     * See the constants in TCPObject.
                     */
                    if (objectReceived.getType().equals(TCPObject.CLIENT_CHANGE_TYPE)) {
                        listReceived = (TCPArrayList) objectReceived;
                        System.out.println("client list received.");
                        if (listReceived.getDisconnected().equals(myName)) {
                            running = false;
                        } else {
                            clients = listReceived.getList();
                            clientUpdated = true;
                        }

                    } else if (objectReceived.getType().equals(TCPObject.MESSAGE_TYPE)) {
                        messageReceived = (TCPMessage) objectReceived;
                        messageWall = messageWall + messageReceived.getSender()+": "+messageReceived.getMessage() + "\n";
                        messagesUpdated = true;
                    } else if (objectReceived.getType().equals(TCPObject.SEARCH_TYPE)) {

                        System.out.println("received a search req");
                        searchTermReceived = (TCPSearch) objectReceived;
                        ArrayList <String>tempList = refreshFiles();
                        String[] myFiles = new String[tempList.size()];

                        for(int i = 0; i < tempList.size(); i++) {
                            myFiles[i] = tempList.get(i);
                        }

                        String[] rawResults;
                        ArrayList <String> processedResultList = new ArrayList<>();

                        SearchFile newSearch = new SearchFile(searchTermReceived.getSearchTerm(), myFiles);
                        //TODO: large int for all files,
                        // adjust tolerance to satisfaction,
                        // sort results by Levensthein number
                        int tolerance = (int)(0.75*searchTermReceived.getSearchTerm().length());
                        if (tolerance < 4) {
                            tolerance = 3;
                        }
                        if(searchTermReceived.getSearchTerm().equals("all")) {
                            tolerance = 1000;
                        }

                        System.out.println("tolerance: "+tolerance);
                        rawResults = newSearch.testForMatch(tolerance);


                        for(int i = 0; i < rawResults.length; i++) {
                            if(!rawResults[i].equals("empty")) {
                                System.out.println("valid result: "+rawResults[i]);
                                processedResultList.add(rawResults[i]);
                            }
                        }
                        String[] finalResults = new String[processedResultList.size()];
                        if(processedResultList.size() == 0) {
                            System.out.println("empty results");
                            finalResults = new String[1];
                            finalResults[0] = "no results found";
                        }
                        for(int i = 0; i < processedResultList.size(); i++) {
                            finalResults[i] = processedResultList.get(i);
                        }

                        searchResults = new TCPSearchResults(TCPObject.SEARCHRESULTS_TYPE,
                                searchTermReceived.getSearchTerm(),
                                searchTermReceived.getSearcherName(), myName,
                                finalResults);

                        outputStream.writeObject(searchResults);
                        outputStream.flush();


                    }else if (objectReceived.getType().equals(TCPObject.SEARCHRESULTS_TYPE)) {
                        TCPSearchResults resultObject = (TCPSearchResults) objectReceived;
                        String searchTermReceived  = resultObject.getSearchTerm();
                        String [] resultsRecvd = resultObject.getResults();
                        for (int i = 0; i < resultsRecvd.length; i ++){
                            resultWall.add(resultObject.getSenderName()+"|"+resultsRecvd[i]);
                        }
                        searchResultsUpdated = true;

                    }else if(objectReceived.getType().equals(TCPObject.DOWNLOAD_REQ)) {
                        System.out.println("received download req");

                        TCPDownloadReq thisReq = (TCPDownloadReq) objectReceived;
                        System.out.println(thisReq.getDownloadingUser()+" wants to download "
                                + thisReq.getFileName()+ " with key "+thisReq.getKey()
                                +" at "+ thisReq.getAddress());

                        checkKey(thisReq);
                    } else if (objectReceived.getType().equals(TCPObject.DHEXCHANGE)) {
                        System.out.println("key exchange received");

                        TCPEncryption thisEnc = (TCPEncryption) objectReceived;
                        if (thisEnc.getPhase() == 1) {
                            sendKeyBack(thisEnc);
                        } else if (thisEnc.getPhase() == 2) {
                            dhe.setReceiverPublicKey(thisEnc.getPublicKey());
                            reqDownload(thisEnc.userWithFile, thisEnc.fileName);
                        }
                    }

                    } catch (IOException e) {
                    System.out.println("The socket must now close.");
                    try {
                        running = false;
                        outputStream.close();
                        inputStream.close();
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            System.out.println("The socket must now close.");
            outputStream.close();
            inputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}