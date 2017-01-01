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

	// Lists
	private static HashMap<Integer, Double> question_weights;
	private static HashMap<Integer, Double> title_weights;
	private List<String> model_questions = new ArrayList<String>();
	private static String[] tags = {"AI", "ML", "artificial intelligence", "machine learning", "autonomous", "programming", 
			"software", "software development", "robots", "robotics", "automate", "intelligent", "navigation", "decision making"};
	private ArrayList<String> rejected_questions = new ArrayList<String>();
	private static String[] question_starts = {"What", "When", "Where", "Why", "Who", "How"};
	// Strings		
	private static String question_file = "data/QuestionModel.txt";
	private static String data_file = "data/History.txt";
	private static String question_weight_file = "data/question_weights.data";
	private static String title_weight_file = "data/title_weights.data";
	private static String n_grams_file = "data/n_grams.data";
	// Numbers
	private static int n_grams;
	private static int max_n_grams = 20;
	// Objects
	private RiMarkov interview_markov;

	public Interviewer()
	{

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
							if (stringContainsItemFromList(text, tags))
							{
								full_data.append(text);
							}
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
		String formatted_data = prepareText(full_data.toString());
		return formatted_data;
	}

	private static HashSet<String> getURLs(String filename){
		HashSet<String> link_list = new HashSet<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-16"));
			String line = br.readLine();

			while (line != null)
			{
				if (line.contains("http") && !line.contains(".gov"))
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
		// Replace double quotations.
		new_sentence = new_sentence.replace("\"",  "");		
		// Create proper beginning.
		char[] character_check = new_sentence.toCharArray();
		if (!Character.isUpperCase(character_check[0]))
		{
			character_check[0] = Character.toUpperCase(character_check[0]);
			new_sentence = String.copyValueOf(character_check);
		}
		// Create proper ending.
		if (new_sentence.endsWith("."))
		{
			return_string = new_sentence.substring(0, new_sentence.length() - 1) + "?";
		}
		else if (new_sentence.endsWith(" ?"))
		{
			return_string = new_sentence.substring(0, new_sentence.length() - 2) + "?";
		}
		else if (new_sentence.endsWith("?"))
		{
			return_string = new_sentence;
		}
		else
		{
			return_string = new_sentence + "?";
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
			br = new BufferedReader(new InputStreamReader(new FileInputStream(question_file), "UTF-8"));
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
							if (stringContainsItemFromList(text, tags))
							{
								full_data.append(text);
							}
						}
					} catch (IOException e) {
						// Too many websites to post every error.
					}
					return null;
				}
			};
			// Take a sample of 10% of the links.
			Random generator = new Random();
			int i = generator.nextInt(125) + 1;
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

	protected static boolean stringContainsItemFromList(String text, String[] tags) {
		boolean truth = false;
		for (String item : tags)
		{
			if (text.contains(item))
			{
				truth = true;
				break;
			}
		}
		return truth;
	}

	private static String prepareText(String text) {
		text = text.replaceAll("[[(]){}<>]", "");
		text = text.replaceAll("\\+", "");
		text = text.replaceAll("$", "");
		text = text.replaceAll("[\\r\\n ]+", " ");
		text = text.trim().replaceAll(" +",  " ");
		return text;
	}

	private String generateQuestion(RiMarkov ri) {
		String generated_question = null;
		
		boolean isQuestion = false;
		while (!isQuestion)
		{
			// String new_sentence = ri.generateSentence();
			StringBuffer new_build = new StringBuffer();
			String[] squish = ri.generateUntil("?", 7, 35);
			for (int i = 0; i < squish.length; i++)
			{
				new_build.append(squish[i]);
				if (i < squish.length - 1)
				{
					new_build.append(" ");
				}				
			}
			String new_sentence = new_build.toString();
			if (isWH_Question(new_sentence))
			{
				String new_question = convertToProperQuestion(new_sentence);
				// Check if the question has already been rejected.
				if (!rejected_questions.contains(new_question))
				{
					generated_question = new_question;
				}
			}
			// Check if the question meets criteria.
			if (generated_question != null && !generated_question.isEmpty())
			{
				boolean already_created = model_questions.contains(generated_question);
				if (!already_created)
				{
					isQuestion = true;
				}
			}
		}
		return generated_question;
	}
	
	public static boolean isWH_Question(String sentence) {
	    for (int i = 0; i < question_starts.length; i++)
	    {
	    	if ((sentence.trim().toUpperCase()).startsWith(question_starts[i].toUpperCase()))
	    	{
	    		return true;
	    	}
	    }
	    return false;
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

	public void punishQuestionModel(String bad_question) {
		// Update weights
		double previous_value = question_weights.get(n_grams);
		question_weights.put(n_grams, previous_value - n_grams);
		exportWeights(question_weights, question_weight_file);
		// Put question on rejection list.
		rejected_questions.add(bad_question);
	}

	public void startInterview() {
		importWeights();
		getN();
		getModelQuestions();
		interview_markov = new RiMarkov(n_grams, true, false);
		String data = prepareData();
		interview_markov.loadText(data);
	}

	public void rewardQuestionModel(String good_question, int length) {
		// Update weights
		double previous_value = question_weights.get(n_grams);
		question_weights.put(n_grams, previous_value + length);
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

	public String getInterviewQuestion() {
		String generated_question = generateQuestion(interview_markov);
		return generated_question;
	}

	public void createTitle(BlogPost current_blog) {
		importWeights();
		getN();
		RiMarkov ri = new RiMarkov(n_grams, true, false);
		// Add posts to the data used for creating the title.
		StringBuilder post_builder = new StringBuilder();
		for (Post post : current_blog.getPosts())
		{
			post_builder.append(post.getQuestion() + " ");
			post_builder.append(post.getResponse() + " ");
		}
		String data = post_builder.toString();
		ri.loadText(data);
		String title = ri.generateSentence();
		current_blog.setTitle(title);
	}
}
