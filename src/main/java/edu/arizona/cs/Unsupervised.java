package edu.arizona.cs;

import org.apache.lucene.document.Document;
import org.apache.xpath.operations.Bool;

import javax.print.Doc;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

public class Unsupervised {
    String questionFile = "";
    Hashtable<String, ArrayList<Double>> wordMap = null;

    public Unsupervised(String qName){
        this.questionFile = qName;
        this.wordMap = new Hashtable<String, ArrayList<Double>>();
    }

    public ArrayList<Double> findVectorForWord(String word) {
        ArrayList<Double> sList = new ArrayList<Double>();
        try {
            String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.toString());
            URL obj = new URL("http://localhost:8080/" + encodedWord);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            //        con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // print result
                String s = response.toString().replace("\"", "");
                String[] cLineSplit = s.split("\\s+");
                for(String ce : cLineSplit) {
                    sList.add(Double.parseDouble(ce));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(sList.size() == 0) {
            for (int i = 0; i < 300; ++i) {
                sList.add(0.0);
            }
        }
        return  sList;
    }

//    public void readGloveFile() {
//        try {
//            BufferedReader inputScanner = new BufferedReader(new FileReader(this.gloveFile));
////            File file = new File(this.gloveFile);
////            Scanner inputScanner = new Scanner(file);
//            int lineCount = 0;
//            Boolean isFirstWord;
//            ArrayList<Double> iList = null;
//            this.wordMap.put("unknown", new ArrayList<Double>());
//            String firstWord = "(unknown)";
//
//            String cLine = inputScanner.readLine();
//            while (cLine != null) {
////            while (inputScanner.hasNextLine()) {
////                String cLine = inputScanner.nextLine();
//                String[] cLineSplit = cLine.split("\\s+");
//                isFirstWord = true;
//                iList = new ArrayList<Double>();
//                for(String ce : cLineSplit) {
//                    if(lineCount == 0) {
//                        if(isFirstWord) {
//                            isFirstWord = false;
//                        } else {
//                            iList.add(Double.parseDouble(ce));
//                        }
//                    } else {
//                        if(isFirstWord) {
//                            firstWord = ce;
//                            isFirstWord = false;
//                        } else {
//                            iList.add(Double.parseDouble(ce));
//                        }
//                    }
//                }
//                this.wordMap.put(firstWord, iList);
//                cLine = inputScanner.readLine();
//                lineCount += 1;
////                if(lineCount > 5000) break;
//            }
//            inputScanner.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    private ArrayList<Double> findVectorForWord(String word) {
//        ArrayList<Double> ret = null;
//        ret = this.wordMap.get(word);
//        if(ret == null) ret = this.wordMap.get("(unknown)");
//        return ret;
//    }
    // it will do a + b and store into a
    private void addVectors(ArrayList<Double> a, ArrayList<Double> b) {
        for (int i = 0; i < a.size(); ++i) {
            a.set(i, a.get(i) + b.get(i));
        }
    }

    private ArrayList<Double> findVectorForQuery(String query) {
        ArrayList<Double> iList = new ArrayList<Double>();
        for (int i = 0; i < 300; ++i) {
            iList.add(0.0);
        }
        int wordCount = 0;
        String[] wordSplit = query.split("\\s+");
        for(String word : wordSplit) {
            if(word.length() == 0)continue;
            this.addVectors(iList, this.findVectorForWord(word));
            wordCount++;
        }
        for (int i = 0; i < 300; ++i) {
            iList.set(i, iList.get(i) / wordCount);
        }
        return iList;
    }

    private ArrayList<Double> findVectorForDoc(Document doc) {
        String docContent = doc.get("d_content");
        String[] sentenceSplit = docContent.split("\\.");
        int sentenceCount = 0;
        int wordCount = 0;
        ArrayList<Double> iList = new ArrayList<Double>();
        for (int i = 0; i < 300; ++i) {
            iList.add(0.0);
        }
        for(String sentence : sentenceSplit) {
            String[] wordSplit = sentence.split("\\s+");
            for(String word : wordSplit) {
                if(word.length() == 0 || word.equalsIgnoreCase("category") || word.equalsIgnoreCase("category:"))continue;
                this.addVectors(iList, this.findVectorForWord(word));
                wordCount++;
                if(wordCount >= 300) {
                    sentenceCount = 11; // break the outer loop also
                    break;
                }
            }
            sentenceCount++;
            if(sentenceCount>=10)break;
        }
        for (int i = 0; i < 300; ++i) {
            iList.set(i, iList.get(i) / wordCount);
        }
        return iList;
    }

    private double calcNorm(ArrayList<Double> sList) {
        double ans = 0;
        for (int i = 0; i < 300; ++i) {
            ans += (sList.get(i) * sList.get(i));
        }
        return Math.sqrt(ans);
    }

    private double calcCosineSimilarity(ArrayList<Double> qList, ArrayList<Double> dList) {
        double ans = 0;
        for (int i = 0; i < 300; ++i) {
            ans += (qList.get(i) * dList.get(i));
        }
        double dNorm = this.calcNorm(dList);
        return ans / dNorm;
    }

    public String findAnswerWithNN(String query, List<Document> nnDocs) {
        boolean isFirst = true;
        double maxScore = 0.0;
        String maxDocTitle = "";
        ArrayList<Double> qList = this.findVectorForQuery(query);
        for(Document d : nnDocs) {
            ArrayList<Double> dList = this.findVectorForDoc(d);
            double cos = this.calcCosineSimilarity(qList, dList);
            if(isFirst) {
                maxScore = cos;
                maxDocTitle = d.get("docid");
                isFirst = false;
            } else if(maxScore < cos) {
                maxScore = cos;
                maxDocTitle = d.get("docid");
            }
        }
        return maxDocTitle;
    }

}
