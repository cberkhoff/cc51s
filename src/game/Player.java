package game;

import java.io.Serializable;

public class Player implements Serializable{
	private static final long serialVersionUID = 3299131915324551619L;
	
	protected Car c;
	
	public Player(Car c){
		this.c = c;
	}
	
	public Car getCar() {
		return c;
	}
	
	public boolean isHuman(){
		return false;
	}
}
