package com.brandenhuggins;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.awt.Font;

public class InterviewCreator {
	private JFrame frame;
	private JButton btnYes;
	private JButton btnNo;
	private JButton btnRespond;
	private JButton btnInterview;
	private JButton btnTrain;
	private JTextArea lblQuestion;
	private static Interviewer computer;
	private JTextField txtResponse;
	
	
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
		gridBagLayout.columnWidths = new int[]{100, 100, 100, 101, 100, 0};
		gridBagLayout.rowHeights = new int[]{31, 23, 30, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		frame.getContentPane().setLayout(gridBagLayout);
		
		btnTrain = new JButton("Train");
		btnTrain.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnYes.setEnabled(true);
				btnNo.setEnabled(true);
				btnInterview.setEnabled(false);
				btnTrain.setEnabled(false);
				lblQuestion.setText(computer.getTrainingQuestion());
			}
		});
		GridBagConstraints gbc_btnTrain = new GridBagConstraints();
		gbc_btnTrain.anchor = GridBagConstraints.NORTH;
		gbc_btnTrain.insets = new Insets(0, 0, 5, 5);
		gbc_btnTrain.gridx = 1;
		gbc_btnTrain.gridy = 1;
		frame.getContentPane().add(btnTrain, gbc_btnTrain);
		
		btnInterview = new JButton("Interview");
		GridBagConstraints gbc_btnInterview = new GridBagConstraints();
		gbc_btnInterview.insets = new Insets(0, 0, 5, 5);
		gbc_btnInterview.gridx = 2;
		gbc_btnInterview.gridy = 1;
		frame.getContentPane().add(btnInterview, gbc_btnInterview);
		
		JButton btnCreatePost = new JButton("Create Post");
		btnCreatePost.setEnabled(false);
		GridBagConstraints gbc_btnCreatePost = new GridBagConstraints();
		gbc_btnCreatePost.insets = new Insets(0, 0, 5, 5);
		gbc_btnCreatePost.gridx = 3;
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
		gbc_lblQuestion.gridwidth = 3;
		gbc_lblQuestion.gridheight = 2;
		gbc_lblQuestion.insets = new Insets(0, 0, 5, 5);
		gbc_lblQuestion.gridx = 1;
		gbc_lblQuestion.gridy = 2;
		frame.getContentPane().add(lblQuestion, gbc_lblQuestion);
		
		JLabel lblStatus = new JLabel("");
		GridBagConstraints gbc_lblStatus = new GridBagConstraints();
		gbc_lblStatus.insets = new Insets(0, 0, 5, 0);
		gbc_lblStatus.gridx = 4;
		gbc_lblStatus.gridy = 2;
		frame.getContentPane().add(lblStatus, gbc_lblStatus);
		
		txtResponse = new JTextField();
		GridBagConstraints gbc_txtResponse = new GridBagConstraints();
		gbc_txtResponse.gridheight = 3;
		gbc_txtResponse.gridwidth = 3;
		gbc_txtResponse.insets = new Insets(0, 0, 5, 5);
		gbc_txtResponse.fill = GridBagConstraints.BOTH;
		gbc_txtResponse.gridx = 1;
		gbc_txtResponse.gridy = 4;
		frame.getContentPane().add(txtResponse, gbc_txtResponse);
		txtResponse.setColumns(10);
		
		btnYes = new JButton("Yes");
		btnYes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnYes.setEnabled(false);
				btnNo.setEnabled(false);
				computer.rewardQuestionModel(lblQuestion.getText());
				btnTrain.setEnabled(true);
			}
		});
		btnYes.setEnabled(false);
		GridBagConstraints gbc_btnYes = new GridBagConstraints();
		gbc_btnYes.insets = new Insets(0, 0, 5, 5);
		gbc_btnYes.gridx = 1;
		gbc_btnYes.gridy = 7;
		frame.getContentPane().add(btnYes, gbc_btnYes);
		
		btnNo = new JButton("No");
		btnNo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnYes.setEnabled(false);
				btnNo.setEnabled(false);
				computer.punishQuestionModel();
				btnTrain.setEnabled(true);
			}
		});
		btnNo.setEnabled(false);
		GridBagConstraints gbc_btnNo = new GridBagConstraints();
		gbc_btnNo.insets = new Insets(0, 0, 5, 5);
		gbc_btnNo.gridx = 2;
		gbc_btnNo.gridy = 7;
		frame.getContentPane().add(btnNo, gbc_btnNo);
		
		btnRespond = new JButton("Respond");
		btnRespond.setEnabled(false);
		GridBagConstraints gbc_btnRespond = new GridBagConstraints();
		gbc_btnRespond.insets = new Insets(0, 0, 5, 5);
		gbc_btnRespond.gridx = 3;
		gbc_btnRespond.gridy = 7;
		frame.getContentPane().add(btnRespond, gbc_btnRespond);
	}
}
