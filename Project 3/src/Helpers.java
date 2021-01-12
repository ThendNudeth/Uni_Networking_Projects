import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Helpers {

    public static String genIP(boolean isInternal) throws IOException {
        // Generate a random IP to store in the address pool.
        File file = new File("IPaddresses.txt");
        boolean exists = false;

        String ipS = "";
        int ipI;
        for (int i = 0; i < 4; i++) {
            if (isInternal) {
                ipI = (int)Math.floor(Math.random()*255);
                if (i == 0) {
                    ipS = ipS + "10";
                } else if (i > 0) {
                    ipS = ipS + String.format(".%d", ipI);
                }
            } else {
                ipI = (int)Math.floor(Math.random()*255);
                if (i == 0) {
                    ipS = ipS + "192.168";
                } else if (i > 1) {
                    ipS = ipS + String.format(".%d", ipI);
                }
            }

        }

        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            if (scanner.nextLine().equals(ipS)) {
                exists = true;
                break;
            }
        }

        if (exists) {
            ipS = genIP(isInternal);
        } else {
            FileWriter fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            PrintWriter pr = new PrintWriter(br);
            pr.println(ipS);
            pr.close();
            br.close();
            fr.close();
        }

        return ipS;
    }

    public static String genMAC() throws IOException{
        // Generate a random MAC to store in the address pool.
        File file = new File("MACaddresses.txt");
        boolean exists = false;

        String macS = "";
        int macI;
        for (int i = 0; i < 6; i++) {
            macI = (int)(Math.random()*255);
//            System.out.println(macI);
            if (i == 0) {
                macS = macS + Integer.toHexString(macI);
            }else {
                macS = macS + ":" + Integer.toHexString(macI);
            }
        }

        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            if (scanner.nextLine().equals(macS)) {
                exists = true;
                break;
            }
        }

        if (exists) {
            macS = genMAC();
        } else {
            FileWriter fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            PrintWriter pr = new PrintWriter(br);
            pr.println(macS);
            pr.close();
            br.close();
            fr.close();
        }


        return macS;
    }
}
