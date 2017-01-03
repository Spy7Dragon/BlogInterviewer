package com.brandenhuggins;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.UIManager;

import java.awt.Font;

public class InterviewCreator {
	// Interface
	private JFrame frame;
	private JButton btnYes;
	private JButton btnNo;
	private JButton btnRespond;
	private JButton btnInterview;
	private JButton btnTrain;
	private JButton btnCreatePost;
	private JTextArea lblQuestion;
	private JTextArea txtResponse;
	// Statuses
	private boolean interview_mode = false;
	private int interview_count;
	// Objects
	private static Interviewer computer;
	private BlogPost current_blog;
	
	public static void main(String[] args) 
	{
		computer = new Interviewer();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					InterviewCreator window = new InterviewCreator();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public InterviewCreator() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 500, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{1, 91, 66, 42, 79, 91, 0};
		gridBagLayout.rowHeights = new int[]{1, 23, 55, 85, 23, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		frame.getContentPane().setLayout(gridBagLayout);
		
		btnInterview = new JButton("Interview");
		btnInterview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Disable interface.
				btnInterview.setEnabled(false);
				btnTrain.setEnabled(false);
				// Initialize variables.
				interview_mode = true;
				interview_count = 5;
				current_blog = new BlogPost();
				// Perform actions.
				computer.startInterview();
				updateInterview();
				// Update interface;
				btnNo.setEnabled(true);
				btnRespond.setEnabled(true);
			}
		});
		
		btnTrain = new JButton("Train");
		btnTrain.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// Disable interface.
				btnInterview.setEnabled(false);
				btnTrain.setEnabled(false);
				// Perform actions.
				lblQuestion.setText(computer.getTrainingQuestion());
				// Update interface.
				btnYes.setEnabled(true);
				btnNo.setEnabled(true);
			}
		});
		
		JLabel lblStatus = new JLabel("");
		GridBagConstraints gbc_lblStatus = new GridBagConstraints();
		gbc_lblStatus.insets = new Insets(0, 0, 5, 5);
		gbc_lblStatus.gridx = 0;
		gbc_lblStatus.gridy = 0;
		frame.getContentPane().add(lblStatus, gbc_lblStatus);
		GridBagConstraints gbc_btnTrain = new GridBagConstraints();
		gbc_btnTrain.anchor = GridBagConstraints.NORTH;
		gbc_btnTrain.insets = new Insets(0, 0, 5, 5);
		gbc_btnTrain.gridx = 2;
		gbc_btnTrain.gridy = 1;
		frame.getContentPane().add(btnTrain, gbc_btnTrain);
		GridBagConstraints gbc_btnInterview = new GridBagConstraints();
		gbc_btnInterview.insets = new Insets(0, 0, 5, 5);
		gbc_btnInterview.gridx = 4;
		gbc_btnInterview.gridy = 1;
		frame.getContentPane().add(btnInterview, gbc_btnInterview);
		
		btnYes = new JButton("Yes");
		btnYes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Disable interface.
				btnYes.setEnabled(false);
				btnNo.setEnabled(false);
				// Perform Actions.
				computer.rewardQuestionModel(lblQuestion.getText());
				// Update interface.
				btnTrain.setEnabled(true);
				btnInterview.setEnabled(true);
			}
		});
		
		btnCreatePost = new JButton("Create Post");
		btnCreatePost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Disable interface.
				btnCreatePost.setEnabled(false);
				// Perform actions.
				current_blog.postToBlog();
			}
		});
		btnCreatePost.setEnabled(false);
		GridBagConstraints gbc_btnCreatePost = new GridBagConstraints();
		gbc_btnCreatePost.insets = new Insets(0, 0, 5, 0);
		gbc_btnCreatePost.gridx = 5;
		gbc_btnCreatePost.gridy = 1;
		frame.getContentPane().add(btnCreatePost, gbc_btnCreatePost);
		
		lblQuestion = new JTextArea("");
		lblQuestion.setWrapStyleWord(true);
		lblQuestion.setLineWrap(true);
		lblQuestion.setEditable(false);
		lblQuestion.setBackground(UIManager.getColor("Label.background"));
		lblQuestion.setFont(new Font("Verdana", Font.PLAIN, 11));
		GridBagConstraints gbc_lblQuestion = new GridBagConstraints();
		gbc_lblQuestion.fill = GridBagConstraints.BOTH;
		gbc_lblQuestion.insets = new Insets(0, 0, 5, 0);
		gbc_lblQuestion.gridwidth = 4;
		gbc_lblQuestion.gridx = 2;
		gbc_lblQuestion.gridy = 2;
		frame.getContentPane().add(lblQuestion, gbc_lblQuestion);
		
		txtResponse = new JTextArea(3, 15);
		txtResponse.setWrapStyleWord(true);
		txtResponse.setLineWrap(true);
		txtResponse.setFont(new Font("Verdana", Font.PLAIN, 11));
		JScrollPane scrollPane = new JScrollPane(txtResponse);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane.gridwidth = 4;
		gbc_scrollPane.gridx = 2;
		gbc_scrollPane.gridy = 3;
		frame.getContentPane().add(scrollPane, gbc_scrollPane);
		btnYes.setEnabled(false);
		GridBagConstraints gbc_btnYes = new GridBagConstraints();
		gbc_btnYes.insets = new Insets(0, 0, 0, 5);
		gbc_btnYes.gridx = 2;
		gbc_btnYes.gridy = 4;
		frame.getContentPane().add(btnYes, gbc_btnYes);
		
		btnRespond = new JButton("Respond");
		btnRespond.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Initialize variables.
				Post new_post = new Post(lblQuestion.getText(), txtResponse.getText());
				//Perform actions.
				current_blog.addPost(new_post);
				computer.rewardQuestionModel(lblQuestion.getText(), txtResponse.getText().length());
				interview_count--;
				updateInterview();
				// Update interface.
				txtResponse.setText("");
			}
		});
		
		btnNo = new JButton("No");
		btnNo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Perform actions.
				if (interview_mode)
				{
					updateInterview();
				}
				else
				{
					// Disable interface.
					btnYes.setEnabled(false);
					btnNo.setEnabled(false);
					// Update interface.
					btnTrain.setEnabled(true);
					btnInterview.setEnabled(true);
				}
				computer.punishQuestionModel(lblQuestion.getText());
			}
		});
		btnNo.setEnabled(false);
		GridBagConstraints gbc_btnNo = new GridBagConstraints();
		gbc_btnNo.insets = new Insets(0, 0, 0, 5);
		gbc_btnNo.gridx = 4;
		gbc_btnNo.gridy = 4;
		frame.getContentPane().add(btnNo, gbc_btnNo);
		btnRespond.setEnabled(false);
		GridBagConstraints gbc_btnRespond = new GridBagConstraints();
		gbc_btnRespond.gridx = 5;
		gbc_btnRespond.gridy = 4;
		frame.getContentPane().add(btnRespond, gbc_btnRespond);
	}
	
	protected void updateInterview()
	{
		// Perform actions.
		if (interview_count > 0)
		{
			lblQuestion.setText(computer.getInterviewQuestion());
		}
		else
		{
			// Disable interface.
			btnRespond.setEnabled(false);
			btnNo.setEnabled(false);
			// Initialize variables.
			interview_mode = false;
			// Perform actions.
			computer.createTitle(current_blog);
			// Update interface.
			btnTrain.setEnabled(true);
			btnInterview.setEnabled(true);
			btnCreatePost.setEnabled(true);
			
		}
	}
}
