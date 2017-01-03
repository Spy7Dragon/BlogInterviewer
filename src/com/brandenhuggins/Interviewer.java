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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import rita.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Interviewer {

	// Lists
	private static HashMap<Integer, Double> question_weights;
	private static HashMap<Integer, Double> title_weights;
	private static HashMap<Integer, Double> minimum_weights;
	private static HashMap<Integer, Double> maximum_weights;
	private static HashMap<Integer, Double> gap_weights;
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
	private static String minimum_weight_file = "data/minimum_weights.data";
	private static String maximum_weight_file = "data/maximum_weights.data";
	private static String gap_weight_file = "data/gap_weights.data";
	private static String n_grams_file = "data/n_grams.data";
	private static String minimum_file = "data/minimum.data";
	private static String maximum_file = "data/maximum.data";
	private static String gap_file = "data/gap.data";
	// Numbers
	private static int n_grams;
	private static int min_n_grams = 3;
	private static int max_n_grams = 20;
	private static int min_length;
	private static int max_length;
	private static int min_word_length = 3;
	private static int max_word_length = 40;
	private static int gap;
	static int threads = Runtime.getRuntime().availableProcessors() * 4;
	// Objects
	private RiMarkov interview_markov;
	private static ExecutorService searcher = Executors.newFixedThreadPool(threads);

	public Interviewer()
	{
		RiMarkov.MAX_GENERATION_ATTEMPTS = 10000;
	}

	private static List<String> prepareData() {
		List<String> full_data = Collections.synchronizedList(new ArrayList<String>());
		HashSet<String> links = getURLs(data_file);
		// Import text from every website.
		for (String link : links)
		{
			Runnable runnable = new Runnable()
			{
				@Override
				public void run() {
					Connection conn = Jsoup.connect(link);
					String text = null;
					try {
						Document doc = conn.get();
						if (doc.body().text() != null)
						{
							text = doc.body().text();
							if (stringContainsItemFromList(text, tags))
							{
								String[] toAdd = prepareText(text);
								for (String sentence : toAdd)
								{
									full_data.add(sentence);
								}
							}
						}
					} catch (IOException e) {
						// Too many websites to post every error.
					}
				}
			};
			searcher.submit(runnable);
		}

		// Add questions to list.
		ArrayList<String> question_list = prepareFile(question_file);
		for (String question : question_list)
		{
			full_data.add(question);
		}
		return full_data;
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

	private static ArrayList<String> prepareFile(String filename) {
		String text = RiTa.loadString(filename);
		text = text.replaceAll("\\n", " ");
		text = text.replaceAll("\\r", " ");
		text = text.replaceAll(" +", " ");
		ArrayList<String> sentences = new ArrayList<String>();
		for (String sentence : RiTa.splitSentences(text))
		{
			sentences.add(sentence);
		}
		return sentences;
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
			key = min_n_grams;
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


	private void getLength() {
		importLength();
		updateLength();
		exportLength();
	}

	private void updateLength()
	{
		// Initialize values.
		int minimum;
		if (min_length != 0)
		{
			minimum = min_length;
		}
		else
		{
			minimum = min_word_length;
		}
		int maximum;
		if (max_length != 0)
		{
			maximum = max_length;
		}
		else
		{
			maximum = max_word_length;
		}
		int space;
		if (gap != 0)
		{
			space = gap;
		}
		else
		{
			space = 1;
		}

		// Look for highest weight with a preference for lower keys.
		for (int i = min_word_length; i <= max_word_length - 1; i++)
		{
			// Fill in any key that doesn't already have a value.
			if (minimum_weights.get(i) == null)
			{
				minimum_weights.put(i, 0.0);
			}
			// Compare the value of the keys.
			if (minimum_weights.get(i).compareTo(minimum_weights.get(minimum)) > 0)
			{
				minimum = i;
			}
		}
		// Look for highest weight with a preference for lower keys.
		for (int i = min_word_length + 1; i <= max_word_length; i++)
		{
			// Fill in any key that doesn't already have a value.
			if (maximum_weights.get(i) == null)
			{
				maximum_weights.put(i, 0.0);
			}
			// Compare the value of the keys.
			if (maximum_weights.get(i).compareTo(maximum_weights.get(maximum)) > 0)
			{
				maximum = i;
			}
		}
		// Look for highest weight with a preference for higher keys.
		for (int i = max_word_length - min_word_length; i >= 1; i--)
		{
			// Fill in any key that doesn't already have a value.
			if (gap_weights.get(i) == null)
			{
				gap_weights.put(i, 0.0);
			}
			// Compare the value of the keys.
			if (gap_weights.get(i).compareTo(gap_weights.get(space)) > 0)
			{
				space = i;
			}
		}
		// Make sure bounds are appropriate.
		if (space < 1 || space > max_word_length - min_word_length)
		{
			space = 1;
		}

		if (minimum < min_word_length || minimum > maximum - space)
		{
			if (maximum - space >= min_word_length)
			{
				minimum = maximum - space;
			}
			else
			{
				minimum = min_word_length;
			}
		}

		if (maximum > max_word_length || maximum < minimum + space)
		{
			if (minimum + space <= max_word_length)
			{
				maximum = minimum + space;
			}
			else
			{
				maximum = max_word_length;
			}
		}

		min_length = minimum;
		max_length = maximum;
		gap = maximum - minimum;

	}

	private void exportLength() {
		try {
			FileOutputStream file_out = new FileOutputStream(minimum_file);
			ObjectOutputStream obj_out = new ObjectOutputStream(file_out);
			obj_out.writeObject(min_length);
			obj_out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileOutputStream file_out = new FileOutputStream(maximum_file);
			ObjectOutputStream obj_out = new ObjectOutputStream(file_out);
			obj_out.writeObject(max_length);
			obj_out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileOutputStream file_out = new FileOutputStream(gap_file);
			ObjectOutputStream obj_out = new ObjectOutputStream(file_out);
			obj_out.writeObject(gap);
			obj_out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void importLength() {
		// Import Minimum
		FileInputStream minimum_in;
		try {
			minimum_in = new FileInputStream(minimum_file);
			ObjectInputStream obj_in = new ObjectInputStream(minimum_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof Integer) {
				min_length = (Integer) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();		
		}
		// Import Maximum
		FileInputStream maximum_in;
		try {
			maximum_in = new FileInputStream(maximum_file);
			ObjectInputStream obj_in = new ObjectInputStream(maximum_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof Integer) {
				max_length = (Integer) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();		
		}
		// Import Gap
		FileInputStream gap_in;
		try {
			gap_in = new FileInputStream(gap_file);
			ObjectInputStream obj_in = new ObjectInputStream(gap_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof Integer) {
				gap = (Integer) obj;
			}
		} catch (Exception e) {
			e.printStackTrace();		
		}
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
			else
			{
				throw new Exception("Incorrect object");
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the questions.");
			question_weights = new HashMap<Integer, Double>();
			for (int i = min_n_grams; i <= max_n_grams; i++)
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
			else
			{
				throw new Exception("Incorrect object");
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the title.");
			title_weights = new HashMap<Integer, Double>();
			for (int i = min_n_grams; i <= max_n_grams; i++)
			{
				title_weights.put(i, 0.0);
			}
		}

		// Import minimum weights
		FileInputStream minimum_weights_in;
		try {
			minimum_weights_in = new FileInputStream(minimum_weight_file);
			ObjectInputStream obj_in = new ObjectInputStream(minimum_weights_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof HashMap<?, ?>) {
				minimum_weights = (HashMap<Integer, Double>) obj;
			}
			else
			{
				throw new Exception("Incorrect object");
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the minimum length.");
			minimum_weights = new HashMap<Integer, Double>();
			for (int i = min_word_length; i <= max_word_length; i++)
			{
				minimum_weights.put(i, 0.0);
			}
		}

		// Import maximum weights
		FileInputStream maximum_weights_in;
		try {
			maximum_weights_in = new FileInputStream(maximum_weight_file);
			ObjectInputStream obj_in = new ObjectInputStream(maximum_weights_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof HashMap<?, ?>) {
				maximum_weights = (HashMap<Integer, Double>) obj;
			}
			else
			{
				throw new Exception("Incorrect object");
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the maximum length.");
			maximum_weights = new HashMap<Integer, Double>();
			for (int i = min_word_length; i <= max_word_length; i++)
			{
				maximum_weights.put(i, 0.0);
			}
		}

		// Import gap weights
		FileInputStream gap_weights_in;
		try {
			gap_weights_in = new FileInputStream(gap_weight_file);
			ObjectInputStream obj_in = new ObjectInputStream(gap_weights_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof HashMap<?, ?>) {
				gap_weights = (HashMap<Integer, Double>) obj;
			}
			else
			{
				throw new Exception("Incorrect object");
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Setup new weights.
			System.out.println("Creating new weights for the gap.");
			gap_weights = new HashMap<Integer, Double>();
			for (int i = 1; i <= max_word_length - min_word_length; i++)
			{
				gap_weights.put(i, 0.0);
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
		getLength();
		getModelQuestions();
		RiMarkov ri = new RiMarkov(n_grams, true, false);
		List<String> data = preparePartialData();
		for (String sentence : data)
		{
			ri.loadText(sentence);
		}
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

	private List<String> preparePartialData() {
		List<String> full_data = Collections.synchronizedList(new ArrayList<String>());
		HashSet<String> links = getURLs(data_file);
		List<Callable<Void>> call_list = new ArrayList<Callable<Void>>();
		// Import text from every website.
		for (String link : links)
		{
			Callable<Void> callable = new Callable<Void>()
			{
				@Override
				public Void call() {
					Connection conn = Jsoup.connect(link);
					String text = null;
					try {
						Document doc = conn.get();
						if (doc.body().text() != null)
						{
							text = doc.body().text();
							if (stringContainsItemFromList(text, tags))
							{
								String[] toAdd = prepareText(text);
								for (String sentence : toAdd)
								{
									full_data.add(sentence);
								}
							}
						}
					} catch (IOException e) {
						// Too many websites to post every error.
					}
					return null;
				}
			};
			// Take a sample of the links.
			Random generator = new Random();
			int i = generator.nextInt(links.size() / threads) + 1;
			if (i == 5)
			{
				call_list.add(callable);
			}
		}

		try {
			searcher.invokeAll(call_list);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Add questions to list.
		ArrayList<String> question_list = prepareFile(question_file);
		for (String question : question_list)
		{
			full_data.add(question);
		}
		return full_data;
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

	private static String[] prepareText(String text) {
		text = text.replaceAll("\\n", " ");
		text = text.replaceAll("\\r", " ");
		text = text.replaceAll(" +", " ");
		String[] sentences = RiTa.splitSentences(text);
		return sentences;
	}

	private String generateQuestion(RiMarkov ri) {
		String generated_question = null;

		boolean isQuestion = false;
		while (!isQuestion)
		{
			String new_sentence = null;
			try {
				new_sentence = splinterSearch(ri);	
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			if (new_sentence == null)
			{
				adjustQuestionLength();
				continue;
			}
			
			System.out.println(new_sentence);

			if (new_sentence.trim().isEmpty()){
				adjustQuestionLength();
				continue;
			}

			if (isWH_Question(new_sentence))
			{
				String new_question = convertToProperQuestion(new_sentence);
				// Check if the question has already been rejected.
				if (!rejected_questions.contains(new_question))
				{
					generated_question = new_question;
				}
			}
			else
			{
				adjustQuestionLength();
				continue;
			}
			// Check if the question meets criteria.
			if (!model_questions.contains(generated_question))
			{
				isQuestion = true;
			}
			else
			{
				adjustQuestionLength();
			}
		}
		return generated_question;
	}

	private String splinterSearch(RiMarkov ri) throws InterruptedException, ExecutionException {
		// Import text from every website.

		List<Callable<String>> call_list = new ArrayList<Callable<String>>(); 
		// Create the callable list.
		for (int i = 0; i < threads; i++)
		{
			Callable<String> callable = new Callable<String>()
			{
				@Override
				public String call() {
					return generateWrapper(ri);
				}
			};
			call_list.add(callable);
		}

		String first_value = searcher.invokeAny(call_list);
		return first_value;
	}

	protected String generateWrapper(RiMarkov ri) {
		String[] temp_array = null;
		String value = null;
		Random choice = new Random();
		int i = choice.nextInt(4) + 1;
		switch (i)
		{
		case 1:
			temp_array = ri.generateUntilZ("\\?", min_length, max_length);
			break;
		case 2:
			temp_array = ri.generateUntil("?", min_length, max_length);
			break;
		case 3:
			temp_array = ri.generateSentence().split(" ");
			break;
		case 4:
			temp_array = forwardGenerate(ri, min_length, max_length);
			break;
		}
		value = arrayToString(temp_array);
		return value;
	}

	private String[] forwardGenerate(RiMarkov ri, int minLength, int maxLength) {
		StringBuffer build = new StringBuffer();
		// pick random from known starts.
		String first = (String) RiTa.randomItem(question_starts);
		build.append(first + " ");
		// Get completion.
		HashMap<String, Float> probabilities = new HashMap<String, Float>();
		boolean complete = false;
		String second = null;
		while (!complete)
		{
			String third = null;
			if (second == null)
			{
				probabilities = (HashMap<String, Float>) ri.getProbabilities(new String[]{first});
				second = (String) findGreatestProbability(probabilities);
				build.append(second + " ");
			}
			else if (build.length() < minLength)
			{
				probabilities = (HashMap<String, Float>) ri.getProbabilities(new String[]{second});
				third = (String) findGreatestProbability(probabilities);
				build.append(third + " ");
				
				// Update positions.
				first = second;
				second = third;
			}
			else if (build.length() == maxLength)
			{
				complete = true;
			}
			else
			{
				probabilities = (HashMap<String, Float>) ri.getProbabilities(new String[]{second});
				third = (String) findGreatestProbability(probabilities);
				build.append(third);

				// Ask if this is the end.
				if (RiTa.isSentenceEnd(second, third))
				{
					complete = true;
				}
				else
				{
					build.append(" ");
					// Update positions.
					first = second;
					second = third;
				}
			}
		}
		String created_question = build.toString();
		return prepareText(created_question);
	}

	private String findGreatestProbability(HashMap<String, Float> probabilities) {
		Map.Entry<String, Float> max_entry = null;
		for (Entry<String, Float> pair : probabilities.entrySet())
		{
			if (max_entry == null || pair.getValue().compareTo(max_entry.getValue()) > 0)
			{
				max_entry = pair;
			}
		}
		return max_entry.getKey();
	}

	private String arrayToString(String[] temp_array) {
		StringBuffer new_build = new StringBuffer();
		for (int i = 0; i < temp_array.length; i++)
		{
			new_build.append(temp_array[i]);
			if (i < temp_array.length - 1)
			{
				new_build.append(" ");
			}				
		}
		return new_build.toString();
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
		double previous_min = minimum_weights.get(min_length);
		double previous_max = maximum_weights.get(max_length);
		double previous_gap = gap_weights.get(gap);
		question_weights.put(n_grams, previous_value + 100.0);
		minimum_weights.put(min_length, previous_min + 100.0);
		maximum_weights.put(max_length, previous_max + 100.0);
		gap_weights.put(gap, previous_gap + 100.0);
		exportWeights(question_weights, question_weight_file);
		exportWeights(minimum_weights, minimum_weight_file);
		exportWeights(maximum_weights, maximum_weight_file);
		exportWeights(gap_weights, gap_weight_file);
		// Add question to model.
		try {
			PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(question_file, true)));
			output.println(good_question);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public void adjustQuestionLength() {
		// Update weights
		double previous_min = minimum_weights.get(min_length);
		double previous_max = maximum_weights.get(max_length);
		double previous_gap = gap_weights.get(gap);
		minimum_weights.put(min_length, previous_min - min_length);
		maximum_weights.put(max_length, previous_max - max_length);
		gap_weights.put(gap, previous_gap - gap);
		exportWeights(minimum_weights, minimum_weight_file);
		exportWeights(maximum_weights, maximum_weight_file);
		exportWeights(gap_weights, gap_weight_file);
		// Get new weights
		getLength();
	}

	public void punishQuestionModel(String bad_question) {
		// Update weights
		double previous_value = question_weights.get(n_grams);
		double previous_min = minimum_weights.get(min_length);
		double previous_max = maximum_weights.get(max_length);
		double previous_gap = gap_weights.get(gap);
		question_weights.put(n_grams, previous_value - n_grams);
		minimum_weights.put(min_length, previous_min - min_length);
		maximum_weights.put(max_length, previous_max - max_length);
		gap_weights.put(gap, previous_gap - gap);
		exportWeights(question_weights, question_weight_file);
		exportWeights(minimum_weights, minimum_weight_file);
		exportWeights(maximum_weights, maximum_weight_file);
		exportWeights(gap_weights, gap_weight_file);
		// Put question on rejection list.
		rejected_questions.add(bad_question);
	}

	public void startInterview() {
		importWeights();
		getN();
		getModelQuestions();
		interview_markov = new RiMarkov(n_grams, true, false);
		List<String> data = prepareData();
		for (String sentence : data)
		{
			interview_markov.loadText(sentence);
		}
	}

	public void rewardQuestionModel(String good_question, int length) {
		// Update weights
		double previous_value = question_weights.get(n_grams);
		double previous_min = minimum_weights.get(min_length);
		double previous_max = maximum_weights.get(max_length);
		double previous_gap = gap_weights.get(gap);
		question_weights.put(n_grams, previous_value + length);
		minimum_weights.put(min_length, previous_min + length);
		maximum_weights.put(max_length, previous_max + length);
		gap_weights.put(gap, previous_gap + length);
		exportWeights(question_weights, question_weight_file);
		exportWeights(minimum_weights, minimum_weight_file);
		exportWeights(maximum_weights, maximum_weight_file);
		exportWeights(gap_weights, gap_weight_file);
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
		getLength();
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
