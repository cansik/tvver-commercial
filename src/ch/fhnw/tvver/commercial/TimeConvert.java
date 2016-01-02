package ch.fhnw.tvver.commercial;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import ch.fhnw.util.TextUtilities;

public class TimeConvert {

	public static void main(String[] args) {
		JFrame     frameUI   = new JFrame("Time Convert");
		JTextField fpsUI     = new JTextField("25");
		JTextField inputUI   = new JTextField(10);
		JLabel     outputUI  = new JLabel("");
		JLabel     frameNoUI = new JLabel("");

		frameUI.setLayout(new GridLayout(0, 2));
		frameUI.add(new JLabel("FPS"));
		frameUI.add(fpsUI);
		frameUI.add(new JLabel("Input"));
		frameUI.add(inputUI);
		frameUI.add(new JLabel("Output"));
		frameUI.add(outputUI);
		frameUI.add(new JLabel("Frame #"));
		frameUI.add(frameNoUI);

		inputUI.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e)    {keyPressed(e);}
			@Override public void keyReleased(KeyEvent e) {keyPressed(e);}
			
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					double fps  = Double.parseDouble(fpsUI.getText());
					double secs = 0;
					long   frameNo;
					String input = inputUI.getText();
					if(input.indexOf(':')  < 0) {
						secs    = Double.parseDouble(input);
						frameNo = (long) (secs * fps);
						int hrs = (int) (secs / 3600);
						secs -= hrs * 3600;
						int mins = (int) (secs / 60);
						secs -= mins * 60;
						outputUI.setText(String.format("%02d:%02d:%02d", hrs, mins, (int)secs));
					} else {
						String[] parts = input.split(":");
						if(parts.length > 0) {
							secs = Integer.parseInt(parts[parts.length - 1]);
							if(parts.length > 1) {
								secs += Integer.parseInt(parts[parts.length - 2]) * 60;
								if(parts.length > 2) {
									secs += Integer.parseInt(parts[parts.length - 3]) * 3600;
								}
							}
						}
						frameNo = (long) (secs * fps);
						outputUI.setText(TextUtilities.decimalFormat(2).format(secs));
					}
					frameNoUI.setText(Long.toString(frameNo));
				} catch(Throwable t) {}
			}
		});
		frameUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameUI.pack();
		frameUI.setVisible(true);
	}

}
