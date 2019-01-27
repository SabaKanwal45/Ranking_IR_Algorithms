/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rankdoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author saba
 */
public class readIndex {
        public static String getDocName(String docId) {
        String docName = "";
        File docIds = new File("docids.txt");
        //System.out.println("Inside Document Handler");
        try {
            BufferedReader doc_ids_file = new BufferedReader(new FileReader(docIds));
            String str = "";
            while ((str = doc_ids_file.readLine()) != null) {
                //System.out.println(str);
                String[] doc_info = str.split("\t");
                //System.out.println(doc_info[1]);
                if (doc_info[0].equals(docId)) {
                    //System.out.println(doc_info[0]);
                    docName = doc_info[1];
                    break;
                }
            }
            doc_ids_file.close();

        } catch (Exception e) {
            System.out.println("Inside Exception of getting Document Id");

        }
        return docName;
    }
    public static String getDocId(String docName) {
        String docId = "";
        File docIds = new File("docids.txt");
        //System.out.println("Inside Document Handler");
        try {
            BufferedReader doc_ids_file = new BufferedReader(new FileReader(docIds));
            String str = "";
            while ((str = doc_ids_file.readLine()) != null) {
                //System.out.println(str);
                String[] doc_info = str.split("\t");
                //System.out.println(doc_info[1]);
                if (doc_info[1].equals(docName)) {
                    //System.out.println(doc_info[0]);
                    docId = doc_info[0];
                    break;
                }
            }
            doc_ids_file.close();

        } catch (Exception e) {
            System.out.println("Inside Exception of getting Document Id");

        }
        return docId;
    }
    public static HashMap<String, double[]>return_docs_with_term(String term,HashMap<String, double[]> doc_tf,Integer term_i,Integer length){
        String term_id = getTermId(term);
        String[] term_info = SearchTermInfo(term_id);
        try {
                File term_index = new File("term_index.txt");
                RandomAccessFile raf = new RandomAccessFile(term_index, "r");
                raf.seek(Integer.parseInt(term_info[1]));
                String posting = raf.readLine();
                raf.close();
                String[] split_posting = posting.split("\t");
                //int prev_doc_Id = 0;
                int com_doc_Id = 0;
                for (int index = 1; index < split_posting.length; index++) {
                    String[] seperte_t_d = split_posting[index].split(":");
                    com_doc_Id += Integer.parseInt(seperte_t_d[0]);
                    if (doc_tf.containsKey(Integer.toString(com_doc_Id)) == false) {
                        double[] doc_vec=new double[length];
                        //System.out.println("initialize new vector array");
                        for(int i=0;i<length;i++){
                            if(i==term_i){
                                doc_vec[term_i] = 1;
                            }
                            else{
                                doc_vec[i] = 0;
                            }
                            //System.out.print(doc_vec[i]);
                        }
                        doc_tf.put(Integer.toString(com_doc_Id), doc_vec);
                    }
                    else{
                        double[] tf = doc_tf.get(Integer.toString(com_doc_Id));
                        tf[term_i] += 1;
                        /*System.out.println("add term at index "+term_i);
                        for(int i=0;i<length;i++){
                            System.out.print(tf[i]);
                        }*/
                        doc_tf.put(Integer.toString(com_doc_Id), tf);
                    }
                    
                }
                

            } catch (Exception e) {

            }
        return doc_tf;
    }
    public static Integer[] get_terms_info_of_doc(List<Integer> info){
        //info document vector
        Integer [] result = {0,0};
        //result[0] store total terms
        //result[1] store total distinct terms
        //System.out.print("doc vector");
        for (Integer tf:info){
            //System.out.print(" "+tf);
            result[0] +=tf;
            result[1] +=1;
        }
        //System.out.println();
        return result;
        
    }
    public static Integer doc_length(String docId){
        List<Integer> info = new ArrayList<Integer>();
        info = document_vector(docId);
        Integer[] value = {0,0};
        value = get_terms_info_of_doc(info);
        return value[0];
    }
    
