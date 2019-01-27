/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rankdoc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

//Split in to tokens PTB Tokenizer 
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import java.io.BufferedWriter;
import java.io.FileWriter;
//Poster Stemmer
import org.apache.lucene.analysis.snowball.*;
import org.tartarus.snowball.ext.PorterStemmer;

/**
 *
 * @author saba
 */
public class queryPreprocessing {
        // Convert from string to tokens and convert each token into LowerCase
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<String>();
        PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new StringReader(text),
                new CoreLabelTokenFactory(), "");
        while (ptbt.hasNext()) {
            CoreLabel label = ptbt.next();
            String token = label.toString().replaceAll("[^\\p{Alpha}]+", "");
            if (token.length() > 1) {
                tokens.add(token.toLowerCase());
            }
        }
        return tokens;

    }

    //Remove all Stop words from tokens
    public static List<String> remove_stop_words(List<String> tokens) {
        List<String> stop_words = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(System.getProperty("user.dir") + "\\" + "stoplist.txt"));
            String str;
            while ((str = in.readLine()) != null) {
                stop_words.add(str);
            }
            for (String stop_word : stop_words) {
                while (tokens.contains(stop_word)) {
                    tokens.remove(stop_word);
                }
            }

        } catch (Exception e) {

        }

        return tokens;

    }

    public static List<String> Stemmer_for_tokens(List<String> tokens) {
        List<String> stemmed_tokens = new ArrayList<String>();
        for (String token : tokens) {
            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(token); //set string you need to stem
            stemmer.stem();  //stem the word
            stemmed_tokens.add(stemmer.getCurrent());//get the stemmed word
        }

        return stemmed_tokens;

    }
    
    public static List<String> preprocess_query(String text){
        List<String> tokens = new ArrayList<String>();
        tokens = tokenize(text);
        tokens = remove_stop_words(tokens);
        tokens = Stemmer_for_tokens(tokens);
        return tokens;
    }
    
}
