package game;

/*
 * Define las primitivas basicas de un auto
 * considera la direccion de avanze como +y
 * el eje x es los lados 
 */

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

public class Car implements Serializable {

	private static final long serialVersionUID = -228388699055817989L;
	protected Point2D.Float velocity;
	protected float ay, min_vy, top_vy;
	protected float tx;
	protected float fuel,max_fuel;
	protected Rectangle2D.Float boundingBox;
	
	public Car(float ay,float min_vy, float top_vy, float tx,float width, float height,float max_fuel) {
		this.velocity = new Point2D.Float(0, min_vy);
		this.ay = ay;
		this.top_vy = top_vy;
		this.min_vy = min_vy;
		this.tx = tx;
		this.boundingBox = new Rectangle2D.Float(0, 0, width, height);
		this.fuel = max_fuel;
		this.max_fuel = max_fuel;
	}
	
	public void reset(){
		this.fuel = this.max_fuel;
		this.velocity.x = 0;
		this.velocity.y = 0;
	}
	
	public int getRemainingFuel(){
		return (int) this.fuel;
	}
	
	public void setX(float x) {
		this.boundingBox.x = x;
	}
	
	public void setY(float y) {
		this.boundingBox.y = y;
	}
	
	public float getWidth() {
		return this.boundingBox.width;
	}
	
	public float getHeight() {
		return this.boundingBox.height;
	}
	
	public Rectangle2D.Float getBox(){
		return this.boundingBox;
	}
	
	public float getX() {
		return this.boundingBox.x;
	}
	
	public float getY() {
		return this.boundingBox.y;
	}	
	
	public float getVx() {
		return this.velocity.x;
	}
	
	public float getVy() {
		return this.velocity.y;
	}
	
	public Point2D.Float getVelocity(){
		return this.velocity;
	}
	
	public void stop(){
		this.velocity.x = 0;
		this.velocity.y = 0;
	}
	
	public void setVelocity(Point2D.Float velocity) {
		this.velocity = velocity;
	}
	
	public void move(){
		if(fuel>0){
			this.boundingBox.x+=this.velocity.x;
			this.boundingBox.y+=this.velocity.y;
			this.velocity.x = 0;
			fuel-=(this.velocity.y*this.velocity.y/650+this.velocity.x);
		} else {
			fuel = 0;
			deacelerate();
			this.boundingBox.y+=this.velocity.y;			
		}
		
	}
	
	public int getHUDVelocity(){
		return (int)(this.velocity.y*2);
	}
	
	public void acelerate(){
		this.velocity.y+=ay;
		if(this.velocity.y>top_vy){
			this.velocity.y = top_vy;
		}
	}
	
	public void deacelerate(){
		this.velocity.y-=ay;
		if(this.velocity.y<0){
			this.velocity.y = 0;
		}
	}
	
	public void turnRight(){
		this.velocity.x=tx;
	}
	
	public void turnLeft(){
		this.velocity.x=-tx;
	}
	
	public boolean collides(Car b){
		return this.boundingBox.intersects(b.getBox());
	}
	
	public void setPosition(float x, float y){
		this.boundingBox.x = x;
		this.boundingBox.y = y;
	}
	
	public void setPosition(Point2D.Float p){
		this.boundingBox.x = p.x;
		this.boundingBox.y = p.y;
	}
	
	public Point2D.Float getPosition(){
		return new Point2D.Float(this.boundingBox.x, this.boundingBox.y);
	}
	
	/**
	 * Mide la disctancia que hay hasta el auto c. Una distancia positiva indica que el auto c esta al frente.
	 * @param c Auto contra el cual hay que medir la distancia
	 * @return
	 */
	public float distanceY(Car c){
		return c.getY()-this.getY();
	}
	
	public float getTx() {
		return tx;
	}
	
	@Override
	public String toString() {
		return "pos ("+this.boundingBox.x+","+this.boundingBox.y+")\n";
	}
}
