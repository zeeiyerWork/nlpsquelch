package com.squelch.nlp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class QueryAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(QueryAnalyzer.class);
    private static final String DEFAULT_DICTIONARY_NAME = "squelchDictionary.txt";

	public static void main(String[] args) {
		// Send a file - which has all the input tickets that need to be analyzed.
	   String fileName = "";
	   // Indicate if custom dictionary is needed 
	   boolean useCustomDictionary = true;
	   // DictionaryName 
	   String dictionaryName = "";
	   
	   switch(args.length) {
	   case 0:
		   LOG.error("Usage: QueryAnalyzer [inputFileName], <useCustomDictionary>, <dictionaryFileName>");
		   break;
	   case 1:
		   fileName = args[0];
		   break;
	   case 2:
		   fileName = args[0];
		   useCustomDictionary = (args[1].equalsIgnoreCase("false")) ? false : true;
		   dictionaryName = DEFAULT_DICTIONARY_NAME;
		   break;
	   default:
		   fileName = args[0];
		   useCustomDictionary = (args[1].equalsIgnoreCase("false")) ? false : true;
		   dictionaryName = args[2];
	   }
	 
	   (new QueryAnalyzer()).analyze(fileName, useCustomDictionary, dictionaryName);
	}

	
	@Test
	public void analyze(String fileName) {
		analyze (fileName, false, null);
	}
	public void analyze(String fileName, boolean useCustomDictionary) {
		analyze (fileName, useCustomDictionary, QueryAnalyzer.DEFAULT_DICTIONARY_NAME);
	}
	
	public void analyze (String fileName, boolean useCustomDictionary, String dictionaryName) {	
	    LOG.info("Stanford CoreNLP: Starting to analyze contents of file: " + fileName);
	    Stream<String> ticketTitleStream = null;
	    try {
	    	ticketTitleStream = Files.lines(Paths.get(fileName));
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    LOG.info("Get Content of Each Ticket");
	    ArrayList<String> ticketInputs = new ArrayList<String>();
	    Iterator<String> iTicket = ticketTitleStream.iterator();
	    while (iTicket.hasNext()) {
	    	ticketInputs.add(iTicket.next());
	    }
	    
	    String[] neededAnnotations = {"tokenize, ssplit, pos, lemma, ner, parse, sentiment"};			
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(getNeededProps(neededAnnotations, useCustomDictionary, dictionaryName));  
	    for (String s : ticketInputs) {
	    	analyze(s, pipeline);
	    }
	}
	
	public Properties getNeededProps (String[] neededAnnotations, boolean useCustomDictionary, String dictionaryFileName) {
	    Properties props = new Properties();
		String theNeededAnnotations = "";
		for (String theAnnotation : neededAnnotations) {
			theNeededAnnotations += theAnnotation + ", ";
		}
		theNeededAnnotations = theNeededAnnotations.substring(0, theNeededAnnotations.length()-2);
	    theNeededAnnotations += (useCustomDictionary) ? ", regexner"  : "";
	    LOG.info ("Annotations in play: " + theNeededAnnotations);		
	    props.put("annotators", theNeededAnnotations);
	    if (useCustomDictionary) {
		      props.put("regexner.mapping", dictionaryFileName);
	    }
	    return props;
	}

	
	public void analyze (String s, StanfordCoreNLP pipeline) {
		// run all annotators on the passed-in text
  		LOG.info("=========================================================");
  		LOG.info("input Ticket Text:" + "\t" + s);
  		Annotation document = new Annotation(s);
  		pipeline.annotate(document);

  		// these are all the sentences in this document
  		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
  		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		      	    	
  		
  		for (CoreMap sentence : sentences) {
  			// get SENTIMENT for the sentence.
  			String sentenceSentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
  			LOG.info(sentenceSentiment + ":\t" + sentence.toString());
  			
  			// prepare Data Structures for POS and NER.
  			
  			// for POS
  			ListMultimap<String, String> posMultiMap = ArrayListMultimap.create();

  			// for NER:  not interested in unclassified / unknown things..
  			String nerPreviousToken = "";
  			String nerPhrase = "";
  			
  			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
  				String word       = token.get(TextAnnotation.class);
  				String posToken   = token.get(PartOfSpeechAnnotation.class);
  				String nerToken   = token.get(NamedEntityTagAnnotation.class);
  				
  				// NER - process token appropriately., Only care about POS for words recognized by NER? 
  				if (! nerToken.equals("O")) {
  					// POS - process token into the DataStructure.
  	  				posMultiMap.put(posToken, word);
  					nerPhrase = (nerPreviousToken.equalsIgnoreCase(nerToken)) ?  nerPhrase + " " + word : word;
  					nerPreviousToken = nerToken;
  				} else {
  					if ((nerPhrase.length() > 0)) {
  						LOG.info (nerPreviousToken + " : " + nerPhrase);
  						nerPhrase = "";
  						nerPreviousToken = "";
  					}
  				}
  			}	
  			for (String eachPosToken : posMultiMap.keySet()) {
  				LOG.info ("Part of Speech: " + eachPosToken + " - " + posMultiMap.get(eachPosToken));
  			}
  	  		LOG.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  		}      
	  }
}
	


