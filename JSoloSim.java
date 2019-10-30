/* JSoloSim
Copyright 2019, Ron Mignery
=================================================================================================================================================================
jsoup License
The jsoup code-base (including source and compiled packages) are distributed under the open source MIT license as described below.

The MIT License
Copyright © 2009 - 2017 Jonathan Hedley (jonathan@hedley.net)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
=================================================================================================================================================================
Freetts license
Portions Copyright 2001-2004 Sun Microsystems, Inc.  
Portions Copyright 1999-2001 Language Technologies Institute,
Carnegie Mellon University.  
All Rights Reserved.  Use is subject to license terms.

Permission is hereby granted, free of charge, to use and distribute
this software and its documentation without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of this work, and to
permit persons to whom this work is furnished to do so, subject to
the following conditions:

 1. The code must retain the above copyright notice, this list of
    conditions and the following disclaimer.
 2. Any modifications must be clearly marked as such.
 3. Original authors' names are not deleted.
 4. The authors' names are not used to endorse or promote products
    derived from this software without specific prior written
    permission.
=================================================================================================================================================================

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

This software and its author are not affiliated with, sponsored by, or operated by Jeopardy Productions, Inc., the owners of the Jeopardy! trademark.
*/
package jSoloSim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;
import java.net.URISyntaxException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sun.speech.freetts.*;

public class JSoloSim extends JFrame {
	/**
	 * A single-player Jeopardy! live-play simulator for training prospective
	 * Jeopardy! contestants
	 */
	private static final long serialVersionUID = 1L;

	static JFrame theFrame;
	static JPanel jBoard; // The game squares
	static JPanel rightPanel; // on the right side

	static JButton[] sqs = new JButton[30]; // button for every square on the board left to right
	static JButton[] cats = new JButton[6]; // button for every category

	static JCheckBox bOffLine; // controls
	static JCheckBox bCaptureEnable;
	static JButton bPlay, bPrev, bLoad, bNext, bG, bY, bR, bSetup;
	static JButton bCopy;
	static JButton bHelp;
	static JLabel l;
	static JSpinner gameSelection;
	static JLabel gameNoLabel;
	static JSpinner buzzWindow;
	static JLabel labWon = new JLabel("<html>");
	static JLabel labLost = new JLabel("<html>");
	static JLabel labResp = new JLabel("<html>");
	static JLabel labGRX;

	// Control values
	static int won = 0; // amount player has won in game
	static int lost = 0; // amount player has not won in game
	static int gCount = 0;
	static int rCount = 0;
	static int xCount = 0;

	// Game state flags
	static enum st {
		VIRGIN, YOU_CHOOSE, THEY_CHOOSE, CLUE_REVEALED, BUZZERS_ARMED, BUZZED_IN, RESPONSE_REVEALED;
	}

	static st gameState = st.VIRGIN;
	static boolean capturingAudio; // timing state flag
	static boolean responding; // timing state flag
	static boolean waitingShowResponse; // timing state flag
	static boolean lastWon = false; // user clicked in time flag
	static boolean wasLastWon = false; // previous value of lastWon
	static boolean isLockout; // user clicks ignored

	// Board square states
	static enum bs {
		VIRGIN, BLANK, CLUE, RESPONSE;
	}

	static bs[] squareState = new bs[30];

	// Board state (used as index)
	static int bJDF = 0; // -1=empty 0=single 1=double 2=final Jeopardy!

	static boolean live = false; // flag buzzers armed
	static long liveTimeStart; // timestamp when buzzers armed
	static long responseMsecs = 0; // how long player took to buzz after live start
	static long clickTime = 0L; // timestamp of last click

	// Data loaded from J! or local xml
	static String[] categories = new String[13]; // 6 in J, 6 in DJ, 1 in FJ
	static String[] categoryComments = new String[13]; // 6 in J, 6 in DJ, 1 in FJ
	static String[] clues = new String[61]; // 30 in J, 30 in DJ, 1 in FJ
	static String[] responses = new String[61]; // 30 in J, 30 in DJ, 1 in FJ
	static boolean[] isDd = new boolean[61]; // 30 in J, 30 in DJ, 1 in FJ

	static String title = "JSoloSim  ";
	static String newTitle; // non-static copy of title

	static int iSquare = 0; // index of last square clicked (category -20 to -25, board 0 to 29)
	// iSquare%6 = category iSquare/5 = row
	static int iClue = 0; // index of clue for board iSquare (j=0-29, dj=30-59, fj=60)
	static int wasSquare = -1; // previous value of iSquare;
	static int theySquareNum = -1; // index of square auto-selected by computer

	int valueBase; // min square $value as loaded (all square $values=n*valueBase)
	ByteArrayOutputStream out; // audio capture and play stream
	Color colorGreen = new Color(0xff1c7329); // light green for black text background

	static int squareMoneyValue;
	static Random rand;

	// Setup value (non-volatile)
	static int buzzWindowMsecs = 1000;
	static int buzzLockoutMsecs = 250;
	static int liveLatencyMsecs = 20;
	static int debounceMsecs = 2;
	static float sampleRateFloat = 8000.0f;
	static int sampleSizeInBitsInt = 8; 
	static boolean signedBoolean = true;
	static boolean bigEndianBoolean = false;
	static int channelsInt = 1;
	static boolean localXml = false;
	static boolean ddEnabled = true; // daily double enabled

	static XmlHandler xml = null; // class instance for handling local xml
	static Voice voice; // Freetts class instance for text-to-speech

