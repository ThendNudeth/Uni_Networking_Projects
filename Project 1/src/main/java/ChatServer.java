import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static ArrayList<String> names = new ArrayList<>();
    private static ArrayList<PrintWriter> writers = new ArrayList<>();
    static boolean invalidated = false;

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(8000)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Handler(Socket socket) {
            System.out.println("client connected.");
            this.socket = socket;
        }

        public void update() {
                for (PrintWriter writer : writers) {
                    writer.println("_UPDATE_START");
                    //System.out.println("WHAT");
                    for (String temp : names) {
                        writer.println("_UPDATE"+temp);
                        //System.out.println(temp+"heyheyhey");
                    }
                    //System.out.println("GOING");
                    writer.println("_END_UPDATE");
                }
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new Scanner(socket.getInputStream());

                out.println("SUBMIT_NAME");
                //System.out.println("SUBMIT_NAME");
                name = in.nextLine();
                //System.out.println(name);
                while (true) {
                    if (!names.contains(name)) {
                        names.add(name);
                        invalidated = true;
                        break;
                    } else {
                        out.println("INVALID_NAME");
                        //System.out.println("INVALID_NAME");
                        name = in.nextLine();
                        //System.out.println(name);
                    }
                }

                out.println("NAME_ACCEPTED " + name);
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");
                }
                writers.add(out);

                    update();


                while (true) {
                    String input = in.nextLine();
                    //whisper convention <identifier>@<toUser>@<fromUser>
                    if (input.startsWith("WHISPER@")) {
                        String toUser;
                        String fromUser;
                        toUser = input.substring(8, input.lastIndexOf('@'));
                        fromUser = input.substring(input.lastIndexOf('@')+1, input.indexOf(' '));
                        String content = input.substring(input.indexOf(' ')+1);

                        if (names.contains(toUser)) {
                            for (PrintWriter writer : writers) {
                                ///whisper convention <identifier>@<toUser>@<fromUser>
                                writer.println(input);
                            }
                        } else {
                            //if no such user exists
//                            for (PrintWriter writer : writers) {
//                                ///whisper convention <identifier>@<toUser>@<fromUser>
//                                writer.println("WHISPER@"+toUser+"@"+toUser+" "+fromUser+" no such user exists");
//                            }
                        }
                    }else {
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + input);
                            //System.out.println("MESSAGE " + name + ": " + input);
                            //System.out.println("Message sent from server" + input);
                        }
                    }
                }
            } catch (Exception e) {
                //System.out.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    update();
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                }
                try {
                    out.close();
                    in.close();
                    socket.close();
                } catch (IOException e) {}
            }
        }
    }
}