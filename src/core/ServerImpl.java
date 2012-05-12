package core;

import game.HumanPlayer;
import game.Race;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class ServerImpl extends UnicastRemoteObject implements Server {
	
	private static final long serialVersionUID = -6331011358835279138L;
	protected final int RACE_WIDTH = 400;
	protected final int RACE_LENGTH = 28000;
	protected enum Status {OFFLINE, ACCEPTING_PLAYERS,PLAYING}; 
	
	protected Id[] initialClientsId;
	protected Status status;
	protected String rminame;
	protected Race race;	
	protected Client[] clients;
	protected int currentSlot;
	private static int loglvl = 5; //a medida que es más alto es más especifico
	protected Spawn currentSpawn;
	
	private int port, nPlayers,nIAPlayers;
	
	public ServerImpl(int n_players, int n_iaplayers,int port) throws RemoteException{
		this.nPlayers = n_players;
		this.nIAPlayers = n_iaplayers;
		this.port = port;
		this.status = Status.OFFLINE;
		this.rminame = "u.cc51s.server";
		reset();
	}
	
	private void reset(){
		log(0,"(Re)iniciando Sesion.");
		deleteSavedState();
		this.clients = new Client[nPlayers];
		this.initialClientsId = new Id[nPlayers];
		this.currentSlot = 0;
		race = new Race(nPlayers, nIAPlayers, this.RACE_WIDTH, this.RACE_LENGTH);
		this.status = Status.ACCEPTING_PLAYERS;
	}

	public int getPort() {
		return port;
	}

	public int getnPlayers() {
		return nPlayers;
	}

	public int getnIAPlayers() {
		return nIAPlayers;
	}

	@Override
	public Status getStatus() throws RemoteException {
		return status;
	}

	@Override
	public Client[] getClients() throws RemoteException {
		return clients;
	}
	
	@Override
	public Race getRace() throws RemoteException {
		return race;
	}
	
	public String getRMIName() {
		return rminame;
	}
	
	@Override
	public Spawn getCurrentSpawn() throws RemoteException {
		return currentSpawn;
	}
	
	@Override
	public void setCurrentSpawn(Spawn s)  throws RemoteException {
		currentSpawn = s;
	}
	
	@Override
	public synchronized void endSession() throws RemoteException {
		log(0,"Terminado sesion");
		if(status==Status.PLAYING || status== Status.ACCEPTING_PLAYERS){
			for(int i=0;i<clients.length;i++){
				if(clients[i]!=null)
					clients[i].endSession();
			}
		}
		reset();
	}
	
	@Override
	public synchronized void addClient(Client c) throws RemoteException{
		log(0,"El jugador "+c.getName()+" se esta uniendo a la partida.");
		
		if(currentSlot>=clients.length) {
			System.err.println("[Server] Espacio insuficiente para agregar otro usuario ("+currentSlot+"/"+clients.length+").");
			return;
		}
		
		if(status!=Status.ACCEPTING_PLAYERS) {
			System.err.println("[Server] El servidor no esta aceptado usuarios.");
			return;
		}

		//añado al cliente
		//el campo i del id corresponde al slot
		clients[currentSlot] = c;
		Id id = new Id(currentSlot);
		initialClientsId[currentSlot] = id;
		c.setId(id);
		race.setHumanPlayer(currentSlot, new HumanPlayer(c.getName()));
		currentSlot++;
		
		saveState();
		
		//notifico a EL RESTO
		for(int i=0;i<(currentSlot-1);i++){
			try {
				clients[i].addClient(c);
			} catch (RemoteException e) {
				handleDisconection(i);
				//e.printStackTrace();
			}
		}
	}
	
	@Override
	public synchronized void clientReady(Client c) throws RemoteException{
		log(0,"El jugador \""+c.getName()+"\" esta listo para comenzar");
		
		if(status!=Status.ACCEPTING_PLAYERS) {
			System.err.println("[Server] El servidor no esta aceptado notificaciones de empezar el juego");
			return;
		}
		
		//aviso a todos que este cliente esta listo
		for(int i=0;i<currentSlot;i++){
			try {
				if(clients[i]!=null)
					clients[i].clientReady(c);
			} catch (RemoteException e) {
				handleDisconection(i);
				//e.printStackTrace();
			}
		}
		
		log(4,"Los clientes fueron notificados de la insercion");
		
		//chequeo si todos estan listos para comenzar		
		if(currentSlot==clients.length){
			boolean allReady = true;
			for(int i=0;i<clients.length;i++){
				try {
					if(clients[i]==null){
						log(4,"No se puede empezar la partida ya que un usuario no esta conectado");
						allReady = false;
						break;
					}
					if(!clients[i].isReady()){
						allReady = false;
						break;
					}
				} catch (RemoteException e) {
					handleDisconection(i);
					//e.printStackTrace();
				}
			}
			if(allReady){
				startGame();
			}
		}
	}

	private void startGame() {
		log(0,"Iniciando juego");
		
		if(status != Status.ACCEPTING_PLAYERS){
			System.err.println("[Server] No se puede iniciar juego si no se estubo aceptando clientes");
			return;
		}
		
		if(currentSlot<(clients.length-1)){
			System.err.println("[Server] Falta que ingresen jugadores a la carrera antes de iniciarla");
			return;
		}
		
		race.reset();
		
		/* Inicio spawn */
		int i=0;
		try{
			for(;i<clients.length;i++){
				clients[i].startGame(race,race.getHumanPlayer(i));
			}
			if(clients[getNextSpawn()].getSpawn()==null){
				System.err.println("[Server] Spawn no ha sido iniciado!");
				return;
			}else{
				clients[getNextSpawn()].receiveSpawn(this);
			}
		}catch (RemoteException e) {
			handleDisconection(i);
			//e.printStackTrace();
		}
		
		/* Cambio status a jugando */ 
		status = Status.PLAYING;
		saveState();
	}

	@Override
	public synchronized void receiveMessage(String s) throws RemoteException {
		log(1,"Mensaje recibido: "+s);
	}

	private static void log(int lvl, String msg){
		if(lvl<=loglvl){
			System.out.println("[Server] "+msg);
		}
	}
	
	private static void error(String msg){
		System.err.println("[Server] "+msg);
	}
	
	private void handleDisconection(int i){
		log(0,"El cliente con Id "+initialClientsId[i]+" ha abandonado la partida");
		clients[i] = null;
		for(int j=0;j<currentSlot;j++){
			try {
				if(clients[j]!=null)
					clients[j].notifyDisconnection(initialClientsId[i]);
			} catch (RemoteException e) {
				error("Error al notificar clientes de desconexion");
				e.printStackTrace();
			}
		}
	}

	@Override
	public int getNextSpawn() throws RemoteException {
		return 0;
	}

	@Override
	public boolean reconnect(Client c) throws RemoteException {
		/* primero reviso si el cliente pertenece a la lista original de clientes
		 * que se han conectado al servidor.
		 */
		log(4,"Revisando id: "+c.getId());
		for(int i=0;i<currentSlot;i++){
			if(initialClientsId[i]==null){
				/* si borre un elemento de la lista significa que ya termine la sesion */
				return false;
			}
			if(initialClientsId[i].equals(c.getId())){
				clients[i] = c;
				for(int j=0;j<currentSlot;j++)
					if(clients[j]!=null)
						clients[j].notifyReConnection(c.getId());
				saveState();
				return true;
			}
		}
		return false;
	}

	@Override
	public void removeClient(int i) throws RemoteException {
		clients[i] = null;
		saveState();
	}
	
	private synchronized void saveState(){
		try {
			FileOutputStream fout = new FileOutputStream("server.tmp");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this);
			oos.close();
		}
		catch (Exception e) { 
			//e.printStackTrace(); 
		}
	}
	
	public synchronized static void deleteSavedState(){
		File state = new File("server.tmp");
		if(state.isFile())
			state.delete();
	}
	
	public synchronized static boolean hasSavedState(){
		File state = new File("server.tmp");
		return state.isFile();
	}
	
	public synchronized static ServerImpl loadState(){
		try {
			FileInputStream fin = new FileInputStream("server.tmp");
			ObjectInputStream ois = new ObjectInputStream(fin);
			ServerImpl s = (ServerImpl) ois.readObject();
			ois.close();
			return s;
		}
		catch (Exception e) { 
			e.printStackTrace(); 
		}
		return null;
	}
	
	public static void main(String args[]){
		/* Reviso si no hay un estado de servidor */
		boolean reload = false;
		if(hasSavedState()){
			System.out.println("Se ha encontrado un estado de servidor guardado en el equipo.\n¿Desea reinstanciarlo? (S/n)");
			Scanner in = new Scanner(System.in);
			String line = in.nextLine();
			if(line.toLowerCase().equals("n")||line.toLowerCase().equals("no")){
				deleteSavedState();
			} else {
				reload = true;
			}
		}
		
		/* Obtengo argumentos */
		int port = 1099;
		int n_jugadores = 2;
		int n_ai = 4;
		if(!reload){
			if(args.length!=3){
				System.err.println("Modo de ejecucion:\nServerImpl <puerto> <numero jugadores> <numero bots>");
				return;
			}
			try{
				port = Integer.parseInt(args[0]);
				n_jugadores = Integer.parseInt(args[1]);
				n_ai = Integer.parseInt(args[2]);
			} catch(NumberFormatException nfe){
				System.err.println("Los argumentos deben ser numeros enteros");
				return;
			}
		}
		
		/* Obtengo variables de entorno*/
		System.setProperty("java.security.policy", "u.cc51s.policy");
        System.setSecurityManager ( new RMISecurityManager() );
        String localhost = null;
        try{
        	String d = (InetAddress.getLocalHost()).toString();
        	localhost = d.substring(d.indexOf("/")+1);
       	} catch(Exception e){
          	System.err.println("[Server] Error al obtener la IP del Sistema");
         	e.printStackTrace();
        }
       	
       	/* Creo el servidor */
       	ServerImpl s = null;
       	if(reload){
       		s = loadState();
       		port = s.getPort();
    		n_jugadores = s.getnPlayers();
    		n_ai = s.getnIAPlayers();
       	} else {
			try {
				s = new ServerImpl(n_jugadores,n_ai,port);
			} catch (RemoteException e1) {
				System.err.println("[Server] Error al crear Servidor");
				e1.printStackTrace();
			}
       	}
       	
       	/* Inicio Registro RMI */
       	log(0,"Iniciando servidor en "+localhost+":"+port);
       	Registry registry = null;
		try{
			registry = LocateRegistry.createRegistry(port);
		}catch(Exception e){
			log(3,"Servidor RMI presente");
			try {
				registry = LocateRegistry.getRegistry(port);
			} catch (RemoteException e1) {
				System.err.println("[Server] No se pudo obtener Registro RMI");
				e1.printStackTrace();
				return;
			}
		}
		
		/* Lo agrego al registro RMI */
		try {
			registry.bind(s.getRMIName(), s);
		} catch (Exception e) {
			System.err.println("[Server] Error al agregar Servidor en el Registro RMI");
			e.printStackTrace();
			return;
		}
		
		/* Actualizo referencias */
		if(reload){
			for(int i=0;i<s.currentSlot;i++){
				if(s.clients[i]!=null)
					try {
						s.clients[i].refreshServer();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
			}
		}
	}
	
}