	public static void main(String[] args) {

		// Create instance of Random class
		rand = new Random();

		/* Use an appropriate Look and Feel */
		try {
			// UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		/* Turn off metal's use of bold fonts */
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * Create the GUI and show it. For thread safety, this method is invoked from
	 * the event dispatch thread.
	 */
	private static void createAndShowGUI() {

		// clear data
		clear_data();

		// Create and set up the window.
		theFrame = new JSoloSim("J! trainer  " + title + (localXml ? "  OFF-LINE" : ""));
		theFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		// Set up the content pane.
		((JSoloSim) theFrame).addComponentsToPane(theFrame.getContentPane());

		// Display the window.
		theFrame.pack();
		theFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		theFrame.setVisible(true);

		// Setup for speech-to-text via Freetts
		System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
		try {
			VoiceManager vm = VoiceManager.getInstance();
			voice = vm.getVoice("kevin16");
			voice.allocate();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// Get non-volatiles
		loadParams();

		// Instanciate local file handler (loads entire local.xml as doc)
		xml = new XmlHandler();

		// Trap exit for local file changes
		theFrame.addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (xml.dirty) {
					int c = JOptionPane.showConfirmDialog(theFrame, "Save copy/delete changes?");
					switch (c) {
					case 0:
						xml.save_xml();
					case 1:
						voice.deallocate();
						System.exit(-1);
						break;
					}
				} else {
					voice.deallocate();
					System.exit(-1);

				}
			}

			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

		});
	}

	private static void clear_data() {
		for (int i = 0; i < 61; i++) {
			clues[i] = "";
			responses[i] = "";
		}
		for (int i = 0; i < 13; i++) {
			categories[i] = "";
			categoryComments[i] = "";
		}
	}

	public void addComponentsToPane(final Container framePanel) {
		final JPanel leftPanel = new JPanel(); // categories above board
		leftPanel.setLayout(new BorderLayout());

		final JPanel jCats = new JPanel();
		jCats.setLayout(new GridLayout(1, 6, 4, 4)); // 1 row 6 cols
		jCats.setBorder(new EmptyBorder(new Insets(4, 4, 4, 4)));

		jBoard = new JPanel();
		jBoard.setLayout(new GridLayout(5, 6, 4, 4)); // 5 rows 6 cols
		jBoard.setBorder(new EmptyBorder(new Insets(4, 4, 4, 4))); // fill left to right and wrap
		
		rightPanel = new JPanel();
		rightPanel.setBackground(Color.lightGray);

		// Add buttons to cats pane
		for (int i = 0; i < 6; i++) {
			cats[i] = new JButton("Category");
			cats[i].setBackground(Color.blue);
			cats[i].setForeground(Color.white);
			cats[i].setFont(new Font("Arial", Font.PLAIN, 20));
			cats[i].addActionListener(new MyListener(-i - 20)); // cat button code are -20 to -25
			jCats.add(cats[i]);
		}

		// Add buttons to board pane
		for (int i = 0; i < 30; i++) {
			squareState[i] = bs.BLANK;
			sqs[i] = new JButton();
			sqs[i].setBackground(Color.blue);
			sqs[i].setForeground(Color.white);
			sqs[i].setFont(new Font("Arial", Font.PLAIN, 20));
			sqs[i].addActionListener(new MyListener(i)); // clue button codes are 0 to 29
			jBoard.add(sqs[i]);
		}

		// Add controls to controls pane
		bOffLine = new JCheckBox("<html>Off-line");
		rightPanel.add(bOffLine);
		bOffLine.addActionListener(new MyListener(-11));

		JLabel lab2 = new JLabel("<html> ");
		rightPanel.add(lab2);

		bCaptureEnable = new JCheckBox("<html>Capture audio");
		rightPanel.add(bCaptureEnable);

		bPlay = new JButton("Play");
		bPlay.addActionListener(new MyListener(-7));
		bPlay.setEnabled(false);
		rightPanel.add(bPlay);

		JLabel lab0 = new JLabel("<html> ");
		rightPanel.add(lab0);

		gameNoLabel = new JLabel("<html>J! game no.");
		gameNoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		rightPanel.add(gameNoLabel);
		SpinnerModel gameNo = new SpinnerNumberModel(6274, 1, 100000, 1);
		gameSelection = new JSpinner(gameNo);
		rightPanel.add(gameSelection);

		bPrev = new JButton("   <<   ");
		bPrev.addActionListener(new MyListener(-1));
		rightPanel.add(bPrev);

		bLoad = new JButton("Load");
		bLoad.addActionListener(new MyListener(-2));
		rightPanel.add(bLoad);

		bNext = new JButton("   >>   ");
		bNext.addActionListener(new MyListener(-3));
		rightPanel.add(bNext);

		JLabel lab3 = new JLabel("<html> ");
		rightPanel.add(lab3);
		bG = new JButton("<html>Right");
		bG.setBackground(Color.green);
		bG.setForeground(Color.black);
		bG.addActionListener(new MyListener(-4));
		rightPanel.add(bG);

		bY = new JButton("<html>N/A");
		bY.setBackground(Color.yellow);
		bY.setForeground(Color.black);
		bY.addActionListener(new MyListener(-5));
		rightPanel.add(bY);

		bR = new JButton("<html>&emsp;&emsp;Wrong&emsp;&emsp;");
		bR.setBackground(Color.pink);
		bR.setForeground(Color.black);
		bR.addActionListener(new MyListener(-6));
		rightPanel.add(bR);

		JLabel lab4 = new JLabel("<html> ");
		rightPanel.add(lab4);
		labWon = new JLabel(String.format("<html>Won=$%d", won));
		rightPanel.add(labWon);

		labLost = new JLabel(String.format("<html>Lost=$%d", lost));
		rightPanel.add(labLost);

		JLabel lab1 = new JLabel("<html> ");
		rightPanel.add(lab1);
		JLabel labR = new JLabel(String.format("<html>Response msecs"));
		rightPanel.add(labR);
		labResp = new JLabel(String.format("%d", responseMsecs));
		labResp.setHorizontalAlignment(SwingConstants.CENTER);
		rightPanel.add(labResp);

		JLabel labC = new JLabel(String.format("<html>G R X counts"));
		labC.setHorizontalAlignment(SwingConstants.CENTER);
		rightPanel.add(labC);
		labGRX = new JLabel(String.format("%d / %d / %d", gCount, rCount, xCount));
		labGRX.setHorizontalAlignment(SwingConstants.CENTER);
		rightPanel.add(labGRX);

		JLabel lab5 = new JLabel("<html> ");
		rightPanel.add(lab5);
		bSetup = new JButton("Setup");
		bSetup.setBackground(Color.cyan);
		bSetup.setForeground(Color.black);
		bSetup.addActionListener(new MyListener(-8));
		rightPanel.add(bSetup);

		bCopy = new JButton(localXml ? "Delete" : "Copy");
		bCopy.setBackground(Color.ORANGE);
		bCopy.setForeground(Color.black);
		bCopy.addActionListener(new MyListener(-9));
		rightPanel.add(bCopy);

		bHelp = new JButton("Help");
		bHelp.setBackground(Color.PINK);
		bHelp.setForeground(Color.black);
		bHelp.addActionListener(new MyListener(-10));
		rightPanel.add(bHelp);

		// Set up components preferred size
		Dimension dim = bR.getPreferredSize();
		lab0.setPreferredSize(dim);
		lab1.setPreferredSize(dim);
		lab2.setPreferredSize(dim);
		lab3.setPreferredSize(dim);
		lab4.setPreferredSize(dim);
		lab5.setPreferredSize(dim);
		labR.setPreferredSize(dim);
		bOffLine.setPreferredSize(dim);
		bCaptureEnable.setPreferredSize(dim);
		bPlay.setPreferredSize(dim);
		gameNoLabel.setPreferredSize(dim);
		gameSelection.setPreferredSize(dim);
		bPrev.setPreferredSize(dim);
		bLoad.setPreferredSize(dim);
		bNext.setPreferredSize(dim);
		bG.setPreferredSize(dim);
		bY.setPreferredSize(dim);
		labWon.setPreferredSize(dim);
		labLost.setPreferredSize(dim);
		labResp.setPreferredSize(dim);
		labC.setPreferredSize(dim);
		labGRX.setPreferredSize(dim);
		bSetup.setPreferredSize(dim);
		bCopy.setPreferredSize(dim);
		bHelp.setPreferredSize(dim);

		rightPanel.setPreferredSize(new Dimension((int) (dim.getWidth() * 1.5), (int) (dim.getHeight() * 1.5)));
		jCats.setPreferredSize(new Dimension((int) (dim.getWidth()), (int) (dim.getHeight() * 3.5)));

		leftPanel.add(jCats, BorderLayout.NORTH);
		leftPanel.add(jBoard, BorderLayout.CENTER);
		framePanel.add(leftPanel, BorderLayout.CENTER);
		framePanel.add(rightPanel, BorderLayout.EAST);

		gameSelection.setEnabled(!localXml);
		gameNoLabel.setEnabled(!localXml);
	}

