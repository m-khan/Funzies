package evebot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class LocalWatchBot {

	public static void main(String[] args) throws Exception{
		System.out.println("Set local position...                                        ");

		JFrame frame = new JFrame("Local Watch Bot");
		
		JLabel label = new JLabel("PUT MOUSE OVER LOCAL TOP                  ");
		frame.setLayout(new BorderLayout());
		frame.add(label, BorderLayout.NORTH);
		JCheckBox checkbox = new JCheckBox("SHUT UP");
		frame.add(checkbox, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
        Robot robot = new Robot();
        robot.delay(5000);
        Toolkit.getDefaultToolkit().beep();

        Point localTop = MouseInfo.getPointerInfo().getLocation();
        System.out.println("Local top set at: " + localTop);
        label.setText("Local top set at: " + localTop);
        
        robot.delay(2000);
        Toolkit.getDefaultToolkit().beep();
        
        Point localBottom = MouseInfo.getPointerInfo().getLocation();
        System.out.println("Local bottom set at: " + localBottom);
        label.setText("WATCHING...");
        
        Point cP = new Point(localTop); // currentPosition
        boolean detected = false;
        
        while(true) {
            Color color = robot.getPixelColor((int)cP.getX(), (int)cP.getY());
            if(color.getGreen() + color.getBlue() < color.getRed()) {
                System.out.println("Hostile Detected");
                if(!checkbox.isSelected()) Toolkit.getDefaultToolkit().beep();
                System.out.println("Color: "+ color);
                detected = true;
            }
            robot.delay(2);
            
            if(cP.getY() > localBottom.getY()){
            	cP.y = localTop.y;
                if(detected) label.setText("Hostile Detected!");
                else label.setText("WATCHING...");
            	detected = false;
            }
            else {
            	cP.y = cP.y + 3;
            }
        }
		
	}
	
}
