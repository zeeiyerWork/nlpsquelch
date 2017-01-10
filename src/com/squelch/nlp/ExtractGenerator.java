package com.squelch.nlp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class ExtractGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractGenerator.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	   if (args.length < 1) {
		   System.out.println("Filename required");
	   }
	   String fileName = args[0];
	   String extractedSummary = (new ExtractGenerator()).summarize(fileName);
	   LOG.info("For file: " + fileName + ". The Extracted summary: " + extractedSummary);
	}

	public String summarize(String filename) {
		final int UPPER_THRESHOLD = 10;
		// using StanfordNLP get Sentences in Text Document.
		ArrayList<String> allSentences = getAllSentences(filename);
		
		// assign a score to each Sentence - (fake implementation for now)
        ConcurrentHashMap<String, Integer> scoredSentences = assignScoreToSentences(allSentences);
        
        // sort this Map by value (i.e. score)
        LinkedHashMap<String, Integer> sortedSentences = (LinkedHashMap<String, Integer>) sortSentencesByValue(scoredSentences);
     
        // get the top few, based on an upper threshold.
        ArrayList<String> topSentences = filterTopSentences(sortedSentences, UPPER_THRESHOLD);
      
        // classify each of the top as indicative of the document - (fake implementation for now)
        ArrayList<String> indicativeSentences = classifyAsIndicative(topSentences);

        // concatenate the result to provide a summary.
        return generateExtractionSummary(indicativeSentences);
    }
	
	private ArrayList<String> getAllSentences(String filename) {
		 ArrayList<String> allSentences = new ArrayList<String>();
		 // read the file into the text variable.
		 String text = "";
		 try {
			 text = new String(readAllBytes(get(filename)));
		 } catch (Exception e) {
		 }
		 
		 if (text.length() == 0) {
			 return allSentences;
		 }
		 
		 // create an empty Annotation just with the given text
		 Annotation document = new Annotation(text);

		 // creates a StanfordCoreNLP object, 
		 Properties props = new Properties();
		 props.put("annotators", "tokenize, ssplit");
		 StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		 
		 // run all Annotators on this text
		 pipeline.annotate(document);

		 // these are all the sentences in this document
		 // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		 List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		 for(CoreMap sentence: sentences) {
		   allSentences.add(sentence.toString());
		 }
		 return allSentences;
	 }
	
	private ConcurrentHashMap<String, Integer> assignScoreToSentences(ArrayList<String> allSentences) {
		ConcurrentHashMap<String, Integer> scoredSentences = new ConcurrentHashMap<String, Integer>();
		for (String sentence: allSentences) {
			int theScore = getScore(sentence);
			scoredSentences.put(sentence, theScore);
			LOG.info("SCORE/SENTENCE ::: " + theScore + " ===> " + sentence.toString());
		}
		return scoredSentences;
	}
	
	private Integer getScore(String anySentence) {
		// Get features for this sentence and then score based on the features.
		// For now , just return a random score.
		Integer theScore = 0;
		try {
			theScore = new Integer((int) Math.round(100*Math.random()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return theScore;
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortSentencesByValue(Map<K, V> allSentences) {			 
		Map<K, V> sortedResult = new LinkedHashMap<>();
		Stream<Map.Entry<K, V>> sequentialStream = allSentences.entrySet().stream(); 
		// comparingByValue() returns a comparator that compares Map.Entry in natural order on value.
		sequentialStream.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(c -> sortedResult.put(c.getKey(), c.getValue()));
		return sortedResult;
	}
		 
	private ArrayList<String> filterTopSentences(LinkedHashMap<String, Integer> sortedSentences, int maxThreshold) {
		ArrayList<String> topChosenSentences = new ArrayList<String>();
		int count = 0;
		for (String key : sortedSentences.keySet()) {
			if (count < maxThreshold) {
				topChosenSentences.add(key);
				LOG.info("TOP-SCORE/TOP-SENTENCE ::: " + sortedSentences.get(key) + " ===> " + key);
				count++;
			} else { 
				break;
			}
		}
		return topChosenSentences;
	}
	 
	 private ArrayList<String> classifyAsIndicative(List<String> topSentences) {
		 ArrayList<String> indicativeSentences = new ArrayList<String>();
		 Random r = new Random();
		 for (String s : topSentences) {
			 if (r.nextBoolean()) {
				 indicativeSentences.add(s);
			 }
		 }
		 return indicativeSentences;
	 }
	 	
	 private String generateExtractionSummary(ArrayList<String> indicativeSentences) {
		 String summary = "";
		 final String SEPARATOR = "+++++";
		 for (String indicativeSentence : indicativeSentences) {
	                summary += indicativeSentence + SEPARATOR;
	        }
	        return summary;
	 }

}
