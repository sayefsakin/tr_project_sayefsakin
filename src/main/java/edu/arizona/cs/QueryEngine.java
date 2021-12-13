package edu.arizona.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.Scanner;


public class QueryEngine {
    boolean indexExists=false;

    Analyzer[] analyzer;
    Directory[] index;
    IndexWriter[] indexWriter;
    String[] indexLocation;

    int totalIndexes = 6;
    String indexPath = "";
    NLPAnalyzer nlp = null;
    // 0 - no stemming, no lemmatization, BM25
    // 1 - no stemming, lemmatization, BM25
    // 2 - stemming, no lemmatization, BM25
    // 3 - no stemming, no lemmatization, TFIDF
    // 4 - no stemming, lemmatization, TFIDF
    // 5 - stemming, no lemmatization, TFIDF


    public QueryEngine(String filePath){
        this.indexPath = filePath;
        this.nlp = new NLPAnalyzer();
        this.analyzer = new Analyzer[this.totalIndexes];
        this.index = new Directory[this.totalIndexes];
        this.indexWriter = new IndexWriter[this.totalIndexes];
        this.indexLocation = new String[this.totalIndexes];
    }

    private Boolean isChangeSimilarity(int indexSettings){
        return (indexSettings > 2);
    }

    private Boolean isLemmatize(int indexSettings) {
        return (indexSettings == 1 || indexSettings == 4);
    }

    private Boolean isStemming(int indexSettings) {
        return (indexSettings == 2 || indexSettings == 5);
    }