	public class MyListener implements ActionListener {
		private int buttonNum;
		private boolean[] catCom = new boolean[6];

		MyListener(int buttonNum) {
			this.buttonNum = buttonNum;
		}

		// All buttons handled here
		public void actionPerformed(ActionEvent e) {
			// Ignore double clicks
			if (isLockout || (System.currentTimeMillis() - clickTime) < debounceMsecs) {
				return;
			}

			clickTime = System.currentTimeMillis();

			// If buzzed in
			if ((gameState == st.RESPONSE_REVEALED) || (gameState == st.CLUE_REVEALED)) {

				// (state no longer showWait causes ShowCorrectResponse to time out)
				waitingShowResponse = false;

			}

			// If in buzzing window, all clicks buzz in
			if (gameState == st.BUZZERS_ARMED) {
				isLockout = true;
				Lockout myRunnable = new Lockout(1000); // >1 sec before reveal
				Thread t = new Thread(myRunnable);
				t.start();

				gameState = st.RESPONSE_REVEALED;
				
				responseMsecs = clickTime - liveTimeStart;
				labResp.setText(String.format("%d", responseMsecs));

				playTone aRunnable = new playTone((int) (2000 - (4 * (responseMsecs/10))), 100, 0.1, true);
				Thread w = new Thread(aRunnable);
				w.start();

				if (responseMsecs < buzzWindowMsecs) {

					// Credit and signal win
					lastWon = true;
					won += squareMoneyValue;
					labWon.setText((String.format("<html>Won=$%d", won)));
				} else {
					// Credit and signal loss
					lastWon = false;
					lost += squareMoneyValue;
					labLost.setText((String.format("<html>Lost=$%d", lost)));
				}
				wasSquare = iSquare;
			}

			// Else all buttons active
			else {
				if ((wasSquare >= 0) && (wasSquare != buttonNum)) {
					sqs[wasSquare].setText("");
					sqs[wasSquare].setBackground(Color.BLUE);
					sqs[wasSquare].setForeground(Color.white);
					squareState[wasSquare] = bs.BLANK;
				}
				switch (buttonNum) {
				case -25: // categories= -buttonNum-20
				case -24:
				case -23:
				case -22:
				case -21:
				case -20: // display category comment
					iSquare = buttonNum;
					int cs = -buttonNum - 20; // category square
					int ci = Math.min(12, cs + (bJDF * 6)); // categories idx
					if (categoryComments[ci].length() > 6) {
						if (catCom[cs]) {
							cats[cs].setBackground(Color.blue);
							setAllText(cats[cs], (categories[ci] + ((categoryComments[ci].length() > 0) ? " *" : "")));
							catCom[cs] = false;
						} else {
							cats[cs].setBackground(colorGreen);
							setAllText(cats[cs], categoryComments[ci]);
							catCom[cs] = true;
						}
					}
					break;
				case -11: // Off-line
					localXml = bOffLine.isSelected();
					gameSelection.setEnabled(!localXml);
					gameNoLabel.setEnabled(!localXml);
					bCopy.setText(localXml ? "Delete" : "Copy");
					break;
				case -10: // help
					try {
						java.awt.Desktop.getDesktop().browse(new java.net.URI("https://help4jsolosim.blogspot.com/"));
					} catch (IOException | URISyntaxException e2) {
					}
					break;
				case -9: // copy
					copy_or_delete(!localXml);
					break;
				case -8: // setup
					new SetupDialog(theFrame, "Setup", true);
					break;
				case -7: // play audio
					bPlay.setEnabled(false);
					playAudio();
					break;
				case -6: // wrong
					if (lastWon) {
						if (squareMoneyValue > 0) {
							won -= squareMoneyValue;
							lost += squareMoneyValue;
							squareMoneyValue = 0;
							labWon.setText((String.format("<html>Won=$%d", won)));
							labLost.setText((String.format("<html>Lost=$%d", lost)));
							if (bJDF < 2) {
								TheyChoose myRunnable = new TheyChoose();
								Thread t = new Thread(myRunnable);
								t.start();
							}
							labGRX.setText(String.format("%d/%d/%d", --gCount, ++rCount, xCount));
							
						}
					}

					break;
				case -5: // N/A
					/*
					 * test area
					 *////////////////////////////////////////////////////////////////
						//////////////////////////////////////////////////////////////
					if (squareMoneyValue > 0) {
						if (lastWon) {
							won -= squareMoneyValue;
							labWon.setText((String.format("<html>Won=$%d", won)));
							gCount--;
						} else {
							lost -= squareMoneyValue;
							labLost.setText((String.format("<html>Lost=$%d", lost)));
							rCount--;
						}
						squareMoneyValue = 0;
						if (!wasLastWon) {
							TheyChoose myRunnable = new TheyChoose();
							Thread t = new Thread(myRunnable);
							t.start();
						}
						labGRX.setText(String.format("%d/%d/%d", gCount, rCount, xCount));
					}
					break;
				case -4: // right
					if (!lastWon) {
						if (squareMoneyValue > 0) {
							won += squareMoneyValue;
							lost -= squareMoneyValue;
							squareMoneyValue = 0;
							labWon.setText((String.format("<html>Won=$%d", won)));
							labLost.setText((String.format("<html>Lost=$%d", lost)));
							labGRX.setText(String.format("%d/%d/%d", ++gCount, --rCount, xCount));
						}
					}
					break;
				case -3: // next
					if (bJDF < (localXml ? 1 : 2)) {
						bJDF++;
					} else {
						if (!localXml) {
							gameSelection.setValue((int) gameSelection.getValue() + 1);
						}
						bJDF = -1;
					}
					newBoard();
					break;
				case -2: // load
					bJDF = -1;
					newBoard();
					break;
				case -1: // prev
					if (bJDF > 0) {
						bJDF--;
					} else {
						if (!localXml) {
							gameSelection.setValue((int) gameSelection.getValue() - 1);
						}
						bJDF = -1;
					}
					newBoard();
					break;
				default: // board buttons 0-30
					if (theySquareNum >= 0) {
						sqs[theySquareNum].setForeground(Color.white);
						cats[theySquareNum % 6].setForeground(Color.white);
						theySquareNum = -1;
					}
					iSquare = buttonNum;
					iClue = Math.min(60, iSquare + (bJDF * 30));
					if (!isDd[iClue]) {
						squareMoneyValue = ((bJDF > 0) ? (bJDF > 1) ? 0 : 2 : 1)
							* (valueBase + ((iSquare / 6) * valueBase));
					}
					switch (squareState[buttonNum]) {
					case VIRGIN:
						if (gameState == st.CLUE_REVEALED) { // too early - lockout penalty

							// If not already locked out
							if (bJDF == 2) {
								setAllText(sqs[buttonNum], responses[iClue]);
								sqs[buttonNum].setBackground(Color.gray);
								squareState[iSquare] = bs.RESPONSE;
							} else if (!isLockout) {
								isLockout = true;

								// Flash pink, play low tone, and lock out the buzzer for a while
								rightPanel.setBackground(Color.PINK);
								playTone aRunnable = new playTone(220, buzzLockoutMsecs, 0.1, false);
								Thread w = new Thread(aRunnable);
								w.start();
								xCount++;
								labGRX.setText(String.format("%d/%d/%d", gCount, rCount, xCount));
								Lockout myRunnable = new Lockout(buzzLockoutMsecs);
								Thread t = new Thread(myRunnable);
								t.start();
							}
							break;
						} else if (gameState != st.RESPONSE_REVEALED) {
							if (ddEnabled && isDd[iClue]) {
								String msg = String.format("%s $amount (0-%d)", ((bJDF < 2) ? "Daily-double" : "Final J!"),
										Math.max(won, ((bJDF < 2) ? (5*valueBase*(1+bJDF)) : won)));
								squareMoneyValue = Integer.parseInt(JOptionPane.showInputDialog(msg));
							}
							gameState = st.CLUE_REVEALED;
							setAllText(sqs[buttonNum], clues[iClue]);
							//sqs[buttonNum].setText(clues[iClue]);
							sqs[buttonNum].setBackground(colorGreen);

							Speak_clue myRunnable = new Speak_clue();
							Thread t = new Thread(myRunnable);
							t.start();

						}
						break;
					case BLANK:
						setAllText(sqs[buttonNum], clues[iClue]);
						sqs[buttonNum].setBackground(colorGreen);
						squareState[iSquare] = bs.CLUE;
						break;
					case CLUE:
						setAllText(sqs[buttonNum], responses[iClue]);
						sqs[buttonNum].setBackground(Color.gray);
						squareState[iSquare] = bs.RESPONSE;
						break;
					case RESPONSE:
						sqs[buttonNum].setText("");
						sqs[buttonNum].setBackground(Color.blue);
						squareState[iSquare] = bs.BLANK;
						break;
					}
					break;
				}
			}
		}
	}
	
