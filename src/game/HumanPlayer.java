package game;

public class HumanPlayer extends Player {

	private static final long serialVersionUID = 3956447736817677649L;
	protected String name;
	
	public HumanPlayer(String name){
		super(new Car(5,5,50,10,40,80,2000));
		this.name = name;
	}
	
	public String getName() {
		return name;
	}	
	
	public boolean isHuman(){
		return true;
	}
}
