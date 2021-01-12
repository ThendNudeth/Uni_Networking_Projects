public class SearchFile {
    String input;
    String[] index;

    public SearchFile(String input, String[] index) {
        this.input = input;
        this.index = index;
    }

    public String[] testForMatch(int tolerance){
        Levenshtein x = new Levenshtein();
        String[] results = new String[index.length];

        for(int i = 0; i < index.length; i++) {
            int distance = x.levenshtein(input, index[i].substring(0, index[i].lastIndexOf('.')));
            System.out.println("Distance between "+
                    input+" and "+index[i]+" :"+distance);
            if(distance <= tolerance) {
                results[i] = index[i];
            } else {
                results[i] = "empty";
            }
        }
        return results;
    }
}