	private void setAllText(JButton jButton, String string) {
		
		// While text too big, reduce the font size
		jButton.setFont(new Font("Arial", Font.PLAIN, 20));
		while (true) {
			
			// Calculate the size of a box to hold the text with some padding.
			FontMetrics metrics = jButton.getFontMetrics(jButton.getFont());
			int hgt = metrics.getHeight();
			int adv = metrics.stringWidth(string);
			Dimension tsize = new Dimension(adv, hgt);
			
			// Convert to line counts
			Dimension bsize = jButton.getSize();
			int lines = bsize.height/tsize.height;
			int allLines = 1 + (tsize.width/bsize.width);
			
			if (allLines >= lines) {
				int s = jButton.getFont().getSize();
				jButton.setFont(new Font("Arial", Font.PLAIN, s-1));
			}
			else {
				break;
			}
		}
		jButton.setText(string);
			
	}

	public class Speak_clue implements Runnable {
		public Speak_clue() {
		}

		public void run() {
			try {

				// Speak the clue
				voice.speak(html2text(clues[iClue], true));

				// If not final J!
				if (!isDd[iClue]) {

					// Wait for human to enable buzzers
					Thread.sleep(liveLatencyMsecs);

					// Arm the buzzer
					liveTimeStart = System.currentTimeMillis();
					gameState = st.BUZZERS_ARMED;
					wasLastWon = lastWon;
					jBoard.setBackground(Color.yellow); // buzzers active
					rightPanel.setBackground(Color.yellow); // buzzers active

					// Set timer to close response window
					IsInTime myRunnable = new IsInTime();
					Thread t = new Thread(myRunnable);
					t.start();
				}

				// Set timer 5 (or 30) secs to show correct response
				ShowCorrectResponse myRunnable = new ShowCorrectResponse();
				Thread t = new Thread(myRunnable);
				t.start();
			} catch (Exception ex) {
			}
		}
	}