    public static List<Integer> document_vector(String docId) {
        File doc_index = new File("doc_index.txt");
        List<Integer> info = new ArrayList<Integer>();
        try {
            
            BufferedReader doc_index_file = new BufferedReader(new FileReader(doc_index));
            String str = "";
            while ((str = doc_index_file.readLine()) != null) {
                //System.out.println(str);
                String[] term_info = str.split("\t");
                if (term_info[0].equals(docId)) {
                    info.add(term_info.length - 2);
                    //break;
                } 
            }
            
            
            doc_index_file.close();

        } catch (Exception e) {
            System.out.println("Inside Exception of getting Term Id");

        }
        return info;
    }
    
    public static int[] average_doc_length(){
        File doc_index = new File("doc_index.txt");
        //result[0] average length;result[1] total number of docs;result[2] corpus size
        int [] result = {0,0,0};
        try {
            BufferedReader doc_index_file = new BufferedReader(new FileReader(doc_index));
            HashMap<String, Integer> doc_length_map = new HashMap<String, Integer>();
            String str = "";
            while ((str = doc_index_file.readLine()) != null) {
                String[] doc_info = str.split("\t");
                if (doc_length_map.containsKey(doc_info[0]) == false) {
                    if(doc_info.length-2>0){
                        doc_length_map.put(doc_info[0], doc_info.length-2);
                        //result[1] +=1;
                    }
                }
                else{
                    Integer length = doc_length_map.get(doc_info[0]);
                    if(doc_info.length-2>0){
                        length += doc_info.length-2;
                        doc_length_map.put(doc_info[0], length);
                    }
                    
                    
                }
            }
            Iterator term_it = doc_length_map.entrySet().iterator();
            Integer sum = 0;
            while (term_it.hasNext()) {
                Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) term_it.next();
                sum +=pair.getValue();
                result[1] +=1;
                
            }
            result[2] = sum;
            result[0]= sum/result[1];
            doc_index_file.close();
        } catch (Exception e) {
            System.out.println("Inside Exception of getting Document Id");

        }
        
        
        return result;
    }

    public static void searchDocument(String docId) {
        File doc_index = new File("doc_index.txt");
        List<Integer> info = new ArrayList<Integer>();
        //total terms
        //info.add(0);
        //total distinct terms
        //info.add(0);
        //System.out.println("Inside Term Handler");
        try {
            
            BufferedReader doc_index_file = new BufferedReader(new FileReader(doc_index));
            String str = "";
            int disinctTerms = 0;
            int totalTerms = 0;
            while ((str = doc_index_file.readLine()) != null) {
                //System.out.println(str);
                String[] term_info = str.split("\t");
                //System.out.println(term_info[1]);
                if (term_info[0].equals(docId)) {
                    //System.out.println(term_info[0]);
                    //info.add(1, info.get(1).intValue()+1);
                    disinctTerms += 1;
                    //info.add(0, info.get(0).intValue()+term_info.length - 2);
                    info.add(term_info.length - 2);
                    totalTerms += term_info.length - 2;
                    //break;
                } else {
                    if (disinctTerms > 0) {
                        break;
                    }
                }
            }
            
            doc_index_file.close();
            System.out.println("Distinct terms: " + disinctTerms);
            System.out.println("Total terms: " + totalTerms);

        } catch (Exception e) {
            System.out.println("Inside Exception of getting Term Id");

        }
         System.out.println("result in info array");
        int sum = 0;
        int total_terms = 0;
        for (Integer tf:info){
            sum +=tf;
            total_terms+=1;
            System.out.println(tf);
        }
        System.out.println("total terms "+sum);
        System.out.println("distinct terms "+total_terms);
    }

    public static void handle_document_query(String docName) {
        System.out.println("Listing for document: " + docName);
        String docId = getDocId(docName);
        if (docId != "") {
            System.out.println("DOCID: " + docId);
            searchDocument(docId);
        } else {
            System.out.println("Document not found");
        }

    }

    public static String getTermId(String term) {
        String termId = "";
        File termsIds = new File("termids.txt");
        //System.out.println("Inside Term Handler");
        try {
            BufferedReader term_ids_file = new BufferedReader(new FileReader(termsIds));
            String str = "";
            while ((str = term_ids_file.readLine()) != null) {
                //System.out.println(str);
                String[] term_info = str.split("\t");
                //System.out.println(term_info[1]);
                if (term_info[1].equals(term)) {
                    //System.out.println(term_info[0]);
                    termId = term_info[0];
                    break;
                }
            }
            term_ids_file.close();

        } catch (Exception e) {
            System.out.println("Inside Exception of getting Term Id");

        }
        return termId;
    }
    public static String[] SearchTermInfo(String termId) {
        File term_info = new File("term_info.txt");
        String output[] = {"", "", "", ""};
        //System.out.println("Inside Term Handler");
        try {
            BufferedReader term_info_file = new BufferedReader(new FileReader(term_info));
            String str = "";
            int disinctTerms = 0;
            int totalTerms = 0;
            while ((str = term_info_file.readLine()) != null) {
                //System.out.println(str);
                String[] term_detail = str.split("\t");
                //System.out.println(term_info[1]);
                if (term_detail[0].equals(termId)) {
                    //System.out.println(term_info[0]);
                    output[0] = term_detail[0];
                    output[1] = term_detail[1];
                    output[2] = term_detail[2];
                    output[3] = term_detail[3];
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("Inside Exception of getting Term Id");

        }
        return output;

    }

    public static void handle_term_query(String Term) {
        System.out.println("Listing for term: " + Term);
        String termId = getTermId(Term);
        if (termId != "") {
            System.out.println("TERMID: " + termId);
            String term_info[] = SearchTermInfo(termId);
            System.out.println("Number of documents containing term: " + term_info[3]);
            System.out.println("Term frequency in corpus: " + term_info[2]);
            System.out.println("Inverted list offset: " + term_info[1]);
        } else {
            System.out.println("Termt not found");
        }

    }

    public static void handle_both_queries(String docName, String Term) {
        System.out.println("Inverted list for term: " + Term);
        System.out.println("In document: " + docName);
        String termId = getTermId(Term);
        if (termId != "") {
            System.out.println("TERMID: " + termId);
        }
        String docId = getDocId(docName);
        if (docId != "") {
            System.out.println("DOCID: " + docId);
        }
        if (termId != "" && docId != "") {
            try {
                String term_info[] = SearchTermInfo(termId);
                File term_index = new File("term_index.txt");
                RandomAccessFile raf = new RandomAccessFile(term_index, "r");
                raf.seek(Integer.parseInt(term_info[1]));
                String posting = raf.readLine();
                raf.close();
                String[] split_posting = posting.split("\t");
                //int prev_doc_Id = 0;
                int com_doc_Id = 0;
                int pos = 0;
                int term_freq = 0;
                List<String> positions = new ArrayList<String>();
                for (int index = 1; index < split_posting.length; index++) {
                    String[] seperte_t_d = split_posting[index].split(":");
                    com_doc_Id += Integer.parseInt(seperte_t_d[0]);
                    //System.out.println("docId: "+com_doc_Id);
                    if (com_doc_Id == Integer.parseInt(docId)) {
                        term_freq += 1;
                        pos = pos + Integer.parseInt(seperte_t_d[1]);
                        positions.add(String.valueOf(pos));
                    }
                }
                System.out.println("Term frequency in document: " + term_freq);
                System.out.print("Positions:");
                int index = 1;
                for (String position : positions) {
                    if (index == term_freq) {
                        System.out.print(" " + position);
                    } else {
                        System.out.print(" " + position + ",");
                    }
                    index += 1;
                }

            } catch (Exception e) {

            }
        }

    }
    
}
