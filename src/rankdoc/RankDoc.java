/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rankdoc;
//Read 

import java.io.BufferedWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static rankdoc.readIndex.get_terms_info_of_doc;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;
import java.text.DecimalFormat;
import java.util.Scanner;

/**
 *
 * @author saba
 */
public class RankDoc {

    /**
     * @param args the command line arguments
     */
    public static List<String> read_queries_from_file() {
        List<String> queries = new ArrayList<String>();
        try {
            String directory_path = System.getProperty("user.dir") + "\\" + "topics.xml";
            File topicsFile = new File(directory_path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(topicsFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("topic");
            //System.out.println("----------------------------");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                //System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    queries.add(eElement.getAttribute("number") + ',' + eElement.getElementsByTagName("query").item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queries;

    }

    public static double[] compute_okapi_tf(double[] tf_vect, Integer doc_length, Integer avg_length) {
        double[] ok_tf_vec = new double[tf_vect.length];
        double len = 0.5 + 1.5 * ((float) doc_length / avg_length);
        for (int i = 0; i < tf_vect.length; i++) {
            ok_tf_vec[i] = tf_vect[i] / (tf_vect[i] + len);
        }
        return ok_tf_vec;

    }

    public static double compute_BM25_score(double[] tf_vect, double[] df, Integer doc_length, Integer total_doc, Integer avg_length, double k1, double b, double k2) {
        double score = 0;
        double K = k1 * ((1 - b) + b * (doc_length / avg_length));
        double query_factor = (1 + k2) / (k2 + 1);
        for (int i = 0; i < tf_vect.length; i++) {
            double doc_factor = (total_doc + 0.5) / (df[i] + 0.5);
            score += doc_factor * (((1 + k1) * tf_vect[i]) / (K + tf_vect[i])) * query_factor;
        }
        return score;

    }

    public static void tf_idf(List<String> tokens, Integer avg_len, Integer totaldocs, String query, Boolean appendFile) {
        String[] terms = tokens.toArray(new String[tokens.size()]);
        double[] idf = new double[tokens.size()];
        HashMap<String, double[]> doc_vec = new HashMap<String, double[]>();
        HashMap<String, Double> doc_score = new HashMap<String, Double>();
        for (int temp = 0; temp < terms.length; temp++) {
            doc_vec = readIndex.return_docs_with_term(terms[temp], doc_vec, temp, terms.length);
            String term_id = readIndex.getTermId(terms[temp]);
            String[] term_info = readIndex.SearchTermInfo(term_id);
            idf[temp] = Math.log10((float) totaldocs / Integer.parseInt(term_info[3]));
        }
        Iterator term_it = doc_vec.entrySet().iterator();
        while (term_it.hasNext()) {
            Map.Entry<String, double[]> pair = (Map.Entry<String, double[]>) term_it.next();
            //System.out.println(pair.getKey());
            double[] vector = pair.getValue();
            List<Integer> com_doc = readIndex.document_vector(pair.getKey());
            Integer[] doc_length = {0, 0};
            doc_length = get_terms_info_of_doc(com_doc);
            double[] com_doc_okapi_tf = new double[com_doc.size()];
            for (int i = 0; i < com_doc.size(); i++) {
                com_doc_okapi_tf[i] = com_doc.get(i);
            }
            com_doc_okapi_tf = compute_okapi_tf(com_doc_okapi_tf, doc_length[0], avg_len);
            vector = compute_okapi_tf(vector, doc_length[0], avg_len);
            double dot_product = 0;
            double norm_doc = 0;
            for (int i = 0; i < com_doc_okapi_tf.length; i++) {
                norm_doc += com_doc_okapi_tf[i] * com_doc_okapi_tf[i];
            }
            norm_doc = Math.sqrt(norm_doc);
            double norm_term = 0;
            for (int i = 0; i < terms.length; i++) {
                norm_term += idf[i] * idf[i];
            }
            norm_term = Math.sqrt(norm_term);
            for (int i = 0; i < vector.length; i++) {
                dot_product += vector[i] * idf[i];
            }
            double score = dot_product / (norm_doc * norm_term);
            //score = Double.parseDouble(new DecimalFormat("##.##").format(score));
            doc_score.put(pair.getKey(), score);
        }
        Map<String, Double> sorted = doc_score
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        //System.out.println("map after sorting by values in descending order: " + sorted);
        write_result_file("tf_idf.txt", sorted, query, appendFile);
    }

    public static void write_result_file(String filename, Map<String, Double> doc_score, String query, Boolean appendFile) {
        int rank = 1;
        File resultFile = new File(filename);

        try {
            BufferedWriter doc_rank_writer = new BufferedWriter(new FileWriter(resultFile, appendFile));
            Iterator it = doc_score.entrySet().iterator();
            for (Map.Entry<String, Double> entry : doc_score.entrySet()) {
                //System.out.println(entry.getKey() + "/" + entry.getValue());
                String docId = entry.getKey();
                String docName = readIndex.getDocName(docId);
                Double docScore = entry.getValue();
                doc_rank_writer.write(query + " " + 0 + " " + docName + " " + rank + " " + docScore + " " + "Run1");
                rank += 1;
                doc_rank_writer.newLine();
            }
            doc_rank_writer.close();
        } catch (Exception e) {

        }
    }

    public static void okapi_tf(List<String> tokens, Integer avg_len, String query, Boolean appendFile) {
        String[] terms = tokens.toArray(new String[tokens.size()]);
        HashMap<String, double[]> doc_vec = new HashMap<String, double[]>();
        HashMap<String, Double> doc_score = new HashMap<String, Double>();
        //List of doc with term frequency for each term in query
        for (int temp = 0; temp < terms.length; temp++) {
            doc_vec = readIndex.return_docs_with_term(terms[temp], doc_vec, temp, terms.length);
        }
        Iterator term_it = doc_vec.entrySet().iterator();
        while (term_it.hasNext()) {
            Map.Entry<String, double[]> pair = (Map.Entry<String, double[]>) term_it.next();
            //System.out.println(pair.getKey());
            // document vector with only terms that are present in query
            double[] vector = pair.getValue();
            //Complete document vector 
            List<Integer> com_doc = readIndex.document_vector(pair.getKey());
            Integer[] doc_length = {0, 0};
            doc_length = get_terms_info_of_doc(com_doc);
            double[] com_doc_okapi_tf = new double[com_doc.size()];
            for (int i = 0; i < com_doc.size(); i++) {
                com_doc_okapi_tf[i] = com_doc.get(i);
            }
            com_doc_okapi_tf = compute_okapi_tf(com_doc_okapi_tf, doc_length[0], avg_len);
            vector = compute_okapi_tf(vector, doc_length[0], avg_len);
            double dot_product = 0;
            //Compute norm of doc by using each term present in that doc
            double norm_doc = 0;
            for (int i = 0; i < com_doc_okapi_tf.length; i++) {
                norm_doc += com_doc_okapi_tf[i] * com_doc_okapi_tf[i];
            }
            norm_doc = Math.sqrt(norm_doc);
            //Compute norm for query.Consider only binary queries either term present or not
            double norm_term = 1 * terms.length;
            norm_term = Math.sqrt(norm_term);
            //Compute dot product of query vector with doc vector
            for (int i = 0; i < vector.length; i++) {
                dot_product += vector[i];
            }
            double score = dot_product / (norm_doc * norm_term);
            //score = Double.parseDouble(new DecimalFormat("##.####").format(score));
            doc_score.put(pair.getKey(), score);
        }
        //Sort docs with non decreaing simmilarity score
        Map<String, Double> sorted = doc_score
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        //System.out.println("map after sorting by values in descending order: "+sorted);
        write_result_file("Okapi_tf.txt", sorted, query, appendFile);
    }

    public static void okapi_BM25(List<String> tokens, Integer total_doc, Integer avg_len, String query, Boolean appendFile) {
        String[] terms = tokens.toArray(new String[tokens.size()]);
        HashMap<String, double[]> doc_vec = new HashMap<String, double[]>();
        HashMap<String, Double> doc_score = new HashMap<String, Double>();
        double[] df = new double[tokens.size()];
        //List of doc with term frequency for each term in query
        for (int temp = 0; temp < terms.length; temp++) {
            doc_vec = readIndex.return_docs_with_term(terms[temp], doc_vec, temp, terms.length);
            String term_id = readIndex.getTermId(terms[temp]);
            String[] term_info = readIndex.SearchTermInfo(term_id);
            df[temp] = Integer.parseInt(term_info[3]);
        }
        Iterator term_it = doc_vec.entrySet().iterator();
        while (term_it.hasNext()) {
            Map.Entry<String, double[]> pair = (Map.Entry<String, double[]>) term_it.next();
            double[] vector = pair.getValue();
            String docId = pair.getKey();
            Integer doc_length = readIndex.doc_length(docId);
            double score = compute_BM25_score(vector, df, doc_length, total_doc, avg_len, 1.2, 0.75, 500);
            //score = Double.parseDouble(new DecimalFormat("##.####").format(score));
            doc_score.put(docId, score);
        }
        //Sort docs with non decreaing simmilarity score
        Map<String, Double> sorted = doc_score
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        //System.out.println("map after sorting by values in descending order: " + sorted);
        write_result_file("bm_25.txt", sorted, query, appendFile);
    }

    public static double compute_JM_Score(double[] tf_vect, double[] corpus_f, Integer corpus_size,double lembda,double doc_len) {
        double score = 1;
        for (int i = 0; i < tf_vect.length; i++) {
            double prob = lembda*(tf_vect[i]/doc_len)+(1-lembda)*(corpus_f[i]/corpus_size);
            score *= prob;
        }
        return score;

    }

    public static void smoothing_JM(List<String> tokens, Integer corpus_size, String query, Boolean appendFile) {
        String[] terms = tokens.toArray(new String[tokens.size()]);
        HashMap<String, double[]> doc_vec = new HashMap<String, double[]>();
        HashMap<String, Double> doc_score = new HashMap<String, Double>();
        double[] corpus_tf = new double[tokens.size()];
        //List of doc with term frequency for each term in query
        for (int temp = 0; temp < terms.length; temp++) {
            doc_vec = readIndex.return_docs_with_term(terms[temp], doc_vec, temp, terms.length);
            String term_id = readIndex.getTermId(terms[temp]);
            String[] term_info = readIndex.SearchTermInfo(term_id);
            corpus_tf[temp] = Integer.parseInt(term_info[2]);
        }
        Iterator term_it = doc_vec.entrySet().iterator();
        while (term_it.hasNext()) {
            Map.Entry<String, double[]> pair = (Map.Entry<String, double[]>) term_it.next();
            String docId = pair.getKey();
            System.out.println(docId);
            double[] vector = pair.getValue();
            Integer doc_length = readIndex.doc_length(docId);
            double score = compute_JM_Score(vector, corpus_tf,corpus_size,0.8,doc_length);
            doc_score.put(docId, score);
        }
        //Sort docs with non decreaing simmilarity score
        Map<String, Double> sorted = doc_score
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        //System.out.println("map after sorting by values in descending order: " + sorted);
        write_result_file("jm.txt", sorted, query, appendFile);
    }
    
    public static void execute_queries(String input){
        List<String> queries = read_queries_from_file();
        int[] result = readIndex.average_doc_length();
        //System.out.println(avg_length);
        Boolean appendFile = false;
        for (String query : queries) {
            String[] query_info = query.split(",");
            //System.out.println(query_info[0]);
            //System.out.println(query_info[1]);
            List<String> tokens = new ArrayList<String>();
            tokens = queryPreprocessing.preprocess_query(query_info[1]);
            if(input.toLowerCase().equals("jm")){
                //System.out.println("jm");
                smoothing_JM(tokens, result[2], query_info[0],appendFile);
            }
            else if(input.toLowerCase().equals("bm25")){
                //System.out.println("bm");
                okapi_BM25(tokens, result[1], result[0], query_info[0], appendFile);
            }
            else if(input.toLowerCase().equals("tf")){
                //System.out.println("tf");
                okapi_tf(tokens, result[0], query_info[0],appendFile);
            }
            else if(input.toLowerCase().equals("tf-idf")){
                //System.out.println("tf-idf");
                tf_idf(tokens,result[0],result[1], query_info[0],appendFile);
            }else{
                System.out.println("No Scoring Function match");
                return;
            }
            appendFile = true;
            //okapi_tf(tokens,result[0]);
            //tf_idf(tokens,result[0],result[1]);
        }
        
    }

    public static void main(String[] args) {
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Your Input: ");
        String input = scanner.nextLine();
        String input_Array[] = input.split(" ");
        if (input_Array.length > 0) {
            if (input_Array.length == 2) {
                //Single query
                if (input_Array[0].toLowerCase().equals("--score")) {
                    execute_queries(input_Array[1]);
                    
                } 
                else{
                    execute_queries(input_Array[0]);
                }

            } 
        }
    }

}