	public class Lockout implements Runnable {
		int msecs;

		public Lockout(int msecs) {
			this.msecs = msecs;
		}

		public void run() {
			try {
				Thread.sleep(msecs);
				isLockout = false;
				rightPanel.setBackground(Color.lightGray);
			} catch (Exception ex) {
			}
		}
	}

	public class ShowCorrectResponse implements Runnable {
		public ShowCorrectResponse() {
		}

		public void run() {
			waitingShowResponse = true;
			try {
				for (int i = 0; i < ((bJDF > 1) ? 300 : 50); i++) { // 5 sec (30 if fj)
					Thread.sleep(100);
					if (!waitingShowResponse) {
						break;
					}
				}
				capturingAudio = false;
				setAllText(sqs[iSquare], responses[iClue]);
				sqs[iSquare].setBackground(Color.gray);
				squareState[iSquare] = bs.RESPONSE;
				jBoard.setBackground(Color.white);
				rightPanel.setBackground(Color.lightGray);

				// If daily-double or final j!
				if (isDd[iClue]) {

					// Credit and signal win
					lastWon = true;
					won += squareMoneyValue;
					labWon.setText((String.format("<html>Won=$%d", won)));
				}
				
				// Else if timed out
				//else if (waitingShowResponse && (gameState == st.BUZZERS_ARMED)) {
				else if (gameState == st.BUZZERS_ARMED) {
					
					// Credit and signal loss
					lastWon = false;
					lost += squareMoneyValue;
					labLost.setText((String.format("<html>lost=$%d", lost)));
				}
				gameState = lastWon ? st.YOU_CHOOSE : st.THEY_CHOOSE;

				if ((bJDF < 2) && !lastWon) {
					TheyChoose myRunnable = new TheyChoose();
					Thread t = new Thread(myRunnable);
					t.start();
				}
				waitingShowResponse = false;
			} catch (InterruptedException e) {
			}
		}
	}

	public class IsInTime implements Runnable {
		public IsInTime() {
		}

		public void run() {
			try {
				Thread.sleep(buzzWindowMsecs);
			} catch (InterruptedException e) {
			}
			if (gameState == st.BUZZERS_ARMED) {
				jBoard.setBackground(Color.pink);
				lastWon = false;
				rCount++;
			} else {
				jBoard.setBackground(Color.GREEN);
				lastWon = true;
				gCount++;

			}
			rightPanel.setBackground(Color.lightGray);
			labGRX.setText(String.format("%d/%d/%d", gCount, rCount, xCount));
		}
	}

	public class TheyChoose implements Runnable {
		public TheyChoose() {
		}

		public void run() {

			Thread.yield();

			// Get random uncalled square
			int r = -1;
			int i = 0;
			for (; i < 300; i++) {
				r = rand.nextInt(30);
				if (squareState[r] == bs.VIRGIN) {
					break;
				}
			} // endfor i
			if (i == 300) {
				r = -1;
				for (int j = 0; j < 30; j++) {
					if (squareState[j] == bs.VIRGIN) {
						r = j;
						break;
					}
				} // endfor j
			}

			// If found
			if (r >= 0) {

				if (theySquareNum >= 0) {
					sqs[theySquareNum].setForeground(Color.white);
					cats[theySquareNum % 6].setForeground(Color.white);
				}
				theySquareNum = r;
				sqs[r].setForeground(Color.orange);
				cats[r % 6].setForeground(Color.orange);

				try {
					// TTS its category + for + value
					voice.speak(html2text(
							"Let's do, " + cats[r % 6].getText() + ", for " + sqs[r].getText() + ", Alex.", false));
				} catch (Exception ex) {

				}

				if ((iSquare >= 0) && (gameState == st.THEY_CHOOSE)) {
					sqs[iSquare].setText("");
					sqs[iSquare].setBackground(Color.BLUE);
					sqs[iSquare].setForeground(Color.white);
					squareState[iSquare] = bs.BLANK;
				}
			}
		}
	} // endclass TheyChoose

