package pl.edu.agh.jobshop;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jacop.core.IntVar;

import pl.edu.agh.jobshop.JobshopScheduling.Machine;

@SuppressWarnings("serial")
public class Graph extends JFrame {
	
	List<Machine> m;
	private int delta;

	public Graph(List<Machine> m, int delta) {
		super("plotter");
		this.m = m;
		this.delta = delta;
		setSize(1500, 800);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		add(new DrawingPane());
		
		setVisible(true);
	}
	
	
	private class DrawingPane extends JPanel {
		
		@Override
		public void paint(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			
			g.setColor(Color.BLACK);
			g.drawLine(20, 20, 20, getHeight()-40);
			g.drawLine(20, getHeight()-40, getWidth()-40, getHeight()-40);
			
			int frameHeight = (getHeight() - 40)/m.size()-20;
			int widthMultiplyer = 20;
			
			Color[] colors = new Color[]{Color.pink,Color.cyan,Color.blue,Color.green,Color.red,Color.magenta, Color.yellow, Color.black, Color.gray};
			
			
			for (int i = 0; i < m.size(); i++) {
				g.drawString(""+i, 5, frameHeight*(i+1)-frameHeight/2);
				
				for (IntVar[] rectangle : m.get(i).rect.getRectangles()) {
					if ((rectangle[0].value()+rectangle[2].value())<=delta)
					{
						int x = rectangle[0].value();
						int width = rectangle[2].value();
						
						String text = rectangle[0].id();
						Random rand = new Random();
						int color = Integer.parseInt(text.substring(1,2));
						//int color = rand.nextInt(7);
						g.setColor(colors[color-1]);
						g.drawRect(x*widthMultiplyer+20, frameHeight*i+20, width*widthMultiplyer, frameHeight-20);
						g.setColor(Color.black);
						g.drawString(text, x*widthMultiplyer+30, frameHeight*i+60);
					}
					
				}
				
				
				
			}
			
		
		}
		
	}
	
}
