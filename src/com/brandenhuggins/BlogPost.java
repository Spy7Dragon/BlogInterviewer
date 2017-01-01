package com.brandenhuggins;

import java.io.PrintWriter;
import java.util.ArrayList;

public class BlogPost {
	private String title;
	private ArrayList<Post> posts = new ArrayList<Post>();
	
	public BlogPost()
	{
		title = "";
	}
	
	public void addPost(Post post)
	{
		posts.add(post);
	}
	
	public void createTitle(String the_title)
	{
		title = the_title;
	}
	
	public ArrayList<Post> getPosts()
	{
		return posts;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void postToBlog()
	{
		try
		{
			PrintWriter writer = new PrintWriter("latest_blog.txt", "UTF-8");
			writer.println(getTitle());
			writer.println();
			for (Post post : getPosts())
			{
				writer.println("<b>" + post.getQuestion() + "</b>");
				writer.println();
				writer.println(post.getResponse());
				writer.println();
			}
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setTitle(String the_title) {
		title = the_title;
	}
}
