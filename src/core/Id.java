package core;

import java.io.Serializable;
import java.util.Date;

public class Id implements Serializable{

	private static final long serialVersionUID = 118204797569254816L;
	
	protected int i;
	protected Date creation;
	protected int r;
	
	public Id(int i){
		this.i = i;
		this.creation = new Date();
		this.r = (int)(Math.random()*1000000);
	}
	
	public boolean equals(Id x){
		return this.i==x.getI() && this.creation.equals(x.getCreation()) && this.r==x.getR();
	}

	public int getI() {
		return i;
	}

	public Date getCreation() {
		return creation;
	}

	public int getR() {
		return r;
	}
	
	public String toString(){
		return i+"-"+creation+"-"+r;
	}
	
}
