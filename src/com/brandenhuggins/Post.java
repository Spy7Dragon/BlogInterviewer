package com.brandenhuggins;

public class Post {
	private String question;
	private String response;
	
	public Post(String the_question, String the_response)
	{
		question = the_question;
		response = the_response;
	}

	public String getResponse() {
		return response;
	}
	
	public String getQuestion()
	{
		return question;
	}
}
