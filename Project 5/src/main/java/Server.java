import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    /**
     * Runs the server. When a client connects, the server spawns a new thread to do
     * the servicing and immediately returns to listening. The application limits the
     * number of threads via a thread pool (otherwise millions of clientNames could cause
     * the server to run out of resources by allocating too many threads).
     */
    private static ArrayList<String> clientNames = new ArrayList<>();
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap();
    Boolean hasUpdated;

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        try (ServerSocket listener = new ServerSocket(8000)) {
            System.out.println("The server is running...");
            ExecutorService pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new ClientHandler(listener.accept(), server));
            }
        }
    }

    public void updateClientList(String disconnected) throws IOException {
        System.out.println("Clients currently connected to the server:");
        for (String client : clients.keySet()) {
            ClientHandler tempClientHandler = clients.get(client);

            System.out.println(tempClientHandler.myName+": "+ tempClientHandler.socket);

            TCPArrayList list = new TCPArrayList(TCPObject.CLIENT_CHANGE_TYPE, clientNames);
            list.setDisconnected(disconnected);
            ObjectOutputStream tempOutputsream = tempClientHandler.outputStream;

            tempOutputsream.writeObject(list);
            tempOutputsream.flush();
            tempOutputsream.reset();

        }
    }

    public void sendMessage(TCPMessage message) throws IOException {
        if (message.getReceiver().equals("")) {
            for (String client : clients.keySet()) {

                ClientHandler tempClientHandler = clients.get(client);

                ObjectOutputStream tempOutputStream = tempClientHandler.outputStream;

                tempOutputStream.writeObject(message);
                tempOutputStream.flush();
                tempOutputStream.reset();
                System.out.println(message.getSender() + " sent a message.");
            }
        } else {
            for (String client : clients.keySet()) {
                if (client.equals(message.getSender())||client.equals(message.getReceiver())) {
                    ClientHandler tempClientHandler = clients.get(client);

                    ObjectOutputStream tempOutputStream = tempClientHandler.outputStream;

                    tempOutputStream.writeObject(message);
                    tempOutputStream.flush();
                    tempOutputStream.reset();
                    System.out.println(message.getSender() + " sent a message to " + message.getReceiver());
                }
            }
        }
    }

    public void sendSearch(TCPSearch searched) throws IOException {
        for (String client : clients.keySet()) {
            if (!(client.equals(searched.getSearcherName()))) {
                ClientHandler tempClientHandler = clients.get(client);

                ObjectOutputStream tempOutputStream = tempClientHandler.outputStream;

                tempOutputStream.writeObject(searched);
                tempOutputStream.flush();
                tempOutputStream.reset();
            }
        }
    }

    public void sendSearchResults(TCPSearchResults results) throws IOException {
        for (String client : clients.keySet()) {
            if ((client.equals(results.getSearcherName()))) {
                ClientHandler tempClientHandler = clients.get(client);

                ObjectOutputStream tempOutputStream = tempClientHandler.outputStream;

                tempOutputStream.writeObject(results);
                tempOutputStream.flush();
                tempOutputStream.reset();

                break;
            }
        }
    }

    public void sendDownloadReq(TCPDownloadReq dReq) throws IOException {
        for (String client : clients.keySet()) {
            if ((client.equals(dReq.getDownloadingUser()))) {
                ClientHandler tempClientHandler = clients.get(client);
                //dReq.setAddress(tempClientHandler.socket.getInetAddress().getHostAddress());
                ObjectOutputStream tempOutputStream = tempClientHandler.outputStream;

                tempOutputStream.writeObject(dReq);
                tempOutputStream.flush();
                tempOutputStream.reset();

                break;
            }
        }
    }

    public void sendExchange(TCPEncryption thisEnc) throws IOException {
        for (String client : clients.keySet()) {
            if ((client.equals(thisEnc.toUser))) {
                ClientHandler tempClientHandler = clients.get(client);

                ObjectOutputStream tempOutputStream = tempClientHandler.outputStream;

                tempOutputStream.writeObject(thisEnc);
                tempOutputStream.flush();
                tempOutputStream.reset();

                break;
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        Server serverMain;
        String myName;
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;

        boolean nameAssigned = false;
        TCPObject objectReceived;
        TCPMessage messageToSend;
        TCPMessage messageReceived;
        TCPSearch searchedFor;
        TCPSearchResults returnedSearchResults;
        TCPDownloadReq downloadReq;
        TCPEncryption dhExchange;
        TCPArrayList listToSend;


        ClientHandler(Socket socket, Server serverMain) throws IOException {
            this.serverMain = serverMain;
            this.socket = socket;
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket);

            try {
                while (!nameAssigned) {
                    objectReceived = (TCPObject) inputStream.readObject();

                    if (objectReceived.getType().equals(TCPObject.LOGIN_TYPE)) {
                        messageReceived = (TCPMessage) objectReceived;
                        String nameSubmission = messageReceived.getMessage();
                        System.out.println("The socket: " + socket + " is attempting to connect with the name: "
                                + nameSubmission);
                        if (!clientNames.contains(nameSubmission)) {
                            nameAssigned = true;
                            clientNames.add(nameSubmission);
                            clients.put(nameSubmission, this);
                            myName = nameSubmission;
                            System.out.println("The name: " + myName + " has been accepted");
                            messageToSend = new TCPMessage(TCPObject.LOGIN_TYPE, "accepted");
                            messageToSend.setClientIP(socket.getInetAddress().getHostAddress());
                            outputStream.writeObject(messageToSend);
                            serverMain.updateClientList("");
                        } else {
                            messageToSend = new TCPMessage(TCPObject.LOGIN_TYPE, "not accepted");
                            outputStream.writeObject(messageToSend);
                        }
                    }
                }

                while (true) {
                    objectReceived = (TCPObject) inputStream.readObject();
                    if (objectReceived.getType().equals(TCPObject.MESSAGE_TYPE)) {
                        messageReceived = (TCPMessage) objectReceived;
                        serverMain.sendMessage(messageReceived);
                    } else if(objectReceived.getType().equals(TCPObject.SEARCH_TYPE)) {
                        searchedFor = (TCPSearch) objectReceived;
                        serverMain.sendSearch(searchedFor);
                    }else if(objectReceived.getType().equals(TCPObject.SEARCHRESULTS_TYPE)) {
                        returnedSearchResults = (TCPSearchResults) objectReceived;
                        serverMain.sendSearchResults(returnedSearchResults);
                    }else if(objectReceived.getType().equals(TCPObject.DOWNLOAD_REQ)) {
                        downloadReq = (TCPDownloadReq) objectReceived;
                        serverMain.sendDownloadReq(downloadReq);
                    }else if(objectReceived.getType().equals(TCPObject.DHEXCHANGE)) {
                        dhExchange = (TCPEncryption) objectReceived;
                        serverMain.sendExchange(dhExchange);
                    } else if (objectReceived.getType().equals(TCPObject.DISC_TYPE)) {
                        messageReceived = (TCPMessage) objectReceived;
                        System.out.println("Disconnecting: " + messageReceived.getMessage());
                        clientNames.remove(messageReceived.getMessage());
                        clients.remove(messageReceived.getMessage());
                        serverMain.updateClientList(messageReceived.getMessage());
                        listToSend = new TCPArrayList(TCPObject.CLIENT_CHANGE_TYPE, clientNames);
                        System.out.println("Check message: "+messageReceived.getMessage());
                        listToSend.setDisconnected(messageReceived.getMessage());
                        outputStream.writeObject(listToSend);
                    }

                }

            } catch (Exception e) {
//                System.out.println("Error:" + socket);
            } finally {
                try { socket.close(); } catch (IOException e) {}
                System.out.println("Closed: " + socket);
            }
        }
    }
}