package evebot;

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.Toolkit;

public class LocalWatchBot {

	public static void main(String[] args) throws Exception{
		System.out.println("Set local position...");

		PointerInfo pointer;
        pointer = MouseInfo.getPointerInfo();

        Robot robot = new Robot();
        robot.delay(5000);
        Toolkit.getDefaultToolkit().beep();

        Point localTop = MouseInfo.getPointerInfo().getLocation();
        System.out.println("Local top set at: " + localTop);

        robot.delay(2000);
        Toolkit.getDefaultToolkit().beep();
        
        Point localBottom = MouseInfo.getPointerInfo().getLocation();
        System.out.println("Local bottom set at: " + localBottom);
        
        Point cP = new Point(localTop); // currentPosition
        
        while(true) {
            Color color = robot.getPixelColor((int)cP.getX(), (int)cP.getY());
            if(color.getGreen() + color.getBlue() < color.getRed()) {
                System.out.println("Hostile Detected");
                Toolkit.getDefaultToolkit().beep();
                System.out.println("Color: "+ color);
            }
            robot.delay(2);
            
            if(cP.getY() > localBottom.getY()){
            	cP.y = localTop.y;
            }
            else {
            	cP.y = cP.y + 3;
            }
        }
		
	}
	
}