    public void addDoc(String name, String content, String l_content) {
        try {
            for(int i = 0; i < this.totalIndexes; i++) {
                Document doc = new Document();
                doc.add(new StringField("docid", name, Field.Store.YES));
                if(this.isLemmatize(i)) {
                    doc.add(new TextField("d_content", l_content, Field.Store.YES));
                } else {
                    doc.add(new TextField("d_content", content, Field.Store.YES));
                }
                this.indexWriter[i].addDocument(doc);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getIndexDirectoryName(int indexSetting) {
        if(indexSetting == 0) return "NS_NL_BM25";
        else if(indexSetting == 1) return "NS_L_BM25";
        else if(indexSetting == 2) return "S_NL_BM25";
        else if(indexSetting == 3) return "NS_NL_TFIDF";
        else if(indexSetting == 4) return "NS_L_TFIDF";
        else if(indexSetting == 5) return "S_NL_TFIDF";
        return "NS_NL_BM25";
    }

    public void buildIndex() {
        for(int i = 0; i < this.totalIndexes; i++) {
            try {
                this.indexLocation[i] = this.indexPath + "/" + this.getIndexDirectoryName(i);
                if(this.isStemming(i)) {
                    this.analyzer[i] = new StandardAnalyzer();
                } else {
                    this.analyzer[i] = new WhitespaceAnalyzer();
                }
                this.index[i] = FSDirectory.open(Paths.get(this.indexLocation[i]));
                IndexWriterConfig config = new IndexWriterConfig(this.analyzer[i]);
                if (this.isChangeSimilarity(i)) {
                    config.setSimilarity(new ClassicSimilarity());
                }
                this.indexWriter[i] = new IndexWriter(this.index[i], config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        indexExists = true;
    }

    private void openIndex(String absoluteIndexPath) {
        for(int i = 0; i < this.totalIndexes; i++) {
            try {
                if(absoluteIndexPath != null) {
                    i = 2;
                    this.indexLocation[i] = absoluteIndexPath;
                } else {
                    this.indexLocation[i] = this.indexPath + "/" + this.getIndexDirectoryName(i);
                }
                if (this.isStemming(i)) {
                    this.analyzer[i] = new StandardAnalyzer();
                } else {
                    this.analyzer[i] = new WhitespaceAnalyzer();
                }
                this.index[i] = FSDirectory.open(Paths.get(this.indexLocation[i]));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(absoluteIndexPath != null) break;
        }
    }

    private void readQuestionFile(String questionFile, boolean isNotAll) {
        List<String> c_ans = null;
        int[] match_count = new int[]{0, 0, 0, 0, 0, 0};
        try {
            File file = new File(questionFile);
            Scanner inputScanner = new Scanner(file);
            int lineCount = 0;
            String cat = "";
            String question = "";
            String answer = "";
            while (inputScanner.hasNextLine()) {
                String cLine = inputScanner.nextLine();
                if(lineCount == 0) {
                    cat = cLine;
                } else if(lineCount == 1) {
                    question = cLine;
                } else if(lineCount == 2) {
                    answer = cLine;
                }
                lineCount += 1;
                if(lineCount == 4) {
                    System.out.print("Match Found:");
                    c_ans = this.makeResultForQuery(cat, question, isNotAll);
                    for(int i = 0; i < match_count.length; i++) {
                        int j = i;
                        if(isNotAll) {
                            i = 2;
                            j = 0;
                        }
                        if(answer.toLowerCase().contains(c_ans.get(j).toLowerCase())) match_count[i]++;
                        System.out.print(" " + match_count[i]);
                        if(isNotAll) break;
                    }
                    System.out.println(" For question: " + question);
                    lineCount = 0;
                }
            }
            inputScanner.close();

            System.out.print("Total Match Found:");
            for(int i = 0; i < this.totalIndexes; i++) {
                if(isNotAll) i = 2;
                System.out.print(" " + match_count[i]);
                if(isNotAll) break;
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readQuestionFileForNN(String questionFile, Unsupervised un) {
        List<String> c_ans = null;
        int match_count_nn = 0;
        try {
            File file = new File(questionFile);
            Scanner inputScanner = new Scanner(file);
            int lineCount = 0;
            String cat = "";
            String question = "";
            String answer = "";
            while (inputScanner.hasNextLine()) {
                String cLine = inputScanner.nextLine();
                if(lineCount == 0) {
                    cat = cLine;
                } else if(lineCount == 1) {
                    question = cLine;
                } else if(lineCount == 2) {
                    answer = cLine;
                }
                lineCount += 1;
                if(lineCount == 4) {
                    System.out.print("Match Found:");
                    c_ans = this.makeResultForQueryWithNN(cat, question, un);
                    for(int i = 0; i < c_ans.size(); i++) {
                        if(answer.toLowerCase().contains(c_ans.get(i).toLowerCase())) match_count_nn++;
                        System.out.print(" " + match_count_nn);
                    }
                    System.out.println(" For question: " + question);
                    lineCount = 0;
                }
            }
            inputScanner.close();

            System.out.print("Total Match Found:");
            System.out.print(" " + match_count_nn);
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void terminate(String absoluteIndexPath) {
        for(int i = 0; i < this.totalIndexes; i++) {
            if(absoluteIndexPath != null) i = 2;
            try {
                if (this.indexWriter[i] != null) {
                    this.indexWriter[i].commit();
                    this.indexWriter[i].close();
                }
                if (this.index[i] != null) this.index[i].close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(absoluteIndexPath != null) break;
        }

        System.out.println("Closed and flushed all indexed writer");
    }

    public static void main(String[] args ) {
//        try {
        String helpText = "" +
                "arg[0] -\n" +
                " -- 0 to index wiki data, arg[1] should be the path for the index directory, arg[2] should be the path for the wiki directory \n" +
                " -- 1 to query with questions with all indexes, arg[1] should be the path for the index directory, arg[2] should be the file containing " +
                "questions \n" +
                " -- 2 to query with questions with specified index, arg[1] should be the absolute path for the index file, arg[2] should be the file " +
                "containing questions, \n" +
                " -- 3 to query with questions for the NN, arg[1] should be the absolute path for the index file, arg[2] should be the file containing questions";
        String wikiDataPath = ".";
        String indexPath = ".";
        String questionFile = "";
        if(args.length >= 2) {
            int q = Integer.parseInt(args[0]);
            if(q == 0) {
                indexPath = args[1];
                QueryEngine qe = new QueryEngine(indexPath);
                try {
                    String[] fileList = {"enwiki-20140602-pages-articles.xml-0833.txt",
                            "enwiki-20140602-pages-articles.xml-0841.txt",
                            "enwiki-20140602-pages-articles.xml-0856.txt",
                            "enwiki-20140602-pages-articles.xml-0964.txt",
                            "enwiki-20140602-pages-articles.xml-0985.txt",
                            "enwiki-20140602-pages-articles.xml-1069.txt",
                            "enwiki-20140602-pages-articles.xml-1129.txt",
                            "enwiki-20140602-pages-articles.xml-1259.txt"
                    };
                    WikiCrawler[] wcl = new WikiCrawler[fileList.length];
                    qe.buildIndex();
                    wikiDataPath = args[2];

                    for (int i = 0; i < fileList.length; i++) {
                        wcl[i] = new WikiCrawler(qe, wikiDataPath, fileList[i]);
                        wcl[i].start();
                    }
                    for (int i = 0; i < fileList.length; i++) {
                        wcl[i].join();
                    }
                    System.out.println("All executions done");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                qe.terminate(null);
            } else if(q == 1) {
                indexPath = args[1];
                QueryEngine qe = new QueryEngine(indexPath);
                qe.openIndex(null);
                questionFile = args[2];
                qe.readQuestionFile(questionFile, false);
                qe.terminate(null);
            } else if(q == 2) {
                indexPath = args[1];
                QueryEngine qe = new QueryEngine(indexPath);
                qe.openIndex(indexPath);
                questionFile = args[2];
                qe.readQuestionFile(questionFile, true);
                qe.terminate(indexPath);
            } else {
                indexPath = args[1];
                QueryEngine qe = new QueryEngine(indexPath);
                qe.openIndex(indexPath);
                questionFile = args[2];
                Unsupervised un = new Unsupervised(questionFile);
                qe.readQuestionFileForNN(questionFile, un);
                qe.terminate(indexPath);
            }
        } else {
            System.out.println(helpText);
        }
    }

    private String prepareQueryForNN(String cat, String query, int ind) {
        String q = this.nlp.analyzeQueryCatString(cat);
        q += this.nlp.analyzeQueryString(query, this.isLemmatize(ind));
        return q;
    }

    private String prepareQuery(String cat, String query, int ind) {
        String q = this.nlp.analyzeQueryCatString(cat);
        q += this.nlp.analyzeQueryString(query, this.isLemmatize(ind));
        String regex = "([+\\-!\\(\\){}\\[\\]^\"~*?:\\\\]|[&\\|]{2})";
        String escapedString = q.replaceAll(regex, "\\\\$1");
        return escapedString;
    }

    private List<String> makeResultForQuery(String cat, String query, boolean isNotAll) throws java.io.FileNotFoundException,
            java.io.IOException {
        List<String> ans = new ArrayList<String>();
        try {
            for(int i = 0; i < this.totalIndexes; i++) {
                if(isNotAll) i = 2;
                String preparedQuery = this.prepareQuery(cat, query, i);
                Query q = new QueryParser("d_content", this.analyzer[i]).parse(preparedQuery);

                IndexReader reader = DirectoryReader.open(this.index[i]);
                IndexSearcher searcher = new IndexSearcher(reader);
                if (this.isChangeSimilarity(i)) {
                    searcher.setSimilarity(new ClassicSimilarity());
                }
                int fetchDoc = 1;
                TopDocs docs = searcher.search(q, fetchDoc);
                ScoreDoc[] hits = docs.scoreDocs;

                if(hits.length == 0) {
                    ans.add("...");
                } else {
                    for(int j=0;j<1;++j) {
                        int docId = hits[j].doc;
                        Document d = searcher.doc(docId);
                        ans.add(d.get("docid"));
                    }
                }
                reader.close();
                if(isNotAll)break;
            }
        } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
            System.out.println(ex.getMessage());
        }
        return ans;
    }

    private List<String> makeResultForQueryWithNN(String cat, String query, Unsupervised un) throws java.io.FileNotFoundException,
            java.io.IOException {
        List<String> ans = new ArrayList<String>();
        List<Document> nnDocs = new ArrayList<Document>();
        try {
            int i = 2;
            String preparedQuery = this.prepareQuery(cat, query, i);
            Query q = new QueryParser("d_content", this.analyzer[i]).parse(preparedQuery);

            IndexReader reader = DirectoryReader.open(this.index[i]);
            IndexSearcher searcher = new IndexSearcher(reader);
            if (this.isChangeSimilarity(i)) {
                searcher.setSimilarity(new ClassicSimilarity());
            }
            int fetchDoc = 10;
            TopDocs docs = searcher.search(q, fetchDoc);
            ScoreDoc[] hits = docs.scoreDocs;

            if(hits.length == 0) {
                ans.add("...");
            } else {
                if(un != null) {
                    for (ScoreDoc hit : hits) {
                        int docId = hit.doc;
                        Document d = searcher.doc(docId);
                        nnDocs.add(d);
                    }
                    ans.add(un.findAnswerWithNN(this.prepareQueryForNN(cat, query, i), nnDocs));
                }
            }
            reader.close();
        } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
            System.out.println(ex.getMessage());
        }
        return ans;
    }

}
