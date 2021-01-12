import java.util.ArrayList;

public class NatTable extends Thread{

    public static ArrayList<String[]> natTable;
    long timeoutSeconds;
    long elapsed = 0;

    public NatTable(long timeoutSeconds){
        this.timeoutSeconds = timeoutSeconds;
        timeoutSeconds = timeoutSeconds*1000;
        natTable = new ArrayList<String[]>();
    }

    public void enterEntry(String fromIPinternal, String toIPexternal) {
        String fromToArr[] = {fromIPinternal, toIPexternal, (System.currentTimeMillis()+"")};
        natTable.add(fromToArr);
        System.out.println("entry entered: "+natTable.get(natTable.size()-1)[0]+ "|"+ natTable.get(natTable.size()-1)[1]);
    }


    public String locateByPortEntry(String toPort) {
        for (int i = 0; i < natTable.size(); i++) {
            String thisPort = natTable.get(i)[0].substring(natTable.get(i)[0].indexOf(":")+1);
            if(thisPort.equals(toPort)) {
                natTable.get(i)[2] = System.currentTimeMillis()+"";
                return natTable.get(i)[0];
            }
        }
        return "notfound";
    }

    public String locateExternalEntry(String fromIp) {

        for (int i = 0; i < natTable.size(); i++) {
            if(natTable.get(i)[0].equals(fromIp)) {
                natTable.get(i)[2] = System.currentTimeMillis()+"";
                return natTable.get(i)[1];
            }
        }
        return "notfound";
    }

    public void startTime(long timeoutSeconds) {
        NatTable t = new NatTable(timeoutSeconds);
        t.start();
    }

    public void testTimeoutEntry(long timeoutMilliSeconds) {
        for (int i = 0; i < natTable.size(); i++) {

            long lastUpdated = Long.parseLong(natTable.get(i)[2]);
            long elapsedSinceUpdate = System.currentTimeMillis() - lastUpdated;

            if(elapsedSinceUpdate >= timeoutMilliSeconds) {
                printNatTable();
                System.out.println("removing...");
                natTable.remove(i);
                printNatTable();
                if(natTable.size() == 0){
                    System.out.println("NAT table empty");
                }
            }
        }
    }

    public  void printNatTable() {
        for (int i = 0; i < natTable.size(); i++) {
            System.out.println("internal: "+natTable.get(i)[0]+
                    " external: " +natTable.get(i)[1]+
                    " timeUpdated: "+natTable.get(i)[2]);
        }
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        timeoutSeconds = timeoutSeconds*1000;
        long end;
        while(true) {
            end = System.currentTimeMillis();
            elapsed = end - start;
            if(elapsed >= timeoutSeconds) {
                testTimeoutEntry(timeoutSeconds);
                elapsed = 0;
                start = System.currentTimeMillis();
            }
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
