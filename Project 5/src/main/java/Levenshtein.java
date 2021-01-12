public class Levenshtein{
    public static int levenshtein(String charSequence1, String charSequence2){

        if(charSequence1.length() == 0) {
            //if String 1 is empty
            return charSequence2.length();
        }else if(charSequence2.length() == 0) {
            //if String 2 is empty
            return charSequence1.length();
        } else {
            //check for lcs if firstchar is the same
            if(charSequence1.charAt(0) == charSequence2.charAt(0)) {
                return levenshtein(charSequence1.substring(1), charSequence2.substring(1));
            }

            //try a: change first char of s1 to first char of s2
            int subFirstChar = levenshtein(charSequence1.substring(1), charSequence2.substring(1));
            //try b: remove first char of s1
            int remFirstChars1 = levenshtein(charSequence1, charSequence2.substring(1));
            //try c: remove first char of s2
            int remFirstChars2 = levenshtein(charSequence1.substring(1), charSequence2);

            //determine minimum of the three possiblities
            if(subFirstChar > remFirstChars1){
                subFirstChar = remFirstChars1;
            }
            if(subFirstChar > remFirstChars2){
                subFirstChar = remFirstChars2;
            }

            return subFirstChar + 1;
        }
    }
}