	private void newBoard() {

		gameState = st.YOU_CHOOSE;
		lastWon = true;
		
		// Cancel timers
		waitingShowResponse = false;
		capturingAudio = false;
		responding = false;
		try {
			Thread.sleep(200); // let them time out
		} catch (InterruptedException e) {
		}
		
		// Clear the board 
		for (int i = 0; i < 30; i++) {
			sqs[i].setText("");
			sqs[i].setForeground(Color.WHITE);
			sqs[i].setBackground(Color.BLUE);
			squareState[i] = bs.VIRGIN;
		} // endfor i
		for (int i = 0; i < 6; i++) {
			cats[i].setText("");
			cats[i].setForeground(Color.WHITE);
			cats[i].setBackground(Color.BLUE);
		} // endfor i

		
		if (bJDF < 0) {
			get_game((int) gameSelection.getValue());
			save_params();//???
			bJDF = 0;
			won = 0;
			lost = 0;
			labWon.setText((String.format("<html>Won=$%d", won)));
			labLost.setText((String.format("<html>Lost=$%d", lost)));

		}
		if (bJDF < 2) {
			for (int i = 0; i < 6; i++) {
				int j = i+((bJDF > 0) ? 6 : 0);
				setAllText(cats[i], categories[j] + ((categoryComments[j].length() > 6) ? " *" : ""));
			} // endfor i
			for (int i = 0; i < 30; i++) {
				int j = i+((bJDF > 0) ? 30 : 0);
				if (clues[j].length() > 0) {
					sqs[i].setText(String.format("$%d", (bJDF+1) * (valueBase+((i/6)*valueBase))));
				}
				else {
					squareState[i] = bs.BLANK;
				}
			} // endfor i
		}
		else {
			// Final J!
			for (int i = 0; i < 6; i++) {
				setAllText(cats[i], categories[12] + ((categoryComments[12].length() > 6) ? " *" : ""));
			} // endfor i
			for (int i = 0; i < 30; i++) {
				setAllText(sqs[i], "Final J!");
			} // endfor i
		}
		theFrame.setTitle(newTitle + "    "
				+ ((bJDF > 0) ? (bJDF > 1) ? "Final" : "Double" : "") + " Jeopardy!");
		wasSquare = -1;

	}

	// Copy clue (or category) to local.xml
	public void copy_or_delete(boolean copy) {

		// Input {category, comment, (clue, response)n}
		ArrayList<String> categoryList = new ArrayList<String>();
		int col = (((iSquare < 0) ? -iSquare - 20 : iSquare) % 6);
		int row = (((iSquare < 0) ? 0 : iSquare / 6));
		int cat = Math.min(12, col + (bJDF * 6));
		categoryList.add(categories[cat].replaceAll("<html>", "").replaceAll("\"", "`")); // Jsoup bug workaround?
		categoryList.add(categoryComments[cat].replaceAll("<html>", "").replaceAll("&", "and"));
		for (int i = 0; i < ((iSquare < 0) ? 5 : 1); i++) {
			int cr = Math.min(60, (6 * row) + col + (bJDF * 30));
			if (clues[cr].length() > 0) {
				categoryList.add(clues[cr].replaceAll("<html>", ""));// .replaceAll("&", "and"));
				categoryList.add(responses[cr].replaceAll("<html>", ""));// .replaceAll("&", "and"));
			}
			row++;
		} // endfor
		if (copy) {
			xml.put_category(categoryList);
		} else {
			xml.delete_clues(categoryList);
		}
	}

	public JSoloSim(String name) {
		super(name);
		setResizable(true);
	}

	public void get_game(int num) {
		clear_data();
		if (localXml) {
			get_local_game();
		} else {
			get_j_archive_game(num);
		}
	}

	private void get_local_game() {
		newTitle = new String("OFF-LINE");
		xml.restore_categories();

		// For 13 categories (6 j, 6 dj, 1 fj)
		valueBase = 100;
		for (int i = 0; i < 13; i++) {
			ArrayList<String> cat = xml.get_random_category();
			if (cat.size() > 1) {
				categories[i] = "<html>" + cat.get(0).replaceAll("`", "\""); // Jsoup bug workaround?
				categoryComments[i] = "<html>" + cat.get(1);
				int c = 2;
				int j = (i % 6) + ((i > 5) ? 30 : 0);
				int cnt = 0;
				while ((j < 61) && (c < cat.size())) {
					clues[j] = "<html>" + cat.get(c++);
					responses[j] = "<html>" + cat.get(c++);
					j += 6;

					// If overflow
					if ((++cnt == 5) && (c < cat.size())) {
						cnt = 0;
						if (++i < 13) {
							categories[i] = "<html>" + cat.get(0);
							categoryComments[i] = "<html>" + cat.get(1);
							j = (i % 6) + ((i > 5) ? 30 : 0);
						}
					}
				} // endwhile
			} else {
				break;
			}
		} // endfor
	}

	private void get_j_archive_game(int num) {
		Document doc = null;
		Elements links = null;
		try {
			doc = Jsoup.connect(String.format("http://j-archive.com/showgame.php?game_id=%d", num)).get();
			// org.jsoup.nodes.Element content = doc.getElementById("correct_response");
			links = doc.getElementsByClass("category_name");
			int c = 0;
			for (Element link : links) {
				if (c < 13) {
					categories[c++] = "<html>" + link.text();
				}
			} // endfor links

			links = doc.getElementsByClass("category_comments");
			c = 0;
			for (Element link : links) {
				if (c < 13) {
					categoryComments[c++] = "<html>" + link.text();
				}
			} // endfor links

			links = doc.getElementsByClass("clue_value");
			for (Element link : links) {
				valueBase = Integer.parseInt(link.text().substring(1));
				break;
			} // endfor links

			links = doc.select("[onmouseover]");
			c = 0;
			for (Element link : links) {
				Elements dds = link.getElementsByClass("clue_value_daily_double");
				String linkQ = link.attr("onmouseout");
				int acount = 0;
				int col = 0;
				int row = 0;
				int a = 0;
				String ans = "<html>";
				boolean dj = false;
				for (int i = 0; i < linkQ.length(); i++) {
					if ((linkQ.charAt(i) == '\'') && (linkQ.charAt(i - 1) != '\\')) {
						switch (++acount) {
						case 1:
							if (linkQ.charAt(i + 6) == 'F') {
								col = 0;
								row = 5;
								dj = true;
							} else {
								dj = ((linkQ.charAt(i + 6) == 'D') ? true : false);
								int os = i + (dj ? 1 : 0);
								col = Integer.parseInt(linkQ.substring(8 + os, 9 + os)) - 1;
								row = Integer.parseInt(linkQ.substring(10 + os, 11 + os)) - 1;
							}
							break;
						case 5:
							a = i;
							break;
						case 6:
							ans += linkQ.substring(a + 1, i).replace("\\", "");
						}
					}
				} // endfor
				clues[col + 6 * row + (dj ? 30 : 0)] = new String(ans);

				String linkCR = link.attr("onmouseover").replace("\\", "");
				String cr = "<html>";
				acount = 0;
				a = 18 + linkCR.indexOf("correct_response");
				int b = linkCR.indexOf("</em");
				cr += linkCR.substring(a, b);
				responses[col + 6 * row + (dj ? 30 : 0)] = new String(cr);
				isDd[col + 6 * row + (dj ? 30 : 0)] = (dds.size() > 0);

			} // endfor links
			isDd[60] = true;
		} catch (IOException e) {
		}
		newTitle = new String(doc.title());
		bJDF = 0;
	}

