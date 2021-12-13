package edu.arizona.cs;

import java.util.Properties;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.StanfordCoreNLPClient;
import edu.stanford.nlp.util.CoreMap;

public class NLPAnalyzer {
    private StanfordCoreNLP pipeline;
    private StanfordCoreNLPClient clientPipeline;

    public NLPAnalyzer() {
        // the following compiles
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        this.clientPipeline = new StanfordCoreNLPClient(props, "http://localhost", 9000, 2);
    }

    public String analyzeQueryCatString(String text) {
        Annotation annotation= new Annotation(text);
        this.clientPipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        StringBuilder catSentence = new StringBuilder();
        for(CoreMap sentence: sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lm = token.get(CoreAnnotations.LemmaAnnotation.class);
                if(pos.equals("NN") || pos.equals("NNP") || pos.equals("NNS") || pos.equals("NNPS"))
                    catSentence.append(' ' + lm.toLowerCase());
            }
            catSentence.append(' ');
        }
        return catSentence.toString();
    }

    private Boolean isNotCharacter(String c) {
        String regex = "[,.:;!@#$%^&*\\(\\){}\\[\\]=+\\-?/]";
        return (c.matches(regex));
    }

    public String analyzeQueryString(String text, Boolean isLemmatize) {
        Annotation annotation= new Annotation(text);
        this.clientPipeline.annotate(annotation);
        Boolean isInsideQuote = false;

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        StringBuilder lemmatisedSentence = new StringBuilder();
        for(CoreMap sentence: sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                if(word.equals("\"")) {
                    if(isInsideQuote) isInsideQuote = false;
                    else isInsideQuote = true;
                }
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if(pos.equals("IN") || pos.equals("PRP") || pos.equals("PRP$") || pos.equals("POS")) if(!isInsideQuote) continue;
                String lm = token.get(CoreAnnotations.LemmaAnnotation.class);
                if(!isLemmatize || isInsideQuote) {
                    lm = word;
                }
                if(!this.isNotCharacter(lm))
                    lemmatisedSentence.append(' ' + lm);
            }
            lemmatisedSentence.append(' ');
        }
        return lemmatisedSentence.toString();
    }

    public String analyzeDocContent(String text) {
        try {
            Annotation annotation = new Annotation(text);
            this.clientPipeline.annotate(annotation);

            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            StringBuilder lemmatisedSentence = new StringBuilder();
            for (CoreMap sentence : sentences) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    //                // this is the text of the token
                    //                String word = token.get(CoreAnnotations.TextAnnotation.class);
                    //                // this is the POS tag of the token
                    //                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    //                // this is the NER label of the token
                    //                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    String lm = token.get(CoreAnnotations.LemmaAnnotation.class);
                    lemmatisedSentence.append(' ' + lm);
                }
                lemmatisedSentence.append(' ');
            }
            return lemmatisedSentence.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}