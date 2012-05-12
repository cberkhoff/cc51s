package game;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class Race implements Serializable{
	
	private static final long serialVersionUID = 562275732456844041L;
	protected Player[] players; 
	protected AIPlayer[] aiplayers;
	protected HumanPlayer[] humanplayers;
	
	int length,width;
	
	public Race(int n_humanplayers, int n_aiplayers, int width, int length){
		this(new HumanPlayer[n_humanplayers],n_aiplayers,width,length);
	}
	
	public Race(HumanPlayer[] humanplayers, int n_aiplayers, int width, int length){
		this.humanplayers = humanplayers;
		this.length = length;
		this.width = width;
		aiplayers = new AIPlayer[n_aiplayers];
		players = new Player[n_aiplayers+humanplayers.length];
		
		for(int i=0;i<n_aiplayers;i++){
			aiplayers[i] = new AIPlayer();
		}
		for(int i=0;i<humanplayers.length;i++){
			players[i] = humanplayers[i];
		}
		for(int i=0;i<aiplayers.length;i++){
			players[i+humanplayers.length] = aiplayers[i];
		}
		//reset();
	}
	
	public AIPlayer[] getAIPlayers() {
		return aiplayers;
	}
	
	public HumanPlayer[] getHumanPlayers() {
		return humanplayers;
	}
	
	public Player[] getAllPlayers() {
		return players;
	}
	
	public void mainCicle(){
		//ia piensa
		for(int i =0;i<aiplayers.length;i++){
			aiplayers[i].think(this);
		}
		
		//se mueven
		for(int i =0;i<players.length;i++){
			players[i].getCar().move();
		}
		
		//se resuelven choques
		for(int i =0;i<players.length;i++){
			Car c = testCollision(players[i].getCar());
			if(c!=null){
				crash(players[i].getCar(),c);
			}
		}
		
		//no se pueden salir de la carrera
		for(int i =0;i<players.length;i++){
			if(players[i].getCar().getBox().getMinX()<0){
				players[i].getCar().setX(0);
			} else if(players[i].getCar().getBox().getMaxX()>width){
				players[i].getCar().setX(width - players[i].getCar().getBox().width);
			}
		}
	}
	
	public boolean isThereAWinner(){
		for(int i =0;i<humanplayers.length;i++){
			if(humanplayers[i].getCar().getBox().getMaxY()>length){
				return true;
			}
		}
		return false;
	}
	
	public HumanPlayer getWinner(){
		HumanPlayer w = humanplayers[0];
		for(int i =1;i<humanplayers.length;i++){
			if(humanplayers[i].getCar().getBox().getMaxY()>w.getCar().getBox().getMaxY()){
				w = humanplayers[i];
			}
		}
		return w;
	}
	
	public void reset(){
		//autos de jugadores en la linea de partida y rellenamos tanques de combustible
		int n_autos = 4;
		int rampa_height = 200;
		for(int i = 0;i<players.length;i++){
			players[i].getCar().reset();
			players[i].getCar().setPosition(
					(float)(((i%n_autos+0.5)*width)/n_autos),
					(float)(Math.floor(i/n_autos)*rampa_height));
		}
	}
	
	private Car testCollision(Car c){
		for(int i=0;i<players.length;i++){
			if(c!=players[i].getCar()){
				if(c.collides(players[i].getCar())){
					return players[i].getCar();
				}
			}
		}
		return null;
	}
	
	private void crash(Car a, Car b){
		//los separo
		Point2D.Float mid = new Point2D.Float(
				(float) ((a.getBox().getCenterX()+b.getBox().getCenterX())/2),
				(float) ((a.getBox().getCenterY()+b.getBox().getCenterY())/2));
		
		Car[] cars = {a,b};
		Point2D.Float[] diff = new Point2D.Float[2];
		for(int i =0;i<2;i++){
			diff[i] = new Point2D.Float(
				(float)(mid.x-cars[i].getBox().getCenterX()),
				(float)(mid.y-cars[i].getBox().getCenterY()));
			//normalizo las distancias
			double n = diff[i].distance(0, 0);
			diff[i].x/=n;
			diff[i].y/=n;
		}
		
		Point2D.Float[] orig = new Point2D.Float[2];
		orig[0] = a.getPosition();
		orig[1] = b.getPosition();
		
		for(int d=1;a.collides(b);d++){
			for(int i=0;i<2;i++){
				a.setPosition(d*diff[i].x+orig[i].x, d*diff[i].y+orig[i].y);
			}
		}
		
		//redefino sus velocidades (swap)
		Point2D.Float v_aux = a.getVelocity();
		a.setVelocity(b.getVelocity());
		b.setVelocity(v_aux);
	}
	
	public int getLength() {
		return length;
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setHumanplayers(HumanPlayer[] humanplayers) {
		this.humanplayers = humanplayers;
		for(int i=0;i<humanplayers.length;i++){
			players[i] = humanplayers[i];
		}
	}
	
	public HumanPlayer getHumanPlayer(int i){
		return humanplayers[i];
	}
	
	public void setHumanPlayer(int i,HumanPlayer p){
		//debo actualizar ambas referencias
		humanplayers[i] = p;
		players[i] = p;
	}
}
