package com.squelch.nlp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class ExtractGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractGenerator.class);
    private StanfordCoreNLP pipeline = null;

    public static void main(String[] args) {
		// TODO Auto-generated method stub
	   if (args.length < 2) {
		   LOG.error("Usage: ExtractGenerator [InputFilename:String] [useAuthenticML:boolean]");
	   } else {
		   String fileName = args[0];
		   boolean useAuthenticML = (args[1].equalsIgnoreCase("false")) ? false : true;
		   String extractedSummary = (new ExtractGenerator()).summarize(fileName, useAuthenticML);
		   LOG.info("EXTRACTED SUMMARY For file: " + fileName + ": " + System.getProperty("line.separator") + extractedSummary);
	   }
	}

	public String summarize(String fileName) {
		return summarize(fileName,false);
	}
	
	public String summarize(String filename, boolean useAuthenticML) {
		// using StanfordNLP get Sentences in Text Document.
		List<CoreMap> allSentences = getAllSentences(filename);
		LOG.info("SENTENCE COUNT for file: " + filename + " : " + allSentences.size());
		
		// assign a score to each Sentence - (fake/real implementation based on boolean)
        ConcurrentHashMap<String, Integer> scoredSentences = assignScoreToSentences(allSentences, useAuthenticML);
        
        // sort this Map by value (i.e. score)
        LinkedHashMap<String, Integer> sortedSentences = (LinkedHashMap<String, Integer>) sortSentencesByValue(scoredSentences);
     
        // get the top few, based on an upper threshold.
        ArrayList<String> topSentences = filterTopSentences(sortedSentences);
      
        // classify each of the top as indicative of the document - (fake/real implementation based on boolean)
        ArrayList<String> indicativeSentences = classifyAsIndicative(topSentences, useAuthenticML);

        // concatenate the result to provide a summary.
        return generateExtractionSummary(indicativeSentences);
    }
	
	private List<CoreMap> getAllSentences(String filename) {
		 List<CoreMap> allSentences = new ArrayList<CoreMap>();
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
		 String[] neededAnnotations = {"tokenize, ssplit"};			
		 pipeline = new StanfordCoreNLP(new QueryAnalyzer().getNeededProps(neededAnnotations, false,null));  
		
		 // run all Annotators on this text
		 pipeline.annotate(document);

		 // these are all the sentences in this document
		 // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		 List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		 return sentences;
	 }
	
	private ConcurrentHashMap<String, Integer> assignScoreToSentences(List<CoreMap> allSentences, boolean useAuthenticML) {
		ConcurrentHashMap<String, Integer> scoredSentences = new ConcurrentHashMap<String, Integer>();
	
		// variables for score calculations
		int TOP_FEW = 5;
		int BOTTOM_FEW = 5;
		int POSITION_SCORE = 10;
		int KEY_PHRASE_SCORE = 10;
		int TITLE_TOKEN_SCORE = 10;
		int BASE_SCORE_MINWORDS_SENTENCE = 20;
		int MINWORDS_SENTENCE = 3;		
		List<String> titleTokens = new ArrayList<String>();
		List<String> sentenceTokens = new ArrayList<String>();
		String[] keyPhraseList = {"Introduction", "In summary", "To summarize", "In conclusion", "To Conclude", "Document describes"};
		
		Integer sentencePosition = 0;
		Integer theScore = 0;

  		for (CoreMap sentenceCoreMap : allSentences) {
			String sentence = sentenceCoreMap.toString();
			if (! useAuthenticML) {
				// For this option - just return a random score.
				try {
					theScore = new Integer((int) Math.round(100*Math.random()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				// Get Features and score appropriately: NAIVE IMPLEMENTATION TO ILLUSTRATE CONCEPT.
				// -- Length: MinWords
				// -- Position: Is this sentence in the top few or bottom few
				// -- Content: Does the sentence contain key phrases
				// -- Content: Does the sentence contain any words from the Title.
				// TBD: Does the sentence contain any words from the top 20% Frequent Terms.			
				sentencePosition++;		
				for (CoreLabel token : sentenceCoreMap.get(TokensAnnotation.class)) {
					String word       = token.get(TextAnnotation.class);
					Boolean added     = (sentencePosition == 1) ? titleTokens.add(word) : sentenceTokens.add(word);
				}
				// scoring
				if (sentencePosition == 1) {
					theScore = Integer.MAX_VALUE; // Always include 1st Sentence = Title.
				} else {
					theScore = 0;
					// Does sentence have at least MINWORDS
					if (sentenceTokens.size() > MINWORDS_SENTENCE) {
						theScore += BASE_SCORE_MINWORDS_SENTENCE;
					}
					// Is sentence at Top or Bottom of Document
					if ((sentencePosition < TOP_FEW) || (sentencePosition > allSentences.size() - BOTTOM_FEW)) {
						theScore += POSITION_SCORE;
					}
					// Does sentence have keyPhrases
					for (String keyPhrase : keyPhraseList) {
						if (sentence.toLowerCase().contains(keyPhrase.toLowerCase())) {
							theScore += KEY_PHRASE_SCORE;
						}
					}
					// Does sentence have words in common with the title.
					for (String titleToken: titleTokens) {
						for (String sentenceToken : sentenceTokens) {
							if (titleToken.toLowerCase().equals(sentenceToken.toLowerCase())) {
								theScore += TITLE_TOKEN_SCORE;
								break;
							}
						}
					}
				}					
			}
			scoredSentences.put(sentence, theScore);
			LOG.trace("SCORE/SENTENCE ::: " + theScore + " ===> " + sentence.toString());
		}
		return scoredSentences;
	}
	
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortSentencesByValue(Map<K, V> allSentences) {			 
		Map<K, V> sortedResult = new LinkedHashMap<>();
		Stream<Map.Entry<K, V>> sequentialStream = allSentences.entrySet().stream(); 
		// comparingByValue() returns a comparator that compares Map.Entry in natural order on value.
		sequentialStream.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(c -> sortedResult.put(c.getKey(), c.getValue()));
		return sortedResult;
	}
		 
	private ArrayList<String> filterTopSentences(LinkedHashMap<String, Integer> sortedSentences) {
		ArrayList<String> topChosenSentences = new ArrayList<String>();
	    final int UPPER_THRESHOLD_NUMSENTENCES = 10;
		int count = 0;
		for (String key : sortedSentences.keySet()) {
			if (count < UPPER_THRESHOLD_NUMSENTENCES) {
				topChosenSentences.add(key);
				LOG.info("TOP-SCORE/TOP-SENTENCE ::: " + sortedSentences.get(key) + " ===> " + key);
				count++;
			} else { 
				break;
			}
		}
		return topChosenSentences;
	}
	 
	 private ArrayList<String> classifyAsIndicative(List<String> topSentences, boolean useAuthenticML) {
		 ArrayList<String> indicativeSentences = new ArrayList<String>();
		 // TBD - Fix This
		 if (!useAuthenticML || useAuthenticML) {
			 // Just Randomly classify it as indicative or not
			 Random r = new Random();
			 int count = 0;
			 for (String s : topSentences) {
				 count ++;
				 if ((r.nextBoolean()) || (count == 1)) {
					 indicativeSentences.add(s);
				 }
			 }
		 } else {
			 // Use Real ML to arrive at the answer.
		 }
		 return indicativeSentences;
	 }
	 	
	 private String generateExtractionSummary(ArrayList<String> indicativeSentences) {
		 String summary = "";
		 final String SEPARATOR = System.getProperty("line.separator") + "........" ;
		 for (String indicativeSentence : indicativeSentences) {
	                summary += indicativeSentence + SEPARATOR;
	        }
	        return summary;
	 }

}