	private static String html2text(String html, boolean spellCaps) {
		String text = Jsoup.parse(html).text().replaceAll("&", "and").replaceAll("--", ", ,");
		StringBuilder speech = new StringBuilder();
		boolean isCaps = false;
		boolean isUnderscore = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (spellCaps && isCaps && (c >= 'A') && (c <= 'Z')) {
				speech.append(" ");
			}

			if (c == '*') {
				continue;
			}

			if (c == '_') {
				isUnderscore = true;
				continue;
			}

			if (isUnderscore) {
				speech.append(", blank ");
			}

			speech.append(c);

			isCaps = ((c >= 'A') && (c <= 'Z'));
			isUnderscore = (c == '_');

		}
		return speech.toString();
		// return Jsoup.parse(html).text().replaceAll("&", "and").replaceAll("__", "_")
		// .replaceAll("__", "_").replaceAll("--", ", ,").replaceAll("_", ", blank ");
	}

	public class playTone implements Runnable {

		private int hz;
		private int msecs;
		private double vol;
		public boolean doCap;

		public playTone(int hz, int msecs, double vol, boolean doCap) {
			this.hz = hz;
			this.msecs = msecs;
			this.vol = vol;
			this.doCap = doCap;
		}

		public void run() {
			byte[] buf = new byte[1];
			final AudioFormat af = new AudioFormat(sampleRateFloat, sampleSizeInBitsInt, channelsInt, signedBoolean,
					bigEndianBoolean);
			SourceDataLine sdl = null;
			try {
				sdl = AudioSystem.getSourceDataLine(af);
				sdl.open(af);
			} catch (LineUnavailableException e) {
			}
			sdl.start();
			for (int i = 0; i < msecs * 8; i++) {
				double angle = i / (sampleRateFloat / hz) * 2.0 * Math.PI;
				buf[0] = (byte) (Math.sin(angle) * 127.0 * vol);
				sdl.write(buf, 0, 1);
			}
			sdl.drain();
			sdl.stop();
			sdl.close();

			// Now start audio capture if enabled
			if (doCap && bCaptureEnable.isSelected()) {
				captureAudio();
			}
		}
	} // endclass playTone

	private void captureAudio() {
		try {
			final AudioFormat format = new AudioFormat(sampleRateFloat, sampleSizeInBitsInt, channelsInt, signedBoolean,
					bigEndianBoolean);

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();
			Runnable runner = new Runnable() {
				int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
				byte buffer[] = new byte[bufferSize];

				public void run() {
					out = new ByteArrayOutputStream();
					capturingAudio = true;
					try {
						while (capturingAudio) {
							int count = line.read(buffer, 0, buffer.length);
							if (count > 0) {
								out.write(buffer, 0, count);
							}
						}
						out.close();
						line.stop();
						line.close();
						if (bCaptureEnable.isSelected()) {
							bPlay.setEnabled(true);
						}
					} catch (IOException e) {
						System.err.println("I/O problems: " + e);
						// bPlay.setEnabled(false);
					}
				}
			};
			Thread captureThread = new Thread(runner);
			captureThread.start();
		} catch (LineUnavailableException e) {
			System.err.println("Line unavailable: " + e);
			// bPlay.setEnabled(false);
		}
	}

	private void playAudio() {
		try {
			byte audio[] = out.toByteArray();
			InputStream input = new ByteArrayInputStream(audio);
			final AudioFormat format = new AudioFormat(sampleRateFloat, sampleSizeInBitsInt, channelsInt, signedBoolean,
					bigEndianBoolean);
			final AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();

			Runnable runner = new Runnable() {
				int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
				byte buffer[] = new byte[bufferSize];

				public void run() {
					try {
						int count;
						while ((count = ais.read(buffer, 0, buffer.length)) != -1) {
							if (count > 0) {
								line.write(buffer, 0, count);
							}
						}
						line.drain();
						line.stop();
						line.close();
					} catch (IOException e) {
						System.err.println("I/O problems: " + e);
						// bPlay.setEnabled(false);
					}
				}
			};
			Thread playThread = new Thread(runner);
			playThread.start();
		} catch (LineUnavailableException e) {
			System.err.println("Line unavailable: " + e);
			// bPlay.setEnabled(false);
		}
	}

	public class SetupDialog extends JDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SetupDialog() {
			super();
		}

		public SetupDialog(JFrame mf, String title, boolean modal) {
			super(mf, title, modal);
			this.setSize(400, 300);
			this.setLocationRelativeTo(sqs[14]);
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(12, 2));
			panel.setBorder(new EmptyBorder(new Insets(8, 8, 8, 8)));

			JCheckBox dd = new JCheckBox();
			dd.setSelected(ddEnabled);
			panel.add(new Label("Daily-doubles:"));
			panel.add(dd);

			SpinnerModel dLimit = new SpinnerNumberModel(buzzWindowMsecs, 1, 10000, 1);
			JSpinner buzzWindow = new JSpinner(dLimit);
			panel.add(new Label("Buzzer window (msecs):"));
			panel.add(buzzWindow);

			SpinnerModel pLimit = new SpinnerNumberModel(buzzLockoutMsecs, 0, 10000, 1);
			JSpinner early = new JSpinner(pLimit);
			panel.add(new Label("Early buzz lockout (msecs):"));
			panel.add(early);

			SpinnerModel lLimit = new SpinnerNumberModel(liveLatencyMsecs, 0, 10000, 1);
			JSpinner latency = new JSpinner(lLimit);
			panel.add(new Label("Live latency (msecs):"));
			panel.add(latency);

			SpinnerModel bLimit = new SpinnerNumberModel(debounceMsecs, 0, 10000, 1);
			JSpinner debounce = new JSpinner(bLimit);
			panel.add(new Label("Debounce max (msecs):"));
			panel.add(debounce);

			JButton del = new JButton("Delete file");
			del.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int c = JOptionPane.showConfirmDialog(mf,
							"<html>Are you sure? (may be necessary to fix corrupt file, but data will be lost)");
					if (c == 0) {
						File file = new File("local.xml");
						file.delete();
						xml.doc = Jsoup.parse("<html><head></head><categories></categories><body></body></html>");
						xml.dirty = false;
					}
				}
			});
			panel.add(new Label("Local database:"));
			panel.add(del);

			NumberFormat nFormat = NumberFormat.getIntegerInstance();
			JTextField sampleRate = new JFormattedTextField(nFormat);
			sampleRate.setText(String.format("%.0f", sampleRateFloat));
			sampleRate.setColumns(5);
			panel.add(new Label("Audio sample rate:"));
			panel.add(sampleRate);

			JTextField sampleSizeInBits = new JFormattedTextField(nFormat);
			sampleSizeInBits.setText(String.format("%d", sampleSizeInBitsInt));
			sampleSizeInBits.setColumns(4);
			panel.add(new Label("Audio sample size (bits):"));
			panel.add(sampleSizeInBits);

			JTextField channels = new JFormattedTextField(nFormat);
			channels.setText(String.format("%d", channelsInt));
			channels.setColumns(2);
			panel.add(new Label("Audio channels:"));
			panel.add(channels);

			JCheckBox signed = new JCheckBox();
			signed.setSelected(signedBoolean);
			panel.add(new Label("Audio signed:"));
			panel.add(signed);

			JCheckBox bigEndian = new JCheckBox();
			bigEndian.setSelected(bigEndianBoolean);
			panel.add(new Label("Audio bigendian:"));
			panel.add(bigEndian);

			JButton ok = new JButton("Save");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ddEnabled = dd.isSelected();
					buzzWindowMsecs = (int) buzzWindow.getValue();
					buzzLockoutMsecs = (int) early.getValue();
					liveLatencyMsecs = (int) latency.getValue();
					debounceMsecs = (int) debounce.getValue();
					sampleRateFloat = Float.parseFloat(sampleRate.getText().trim());
					sampleSizeInBitsInt = Integer.parseInt(sampleSizeInBits.getText().trim());
					channelsInt = Integer.parseInt(channels.getText().trim());
					signedBoolean = signed.isSelected();
					bigEndianBoolean = bigEndian.isSelected();
					save_params();
					dispose();
				}
			});
			panel.add(ok);

			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			panel.add(cancel);

			this.getContentPane().add(panel);
			this.setVisible(true);
		}
	}

	private static void loadParams() {
		File file = new File("params.txt");
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
			String text = null;

			text = reader.readLine(); // capture audio
			bCaptureEnable.setSelected(Boolean.parseBoolean(text.trim()));
			text = reader.readLine(); // capture audio
			ddEnabled = Boolean.parseBoolean(text.trim());
			text = reader.readLine(); // game id
			gameSelection.setValue(Integer.parseInt(text.trim()));
			text = reader.readLine(); // buzz window
			buzzWindowMsecs = Integer.parseInt(text.trim());
			text = reader.readLine(); // early hold-off
			buzzLockoutMsecs = Integer.parseInt(text.trim());
			text = reader.readLine(); // latency
			liveLatencyMsecs = Integer.parseInt(text.trim());
			text = reader.readLine(); // debounce
			debounceMsecs = Integer.parseInt(text.trim());
			text = reader.readLine(); // sample rate
			sampleRateFloat = Float.parseFloat(text.trim());
			text = reader.readLine(); // sample size
			sampleSizeInBitsInt = Integer.parseInt(text.trim());
			text = reader.readLine(); // channels
			channelsInt = Integer.parseInt(text.trim());
			text = reader.readLine(); // signed
			signedBoolean = Boolean.parseBoolean(text.trim());
			text = reader.readLine(); // big endian
			bigEndianBoolean = Boolean.parseBoolean(text.trim());
			gameSelection.setEnabled(!localXml);
			gameNoLabel.setEnabled(!localXml);
			bCopy.setText(localXml ? "Delete" : "Copy");
		} catch (FileNotFoundException e) {
			save_params();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
	}

	private static void save_params() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("params.txt", "UTF-8");
			writer.println(bCaptureEnable.isSelected() ? "true" : "false");
			writer.println(ddEnabled ? "true" : "false");
			writer.println(gameSelection.getValue().toString());
			writer.println(String.format("%d", buzzWindowMsecs));
			writer.println(String.format("%d", buzzLockoutMsecs));
			writer.println(String.format("%d", liveLatencyMsecs));
			writer.println(String.format("%d", debounceMsecs));
			writer.println(String.format("%6.0f", sampleRateFloat));
			writer.println(String.format("%d", sampleSizeInBitsInt));
			writer.println(String.format("%d", channelsInt));
			writer.println(signedBoolean ? "true" : "false");
			writer.println(bigEndianBoolean ? "true" : "false");
			writer.println(localXml ? "true" : "false");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e1) {
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (Exception e) {
			}
		}

	}
}
