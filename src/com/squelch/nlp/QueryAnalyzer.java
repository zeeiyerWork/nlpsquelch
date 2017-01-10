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

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	   String fileName = "";
	   if (args.length < 1) {
		  LOG.error("Filename required");
	   }
	   fileName = args[0];
	   (new QueryAnalyzer()).analyze(fileName);
	}

	@Test
	public void analyze(String fileName) {
	    LOG.info("Stanford CoreNLP: Starting to analyze contents of file: " + fileName);
	    Stream<String> ticketTitleStream = null;
	    try {
	    	ticketTitleStream = Files.lines(Paths.get(fileName));
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    ArrayList<String> ticketInputs = new ArrayList<String>();
	    Iterator<String> iTicket = ticketTitleStream.iterator();
	    while (iTicket.hasNext()) {
	    	ticketInputs.add(iTicket.next());
	    }
	    
	    Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		 
	    for (String s : ticketInputs) {
	    	// run all annotators on the passed-in text
    		LOG.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    		LOG.info("input Ticket Text:" + "\t" + s);
    		Annotation document = new Annotation(s);
	    	pipeline.annotate(document);

	    	// these are all the sentences in this document
	    	// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    	List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		      	    	
	    	for (CoreMap sentence : sentences) {
	    		// get Sentiment for the sentence.
	    		String sentenceSentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
	    		LOG.info(sentenceSentiment + ":\t" + sentence.toString());

		        // traversing the words in the current sentence, "O" is a sensible default to initialize
		        // tokens to since we're not interested in unclassified / unknown things..
		
		        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			          String word       = token.get(TextAnnotation.class);
			          String nerToken   = token.get(NamedEntityTagAnnotation.class);
			          String posToken   = token.get(PartOfSpeechAnnotation.class);
			          // One can get a lot more.
			          // String lemmaToken = token.get(LemmaAnnotation.class);

			          if (! nerToken.equals("O")) {
			        	  LOG.info(word + ":\t" + nerToken  + ":\t" + posToken);
			        	  // persist in MongoDB?
			          }
		        }
	    	}
		      
	    }
	}
	
}
	


