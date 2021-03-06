package dist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Spamfilter {
    private final static String             FILE_ROOT_PATH          = "src/main/files/";
    private final static String             HAM_MAILS_LEARNING      = FILE_ROOT_PATH + "ham-anlern/";
    private final static String             SPAM_MAILS_LEARNING     = FILE_ROOT_PATH + "spam-anlern/";
    private final static String             HAM_MAILS_CALIBRATION   = FILE_ROOT_PATH + "ham-kallibrierung/";
    private final static String             SPAM_MAILS_CALIBRATION  = FILE_ROOT_PATH + "spam-kallibrierung/";
    private final static String             HAM_MAILS_TEST          = FILE_ROOT_PATH + "ham-test/";
    private final static String             SPAM_MAILS_TEST         = FILE_ROOT_PATH + "spam-test/";

    private static File[]                   listOfHamLearningFiles;
    private static File[]                   listOfSpamLearningFiles;
    private static File[]                   listOfHamCalibrationFiles;
    private static File[]                   listOfSpamCalibrationFiles;
    private static File[]                   listOfHamtestFiles;
    private static File[]                   listOfSpamtestFiles;

    private static Map<String, Word>        Words                   = new HashMap<>();
    private static int                      spamMailsCount          = 0;
    private static int                      hamMailsCount           = 0;
    private static float                    alpha                   = 0.1f;

    public static void main(String[] args) {

        Spamfilter s = new Spamfilter();

        /***********************************
        Read All Files in the folders and store them in an Array of File Objects
        ************************************/

        s.readAllFiles();

        /***********************************
         * Aufgabe 2) a)
         * Learning Process:
         * Go Through all the Files/Mails in the folder, read them as Strings, chop up the Strings, export all distinct
         * words as a Set, look up words and increase the count in the hashmap if the word has already appeared or add them
         * to the hashmap and last but not least, increment the right count to keep track on how many files have been
         * analysed.
        ************************************/

        s.learnSpamWordsFromAFolder(listOfSpamLearningFiles,   Words);
        s.learnHamWordsFromAFolder(listOfHamLearningFiles,    Words);

        /***********************************
         * Aufgabe 2) b)
         * Calculate Spam Probability for all Words
         ***********************************/

        s.calculateAllProbabilities(Words);

        /***********************************
         * Aufgabe 2) c)
         * Calibration Process:
         * Calibrate Spam filter and play around with the alpha variable
         ************************************/

        s.calibrate();
        System.out.println("****************");

        /************************************
         * Result: 0.1 seems to be a nice working threshold value
         ************************************/

        /************************************
         * Aufgabe 2) d)
         * Console is printing out how many Spam & Ham mails are wrongly classified
         ************************************/

        System.out.println("Amount of wrong classified Ham Mails");
        System.out.println((s.classifyScore(listOfHamtestFiles, Words, false, 0.80f) / listOfHamtestFiles.length * 100) + "%");
        System.out.println("Amount of wrong classified Spam Mails");
        System.out.println((s.classifyScore(listOfSpamtestFiles, Words, true, 0.80f) / listOfSpamtestFiles.length * 100) + "%");
        System.out.println("out of " + (listOfHamtestFiles.length + listOfSpamtestFiles.length) + " Mails");

        System.out.println("The End");
    }

    public Spamfilter(){}

    private float calibrate() {
        score(listOfSpamCalibrationFiles, Words, true);
        score(listOfHamCalibrationFiles, Words, false);
        float thres = 0f;

        for (int i = 1; i < 100; i++){
            thres = (float) i / 100f;
            System.out.println("thres: " + thres + " score: " + (classifyScore(listOfSpamCalibrationFiles, Words, true, thres) + classifyScore(listOfHamCalibrationFiles, Words, false, thres)));
        }
        System.out.println("of " + (listOfSpamCalibrationFiles.length + listOfHamCalibrationFiles.length) + " Mails");
        return 0f;
    }

    private float score(File[] folder, Map<String, Word> words, boolean b) {
        float abw = 0f;
        if (b){
            for (int i = 0; i < folder.length; i++) {
                abw += Math.abs(1f - pOfSpam(folder[i], words));
            }
        } else {
            for (int i = 0; i < folder.length; i++) {
                abw += Math.abs(0f - pOfSpam(folder[i], words));
            }
        }
        return abw;
    }

    private float classifyScore(File[] folder, Map<String, Word> words, boolean b, float thres) {
        float abw = 0f;
        if (b){
            /*
            In Case of Spam
             */
            for (int i = 0; i < folder.length; i++) {
                if (pOfSpam(folder[i], words) < thres) abw++;
            }
        } else {
            /*
            In Case of Ham
            */
            for (int i = 0; i < folder.length; i++) {
                if (pOfSpam(folder[i], words) > thres) abw++;
            }
        }
        return abw;
    }

    public void calculateAllProbabilities(Map<String, Word> map){
        for (Map.Entry<String, Word> element : map.entrySet()) {
            float spamRate  = element.getValue().getcOfSpam()   / (float) spamMailsCount;
            float hamRate   = element.getValue().getcOfHam()    / (float) hamMailsCount;

            /************************************
             * If a word didn't appear in either the Spam or Ham Mails list, the Rate stays 0.
             * Because 0 times n always results in 0, this behaviour must be changed with the following if/else structure.
             * If the Rate of Spam or Ham is 0, it's going to be replaced with the alpha value divided by the total
             * count of mails. If the probability is either to high or to low, it gets corrected as well, because
             * otherwhise the float number type would run out of space. This method is still accurate enough.
             ************************************/

            if(spamRate == 0f) {
                spamRate = alpha / (float) spamMailsCount;
            }
            if(hamRate == 0f) {
                hamRate = alpha / (float) hamMailsCount;
            }
            if(spamRate + hamRate > 0){
                element.getValue().setProbOfSpam(spamRate / (spamRate + hamRate));
            }
            if(element.getValue().getProbOfSpam() < 0.01f){
                element.getValue().setProbOfSpam(0.01f);
            }
            else if(element.getValue().getProbOfSpam() > 0.99f){
                element.getValue().setProbOfSpam(0.99f);
            }
        }
    }

    public float pOfSpam(File f, Map<String, Word> map) {
        String s = readFromTextFile(f);
        float wOfSpam   = 1f;
        float wOfHam    = 1f;
        Set<String> set = split(s);
        for (String element : set) {
            if (map.get(element) != null) {
                wOfSpam     = wOfSpam   * map.get(element).getProbOfSpam();
                wOfHam      = wOfHam    * (1.0f - map.get(element).getProbOfSpam());
            }
        }
        float uBruch = wOfSpam + wOfHam;
        if(uBruch == 0) {
            return 0f;
        }
        return wOfSpam / uBruch;
    }

    public void readAllFiles(){
        File folder = new File(HAM_MAILS_LEARNING);
        listOfHamLearningFiles = folder.listFiles();

        folder = new File(SPAM_MAILS_LEARNING);
        listOfSpamLearningFiles = folder.listFiles();

        folder = new File(HAM_MAILS_CALIBRATION);
        listOfHamCalibrationFiles = folder.listFiles();

        folder = new File(SPAM_MAILS_CALIBRATION);
        listOfSpamCalibrationFiles = folder.listFiles();

        folder = new File(HAM_MAILS_TEST);
        listOfHamtestFiles = folder.listFiles();

        folder = new File(SPAM_MAILS_TEST);
        listOfSpamtestFiles = folder.listFiles();
    }

    public void learnSpamWordsFromAFolder(File[] folder, Map<String, Word> map){
        spamMailsCount = folder.length;
        for (int i = 0; i < folder.length; i++) {
            if (folder[i].isFile()) {
                String s = readFromTextFile(folder[i]);
                if(s != null) {
                    Set<String> set = split(s);
                    for (String element : set) {
                        if (map.containsKey(element)) {
                            Word w = map.get(element);
                            w.incrementSpam();
                            map.put(element, w);
                        } else {
                            Word w = new Word();
                            w.setcOfSpam(1);
                            map.put(element, w);
                        }
                    }
                }
            }
        }
    }

    public void learnHamWordsFromAFolder(File[] folder, Map<String, Word> map){
        hamMailsCount = folder.length;
        for (int i = 0; i < folder.length; i++) {
            if (folder[i].isFile()) {
                String s = readFromTextFile(folder[i]);
                if(s != null) {
                    Set<String> set = split(s);
                    for (String element : set) {
                        if (map.containsKey(element)) {
                            Word w = map.get(element);
                            w.incrementHam();
                            map.put(element, w);
                        } else {
                            Word w = new Word();
                            w.setcOfHam(1);
                            map.put(element, w);
                        }
                    }
                }
            }
        }
    }

    public Set<String> split(String str){
        return Stream.of(str.split(" |\n"))
            .map (elem -> new String(elem))
            .collect(Collectors.toSet());
    }

    public String readFromTextFile(File file){
        try {
            return Files.readString(file.toPath(), StandardCharsets.ISO_8859_1);
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
