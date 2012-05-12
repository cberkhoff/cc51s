package ui;

import game.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;


public class RaceCanvas extends JPanel{

	private static final long serialVersionUID = 5059379448392144767L;
	
	protected Race r;
	protected Player p;
	protected int width, height;
	
	protected int y_offset = 100;
	
	protected Color roadColor = Color.gray;
	protected Color stripeColor = Color.white;
	
	private int stripeSize = 50;
	private int stripeWidth = 10;
	private int stripeGap = 50;
	
	BufferedImage HumanCar,AICar;
	
	public RaceCanvas(int width,int height){
		super(true);
		this.r = null;
		this.p = null;
		this.width = width;
		this.height = height;
		this.setBackground(roadColor);
		
		File myCar1 = new File("src/resources/myCar.png");
		File myCar2 = new File("resources/myCar.png");
		File enemyCar1 =  new File("src/resources/EnemyCar.png");
		File enemyCar2 =  new File("resources/EnemyCar.png");
		
		
		try {
			if(myCar1.exists())
				HumanCar = ImageIO.read(myCar1);
			else
				HumanCar = ImageIO.read(myCar2);
			
			if(enemyCar1.exists())
				AICar = ImageIO.read(enemyCar1);
			else
				AICar = ImageIO.read(enemyCar2);
        } catch (IOException e) {
            System.out.println("No se pudo leer imagen");
            System.exit(1);
        }

		
		setSize(width, height);
		setPreferredSize(new java.awt.Dimension(width, height));
	}
	
	public RaceCanvas(Race r,Player p,int width,int height){
		this(width,height);
		this.r = r;
		this.p = p;
		
	}
	
	public void updateRacePlayer(Race r,Player p){
		this.r = r;
		this.p = p;
	}
	
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		//Graphics g = this.getGraphics();
		//paintComponent(g);
		if(this.r!=null&&this.p!=null){
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(0, height+p.getCar().getY()-y_offset);
			
			drawRoad(g2d);
			drawCars(g2d);
		}
	}
	
	private void drawRoad(Graphics2D g) {
        g.setColor(stripeColor);
        for(int i=stripeSize;i<r.getLength();i+=(stripeSize+stripeGap)){
        	g.fillRect((int)((r.getWidth()-stripeWidth)/2),-i, stripeWidth, stripeSize);
        }
    }
	
	private void drawCars(Graphics2D g){
		Player[] players = r.getAllPlayers();
		for(int i=0;i<players.length;i++){
			g.drawImage(
					players[i].isHuman()?HumanCar:AICar,
					(int)(players[i].getCar().getX()), 
					(int)(-players[i].getCar().getY()), 
					(int)(players[i].getCar().getWidth()),
					(int)(players[i].getCar().getHeight()),
					null);
			/*g.setColor(Color.red);
			g.fillRect((int)(players[i].getCar().getX()), 
					(int)(-players[i].getCar().getY()), 
					(int)(players[i].getCar().getWidth()),
					(int)(players[i].getCar().getHeight()));*/
		}
	}
}
