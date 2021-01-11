import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class VeryBasicServer {
    ArrayList<String> clientsConnected;

    public static void main(String[] args) throws Exception {
        try (ServerSocket listener = new ServerSocket(59898)) {
            System.out.println("The server is running...");
            ExecutorService pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new ClientConnection(listener.accept()));
            }
        }
    }

    private static class ClientConnection implements Runnable {
        private Socket socket;

        public ClientConnection(Socket socket) {
            this.socket = socket;

        }

        public void run() {


        }
    }

}
