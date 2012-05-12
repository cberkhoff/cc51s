package game;

public class AIPlayer extends Player {

	private static final long serialVersionUID = -1504104903730078502L;
	float t,x_actual;

	public AIPlayer(){
		super(new Car(
				(float)(Math.random()*2+1),
				2,
				(float)(Math.random()*10+32),
				(float)(Math.random()*1+2),
				40,
				80,
				2000));
		t = (float) (Math.random()*Math.PI/2);
		x_actual = 0;
	}
	
	public void think(Race g){
		//v3 acelera y hace slalom
		c.acelerate();
		float x_esperado = (float) (200*Math.cos(t));
		if(Math.abs(x_esperado-x_actual)>c.getTx()){
			if(x_esperado-x_actual>0){
				c.turnRight();
				x_actual+=c.getTx();
			} else {
				c.turnLeft();
				x_actual-=c.getTx();
			}
		} 
		t+=0.03;
		
		//v2 acelera hasta un limite
		/*if(c.velocity.y<40)
			c.acelerate();
		else
			c.deacelerate();*/
		
		//v1 acelera
		//c.acelerate();
	}
	
}
