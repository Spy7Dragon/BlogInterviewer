package com.brandenhuggins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rita.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Interviewer {

	private static HashMap<Integer, Double> question_weights;
	private static HashMap<Integer, Double> title_weights;
	private static String question_file = "data/QuestionModel.txt";
	private static String data_file = "data/History.txt";
	private static String question_weight_file = "data/question_weights.data";
	private static String title_weight_file = "data/title_weights.data";
	private static String n_grams_file = "data/n_grams.data";
	private List<String> model_questions = new ArrayList<String>();
	private static int n_grams;
	private static int max_n_grams = 20;

	public Interviewer()
	{

	}

	private void printQuestions(ArrayList<String> generated_sentences) {
		for (String sentence : generated_sentences)
		{
			System.out.println(sentence);
		}
	}

	private ArrayList<String> generateQuestions(RiMarkov ri) {
		ArrayList<String> generated_sentences = new ArrayList<String>();

		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(threads);
		ArrayList<Callable> tasks = new ArrayList<Callable>();

		for (int i = 0; i < 10; i++)
		{
			Callable<Void> callable = new Callable<Void>()
			{
				@Override
				public Void call() throws Exception {
					generated_sentences.add(generateQuestion(ri));
					return null;
				}
			};
			tasks.add(callable);
		}
		try {
			service.invokeAll((Collection<? extends Callable<Void>>) tasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		service.shutdown();
		return generated_sentences;
	}

	private static String prepareData() {
		StringBuffer full_data = new StringBuffer();
		HashSet<String> links = getURLs(data_file);
		// Import text from every website.
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(threads);
		ArrayList<Callable> tasks = new ArrayList<Callable>();

		for (String link : links)
		{
			Callable<Void> callable = new Callable<Void>()
			{
				@Override
				public Void call() throws Exception {
					Connection conn = Jsoup.connect(link);
					String text = null;
					try {
						Document doc = conn.get();
						if (doc.body().text() != null)
						{
							text = doc.body().text();
							full_data.append(text);
						}
					} catch (IOException e) {
						// Too many websites to post every error.
					}
					return null;
				}
			};
			tasks.add(callable);
		}
		try {
			service.invokeAll((Collection<? extends Callable<Void>>) tasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		service.shutdown();

		full_data.append(prepareFile(question_file));		
		return full_data.toString();
	}

	private static HashSet<String> getURLs(String filename){
		HashSet<String> link_list = new HashSet<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-16"));
			String line = br.readLine();

			while (line != null)
			{
				if (line.contains("http") && !line.contains("google"))
				{
					String link = line.substring(line.indexOf(':') + 2, line.length());
					link_list.add(link);
				}
				line = br.readLine();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return link_list;
	}

	private static String convertToProperQuestion(String new_sentence) {
		String return_string = null;
		if (new_sentence.endsWith("."))
		{
			return_string = new_sentence.substring(0, new_sentence.length() - 1) + "?";
		}
		else if (new_sentence.endsWith("?"))
		{
			return_string = new_sentence;
		}
		return return_string;
	}

	private static String prepareFile(String filename) {
		String text = RiTa.loadString(filename);
		text = text.replaceAll("[[(]){}<>]", "");
		text = text.replaceAll("\\+", "");
		text = text.replaceAll("$", "");
		text = text.replaceAll("[\\r\\n ]+", " ");
		return text;
	}

	private static void getN() {
		importN();
		int key;
		if (n_grams != 0)
		{
			key = n_grams;
		}
		else
		{
			key = 2;
		}
		
		// Look for highest weight with a preference for lower keys.
		for (int i = 2; i <= max_n_grams; i++)
		{
			// Fill in any key that doesn't already have a value.
			if (question_weights.get(i) == null)
			{
				question_weights.put(i, 0.0);
			}
			// Compare the value of the keys.
			if (question_weights.get(i).compareTo(question_weights.get(key)) > 0)
			{
				key = i;
			}
		}
		n_grams = key;
		exportN();
	}

	private static void exportN() {
		try {
			FileOutputStream file_out = new FileOutputStream(n_grams_file);
			ObjectOutputStream obj_out = new ObjectOutputStream(file_out);
			obj_out.writeObject(n_grams);
			obj_out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void importN() {
		// Import N
		FileInputStream n_grams_in;
		try {
			n_grams_in = new FileInputStream(n_grams_file);
			ObjectInputStream obj_in = new ObjectInputStream(n_grams_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof Integer) {
				n_grams = (Integer) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();		
		}		
	}

	private static void importWeights() 
	{
		// Import question weights
		FileInputStream question_weights_in;
		try {
			question_weights_in = new FileInputStream(question_weight_file);
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
			for (int i = 2; i <= max_n_grams; i++)
			{
				question_weights.put(i, 0.0);
			}
		}

		// Import title weights
		FileInputStream title_weights_in;
		try {
			title_weights_in = new FileInputStream(title_weight_file);
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
			for (int i = 2; i <= max_n_grams; i++)
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

	public String getTrainingQuestion() {
		importWeights();
		getN();
		getModelQuestions();
		RiMarkov ri = new RiMarkov(n_grams, true, false);
		String data = preparePartialData();
		ri.loadText(data);
		String generated_question = generateQuestion(ri);
		return generated_question;
	}

	private void getModelQuestions() {
		ArrayList<String> question_model = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(question_file), "UTF-16"));
			String line = br.readLine();

			while (line != null)
			{
				question_model.add(line);
				line = br.readLine();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		model_questions = question_model;
	}

	private String preparePartialData() {
		StringBuffer full_data = new StringBuffer();
		HashSet<String> links = getURLs(data_file);
		// Import text from every website.
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(threads);
		ArrayList<Callable> tasks = new ArrayList<Callable>();

		for (String link : links)
		{
			Callable<Void> callable = new Callable<Void>()
			{
				@Override
				public Void call() throws Exception {
					Connection conn = Jsoup.connect(link);
					String text = null;
					try {
						Document doc = conn.get();
						if (doc.body().text() != null)
						{
							text = doc.body().text();
							full_data.append(text);
						}
					} catch (IOException e) {
						// Too many websites to post every error.
					}
					return null;
				}
			};
			// Take a sample of 10% of the links.
			Random generator = new Random();
			int i = generator.nextInt(10) + 1;
			if (i == 5)
			{
				tasks.add(callable);
			}
		}
		try {
			service.invokeAll((Collection<? extends Callable<Void>>) tasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		service.shutdown();

		full_data.append(prepareFile(question_file));
		String formatted_data = prepareText(full_data.toString());
		return formatted_data;
	}

	private String prepareText(String text) {
		text = text.replaceAll("[[(]){}<>]", "");
		text = text.replaceAll("\\+", "");
		text = text.replaceAll("$", "");
		text = text.replaceAll("[\\r\\n ]+", " ");
		return text;
	}

	private String generateQuestion(RiMarkov ri) {
		String generated_question = null;

		boolean isQuestion = false;
		while (!isQuestion)
		{
			String new_sentence = ri.generateSentence();
			if (RiTa.isW_Question(new_sentence))
			{
				generated_question = convertToProperQuestion(new_sentence);
			}
			if (generated_question != null && !generated_question.isEmpty() && !model_questions.contains(generated_question))
			{
				isQuestion = true;
			}	
		}
		return generated_question;
	}

	public void rewardQuestionModel(String good_question) {
		// Update weights
		double previous_value = question_weights.get(n_grams);
		question_weights.put(n_grams, previous_value + 1.0);
		exportWeights(question_weights, question_weight_file);
		// Add question to model.
		try {
			PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(question_file, true)));
			output.println(good_question);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public void punishQuestionModel() {
		// Update weights
		double previous_value = question_weights.get(n_grams);
		question_weights.put(n_grams, previous_value - 1.0);
		exportWeights(question_weights, question_weight_file);
	}
}
