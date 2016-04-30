import java.awt.BorderLayout;
import java.awt.MouseInfo;
import java.awt.Robot;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;

import evebot.MinerBot;


public class TorrentRestarts {

	
	public static void main(String[] args) throws Exception
	{
		
		JFrame frame = new JFrame("TORRENT RESTARTS");
		
		JLabel timerLabel = new JLabel("...............");
		frame.setLayout(new BorderLayout());
		frame.add(timerLabel, BorderLayout.NORTH);
		JSpinner spinner = new JSpinner();
		spinner.setValue(240);
		frame.add(spinner, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		MinerBot r = new MinerBot();
		while(true)
		{
			if((Integer) spinner.getValue() < 10)
				spinner.setValue(10);
			for(int i = 0; i < (Integer) spinner.getValue(); i++)
			{
				Thread.sleep(1000);
				timerLabel.setText("Time left: " + ((Integer)spinner.getValue() - i));
				if((Integer) spinner.getValue() < 10)
					spinner.setValue(10);
			}
			r.click(258, 56);
			Thread.sleep(500);
			r.click(195, 56);
		}
		
		
	}
	
	public static void spamMousePosition() throws Exception
	{
		Robot r = new Robot();
		
		while(true)
		{
			System.out.println(MouseInfo.getPointerInfo().getLocation());
			Thread.sleep(1000);
		}
		
	}
	
}
