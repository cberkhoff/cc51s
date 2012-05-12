package core;

import game.Car;
import game.HumanPlayer;
import game.Race;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.Timer;

public class SpawnImpl extends UnicastRemoteObject implements Spawn, ActionListener {
	
	private static final long serialVersionUID = 6898087174470775158L;
	public enum Status{OFFLINE, ACTIVE, INACTIVE} 
	protected transient ClientImpl myClient;
	protected Status status;
	protected String rminame;
	protected int nextSpawn;
	protected Race race;	
	protected Client[] clients;
	protected int ticksToMigrate = 600; 
	protected int tickCount;
	protected transient Timer t;
	private int loglvl = 6;
	
	protected int pauseAuthor;
	protected boolean isPaused;
	
	public SpawnImpl(ClientImpl c) throws RemoteException{ 
		this.status = Status.OFFLINE;	
		this.myClient = c;
		this.clients = null;
		this.race = null;
		this.nextSpawn = -1;
		this.isPaused = false;
		pauseAuthor = -1;
		t = new Timer(50, this);
		this.rminame = "u.cc51s.spawn."+c.getId();
	}
	
	@Override
	public String getRMIName() throws RemoteException{
		return rminame;
	}

	public void setRMIName(String rminame) {
		this.rminame = rminame;
	}
	
	@Override
	public Race getRace() throws RemoteException {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	@Override
	public Client[] getClients() throws RemoteException{
		return clients;
	}

	public void setClients(Client[] clients) {
		this.clients = clients;
	}
	
	public void stop(){
		t.stop();
		this.status = Status.INACTIVE;
		resetTick();
	}
	
	public void restart(){
		if(this.status==Status.ACTIVE)
			t.start();
	}
	
	public void restore(ClientImpl c) throws RemoteException{
		this.myClient = c;
		if(this.status!=Status.OFFLINE)
			this.clients[c.getId().getI()] = c;
		t = new Timer(50, this);
		/* si yo era el spawn activo tambien tengo que informar al servidor que volvi */
		if(this.status==Status.ACTIVE)
			myClient.getServer().setCurrentSpawn(this);
	}
	
	@Override
	public void updateAndContinue(Server s) throws RemoteException {
		this.myClient.getServer().setCurrentSpawn(this);
		this.clients = s.getClients();
		this.race = s.getRace();
		this.nextSpawn = s.getNextSpawn();
		resetTick();
		t.start();
		this.status = Status.ACTIVE;
	}
	
	@Override
	public void updateAndContinue(Spawn s) throws RemoteException {
		this.myClient.getServer().setCurrentSpawn(this);
		this.clients = s.getClients();
		this.race = s.getRace();
		this.nextSpawn = s.getNextSpawn();
		resetTick();
		t.start();
		this.status = Status.ACTIVE;
	}
	
	private void migrate() throws RemoteException{
		t.stop();
		log(2,"Migrando.");
		this.myClient.notifySpawnMigration();
		this.status = Status.INACTIVE;
		do{
			nextSpawn = (1+nextSpawn)%clients.length;
		} while(clients[nextSpawn]==null);
		Client chosen = clients[nextSpawn];
		if(chosen==null){
			System.err.println("[Spawn] siguiente elegido es nulo");
		}
		log(1,"Enviando Spawn a Cliente \""+chosen.getName()+"\" ( "+chosen.getLocalhost()+":"+chosen.getPort()+" )");
		chosen.receiveSpawn(this);
		log(2,"Envio Completado");
	}
	
	public synchronized void actionPerformed(ActionEvent arg0) {
		int client_i = -1;
		try {
			/* Primero chequeo que no este pausado. Si estoy pausado ignoro todo el resto */
			if(isPaused){
				if(clients[pauseAuthor].getPressedKey() == 32)
					resume();
				else
					return;
			}
			
			//Actualizo los autos deacuerdo a las acciones de los usuarios
			for(client_i=0;client_i<clients.length;client_i++){
				if(clients[client_i]!=null){
					int key = clients[client_i].getPressedKey();
					if(key!=-1){
						Car c = race.getHumanPlayer(client_i).getCar();
						//37 - left arrow; 38 - up arrow; 39 - right arrow; 40 - down arrow;
				        // 32 - space bar
						if(key==37){
							c.turnLeft();
						} else if(key==38){
							c.acelerate();
						} else if(key==39){
							c.turnRight();
						}  else if(key==40){
							c.deacelerate();
						} else if(key ==32){
							pause(client_i);
						} 
						log(6,"Se presiono tecla: "+key);
					}
				}
			}
			//actualizo la carreras
			race.mainCicle();
			
			//les envio el resultado
			for(client_i=0;client_i<clients.length;client_i++){
				if(clients[client_i]!=null){
					clients[client_i].updateGame(race,race.getHumanPlayer(client_i),(isPaused?race.getHumanPlayer(pauseAuthor):null));
				}
			}
		}catch (RemoteException e){
			if(client_i==-1)
				return;
			log(0,"El jugador llamado "+race.getHumanPlayer(client_i).getName()+" se ha desconectado");
			clients[client_i] = null;
			race.getHumanPlayer(client_i).getCar().stop();
			try {
				this.myClient.getServer().removeClient(client_i);
			} catch (RemoteException e1) {
				System.err.println("[Spawn] No fue posible advertir al servidor de la desconeccion");
				e1.printStackTrace();
			}
		}
		
		//hay algun ganador?
		if(race.isThereAWinner()){
			try {
				this.myClient.getServer().endSession();
				stop();
				return;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		//reviso si es hora de emigrar
		if(isTimeToMigrate()){
			try {
				migrate();
				return;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		tickCount++;
	}
	
	private void resetTick(){
		this.tickCount = 0;
	}
	
	private boolean isTimeToMigrate(){
		return tickCount >= ticksToMigrate;
	}
	
	@Override
	public int getNextSpawn() throws RemoteException {
		return nextSpawn;
	}
	
	private void log(int lvl, String msg){
		if(lvl<=loglvl){
			System.out.println("[Spawn] "+msg);
		}
	}

	@Override
	public synchronized boolean reconnect(Client c) throws RemoteException {
		//logClientStatus();
		log(0,"Usuario con Id "+c.getId()+" se esta reconectando");
		if(clients[c.getId().getI()]==null){
			clients[c.getId().getI()] = c;
			//logClientStatus();
			return true;
		} else {
			//logClientStatus();
			return false;
		}
	}
	
	private void logClientStatus() throws RemoteException{
		for(int i=0;i<clients.length;i++){
			if(clients[i]==null)
				log(5,"clients["+i+"]=null");
			else
				log(5,"clients["+i+"]="+clients[i].getId());
		}
		System.out.println();
	}
	
	public Status getStatus(){
		return status;
	}
	
	private void pause(int i){
		if(!isPaused){
			isPaused = true;
			pauseAuthor = i;
		}
	}
	
	private void resume(){
		pauseAuthor = -1;
		isPaused = false;
	}

	@Override
	public boolean isPaused() throws RemoteException {
		return isPaused;
	}

	@Override
	public HumanPlayer pauseAuthor() throws RemoteException {
		if(isPaused)
			return race.getHumanPlayer(pauseAuthor);
		else
			return null;
	}
}
