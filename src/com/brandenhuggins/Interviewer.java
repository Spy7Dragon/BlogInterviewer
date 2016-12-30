package com.brandenhuggins;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import simplenlg.framework.*;
import simplenlg.lexicon.*;
import simplenlg.realiser.english.*;
import simplenlg.phrasespec.*;
import simplenlg.features.*;

import rita.*;

public class Interviewer {

	static HashMap<Integer, Double> question_weights;
	static HashMap<Integer, Double> title_weights;
	
	static String tag = "Bt ";

	public static void main(String[] args) 
	{
		importWeights();
		int key = getN();
		RiMarkov ri = new RiMarkov(key, true, false);
		StringBuilder full_data = new StringBuilder();
		full_data.append(prepareText("data/Information.txt"));
		full_data.append(prepareText("data/QuestionModel.txt"));
		String data = full_data.toString();
		ri.loadText(data);
		String[] generated_sentences = new String[10];
		for (int i = 0; i < generated_sentences.length; i++)
		{
			boolean isQuestion = false;
			while (!isQuestion)
			{
				String new_sentence = ri.generateSentence();
				if (RiTa.isW_Question(new_sentence))
				{
					isQuestion = true;
					generated_sentences[i] = convertToProperQuestion(new_sentence);
				}
			}
		}

		for (String sentence : generated_sentences)
		{
			System.out.println(sentence);
		}
		exportWeights(question_weights, "question_weights.data");
		exportWeights(title_weights, "title_weights.data");
	}

	private static String convertToProperQuestion(String new_sentence) {
		String return_string = null;
		String[] parts_of_speech = RiTa.getPosTags(new_sentence);
		String[] tokens = RiTa.tokenize(new_sentence);
		HashMap<String, String> PoS_map = new HashMap<String, String>();
		for (int i = 0; i < tokens.length; i++)
		{
			PoS_map.put(tokens[i],  parts_of_speech[i]);
		}
		
		NLGFactory nlgFactory = new NLGFactory();
		DocumentElement p = nlgFactory.createSentence(new_sentence);
		p.setFeature(Feature.INTERROGATIVE_TYPE, InterrogativeType.WHERE);
		return_string = p.toString();
		return return_string;
	}

	private static String prepareText(String filename) {
		String text = RiTa.loadString(filename);
	    text = text.replaceAll("<[^>]*>", tag);
	    text = text.replaceAll("[\\r\\n ]+", " ");
	    text = changeCase(text, true);
	    return text;
	}
	
	private static String changeCase(String text, boolean lower) {
	    int start = lower ? 97 : 65; 
	    for (int i = 0; i < 26; i++) {
	      String c = Character.toString((char)(start + i));
	      text = text.replaceAll(tag + c, tag + (lower ?
		  c.toLowerCase() : c.toUpperCase()));
	    }
	    return text;
	  }

	private static int getN() {
		int key = 2;
		// Look for highest weight with a preference for lower keys.
		for (int i = 10; i > 1; i--)
		{
			if (question_weights.get(i).compareTo(question_weights.get(key)) > 0)
			{
				key = i;
			}
		}
		return key;
	}

	private static void importWeights() 
	{
		// Import question weights
		FileInputStream question_weights_in;
		try {
			question_weights_in = new FileInputStream("question_weights.data");
			ObjectInputStream obj_in = new ObjectInputStream(question_weights_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof HashMap) {
				question_weights = (HashMap<Integer, Double>) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the questions.");
			question_weights = new HashMap<Integer, Double>();
			for (int i = 2; i <= 10; i++)
			{
				question_weights.put(i, 0.0);
			}
		}

		// Import title weights
		FileInputStream title_weights_in;
		try {
			title_weights_in = new FileInputStream("title_weights.data");
			ObjectInputStream obj_in = new ObjectInputStream(title_weights_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof HashMap<?, ?>) {
				title_weights = (HashMap<Integer, Double>) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the title.");
			title_weights = new HashMap<Integer, Double>();
			for (int i = 2; i <= 10; i++)
			{
				title_weights.put(i, 0.0);
			}
		}
	}

	private static void exportWeights(HashMap<Integer, Double> newWeights, String filename)
	{
		try {
			FileOutputStream file_out = new FileOutputStream(filename);
			ObjectOutputStream obj_out = new ObjectOutputStream(file_out);
			obj_out.writeObject(newWeights);
			obj_out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
