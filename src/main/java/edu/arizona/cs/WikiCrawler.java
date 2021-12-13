package edu.arizona.cs;

import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.net.URL;
import java.lang.Integer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiCrawler extends Thread {
    int docCount = 0;
    int categoryCount = 0;
    QueryEngine qe = null;
    String candidateFileName = "";
    String wikiPath = "";
    NLPAnalyzer wnlp = null;

    public WikiCrawler(QueryEngine q, String wp, String fName){
        this.qe = q;
        this.wikiPath = wp;
        this.candidateFileName = fName;
        this.wnlp = new NLPAnalyzer();
    }

    private void handlePreviousDoc(String title, String content, String l_content, String cat) {
        if(title == null || content == null || title.length() == 0 || content.replaceAll("[\\n\\t ]", "").length() == 0) return;
        this.docCount += 1;
        if(cat != null) {
            this.categoryCount += 1;
        }
        if((this.docCount%50) >= 49) {
            System.out.println(Integer.toString(this.docCount) + " doc indexed for title: " + this.candidateFileName);
        }
        this.qe.addDoc(title, content, l_content);
    }

    private String removeUnnecessaryThings(String content) {
        content = content.replaceAll("\\[ref\\].*?\\[/ref\\]", "");
        content = content.replaceAll("\\[/ref\\]", "");
        content = content.replaceAll("\\[tpl\\].*?\\[/tpl\\]", "");
        content = content.replaceAll("\\[/tpl\\]", "");
        return content;
    }

    private String handlePreviousSubtexts(String subtitle, String content, Boolean isLemmatize) {
        if(subtitle == null) subtitle = "";
        if(content == null) content = "";
        if(subtitle.length() + content.replaceAll("[\\n\\t ]", "").length() == 0) return "";
        String compactedContent = this.removeUnnecessaryThings(subtitle + content);
        if(compactedContent.length() ==0 || compactedContent.replaceAll("[\\n\\t ]", "").length() == 0) return "";
        if(isLemmatize) {
            compactedContent = this.wnlp.analyzeDocContent(compactedContent);
        }
        return compactedContent;
    }

    public void run() {
        File[] files = new File(this.wikiPath).listFiles();
        String docTitle = null;
        String subTitle = null;
        String content = null;
        StringBuilder lemmatizedContent = new StringBuilder();
        StringBuilder plainContent = new StringBuilder();
        String categories = null;
        System.out.println("Starting thread with file: " + this.candidateFileName);

        for(File f: files) {
            String p = f.getName();
            if(!p.equals(this.candidateFileName))continue;
            try (Scanner inputScanner = new Scanner(f)) {
                int titleCount = 0;
                this.docCount = 0;
                while (inputScanner.hasNextLine()) {
                    String cLine = inputScanner.nextLine();
                    if(cLine.startsWith("[[")) {
                        titleCount += 1;
                        lemmatizedContent.append(this.handlePreviousSubtexts(subTitle, content, true));
                        plainContent.append(this.handlePreviousSubtexts(subTitle, content, false));
                        handlePreviousDoc(docTitle, plainContent.toString(), lemmatizedContent.toString(), categories);
                        docTitle = cLine.substring(2, cLine.length()-2);
                        content = "";
                        lemmatizedContent.setLength(0);
                        plainContent.setLength(0);
                        subTitle = "";
                        categories = null;
                    } else if(cLine.startsWith("CATEGORIES")) {
                        // build a category only index
                        categories = cLine.substring(12);
                        content += cLine; // add the categories for now, since it will increase the query likelihood
                    } else if(cLine.startsWith("#")) {
                        // do nothing for now
                        continue;
                    } else if(cLine.startsWith("=")) {
                        lemmatizedContent.append(this.handlePreviousSubtexts(subTitle, content, true));
                        plainContent.append(this.handlePreviousSubtexts(subTitle, content, false));
                        Pattern pattern = Pattern.compile("[a-zA-Z0-9 ]+");
                        Matcher matcher = pattern.matcher(cLine);
                        subTitle = "";
                        if (matcher.find()) {
                            subTitle += matcher.group() + '\n'; // add new line here, or else lemmatizer will think it inside a sentence
                        }
                        content = "";
                    } else {
                        // handle single sentence
                        content += " " + cLine;
                    }
                }
                lemmatizedContent.append(this.handlePreviousSubtexts(subTitle, content, true));
                plainContent.append(this.handlePreviousSubtexts(subTitle, content, false));
                handlePreviousDoc(docTitle, plainContent.toString(), lemmatizedContent.toString(), categories);
                System.out.println(p + " has title count " + Integer.toString(titleCount));
                lemmatizedContent.setLength(0);
                plainContent.setLength(0);
                inputScanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}