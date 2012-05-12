package core;

import game.HumanPlayer;
import game.Race;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.SwingUtilities;
import ui.MainWindow;

public class ClientImpl extends UnicastRemoteObject implements Client {
	private static final long serialVersionUID = -8433103360591182653L;
	
	public enum Status {DISCONNECTED, AWAITING_PLAYERS, PLAYING};
	protected Status status;	
	protected Id id;
	protected boolean ready;
	protected String name;
	protected int port;
	protected String host;
	protected transient String localhost;
	protected transient Registry registry;
	protected int pressedKey;
	protected transient Server server;
	protected SpawnImpl spawn;
	protected transient MainWindow mainwindow;
	private int loglvl = 5; //a medida que es más alto es más especifico
	public static boolean reconectionSuccessfull = true;
	
	public ClientImpl(MainWindow mainwindow, int port) throws RemoteException{
		this.port = port;
		init(mainwindow);		
	}
	
	private void init(MainWindow mainwindow) throws RemoteException{
		/* Defino variables iniciales */
		this.mainwindow = mainwindow;
		this.status = Status.DISCONNECTED;
				
		/* obtengo localhost */
		try{
			String d = (InetAddress.getLocalHost()).toString();
        	localhost = d.substring(d.indexOf("/")+1);
        	log(0,"Iniciando Cliente en "+localhost+":"+port);
       	} catch(Exception e){
       		error("[Client] Error al obtener la IP del Sistema");
         	e.printStackTrace();
        }
       	
       	/* creo registro RMI */
       	log(4,"Creando Registro RMI");
		try{
			registry = LocateRegistry.createRegistry(port);
		}catch(RemoteException e){
			log(3,"Servidor RMI presente, utilizando esta instancia");
			registry = LocateRegistry.getRegistry(port);
		}
		log(2,"Cliente instanciado correctamente");
	}
	
	public Status getStatus() {
		return status;
	}
	
	@Override
	public Id getId() {
		return id;
	}

	@Override
	public Spawn getSpawn() {
		return (Spawn)spawn;
	}
	
	public SpawnImpl getSpawnImpl(){
		return spawn;
	}

	public Server getServer() {
		return server;
	}
	
	public void setPressedKey(int pressedKey) throws RemoteException{
		this.pressedKey = pressedKey;
	}
	
	public int getPressedKey() throws RemoteException{
		int r = pressedKey;
		pressedKey = -1;
		return r;
	}
	
	public int getPort() throws RemoteException {
		return port;
	}
	
	public String getHost(){
		return host;
	}
	
	@Override
	public boolean isReady() {
		return ready;
	}

	@Override
	public void setId(Id id) throws RemoteException {
		this.id = id;
	}
	
	@Override
	public String getName()throws RemoteException {
		return name;
	}

	public String getLocalhost() throws RemoteException {
		return localhost;
	}

	public void restoreSession(MainWindow mainwindow) throws RemoteException{
		log(2,"Restaurando sesion");
		Status previousStatus = status;
		init(mainwindow);
		if(previousStatus==Status.DISCONNECTED){
			/* en este caso no habia nada hecho (solo estaban definidas variables
			 * de instancia). Es necesario hacer el login de forma manual. 
			 */
		} else if(previousStatus == Status.AWAITING_PLAYERS){
			/* Antes de "salirme" estaba en el lobby. Es necesario reconectarse
			 * al servidor de login (Server) es decir hacer login automatico.
			 */
			restoreLogin();
			reconectionSuccessfull = this.status==Status.AWAITING_PLAYERS;
		} else {
			/* Antes de "salirme" estaba jugando. Es necesario primero reconectarse 
			 * con el servidor de login y luego reconectarse con el servidor de 
			 * juego (Spawn). Ambos automaticos.
			 */
			restoreLogin();
			restoreGame();
			reconectionSuccessfull = this.status==Status.PLAYING;
		}
	}

	public void login(String host,String name) throws RemoteException{
		log(4,"Iniciando login manual");
		this.host = host;
		this.name = name;
		login(false);
	}
	
	public void restoreLogin() throws RemoteException{
		log(4, "Iniciando login automatico");
		login(true);
	}
	
	private synchronized void login(boolean reconnect) throws RemoteException{
		if(status!=Status.DISCONNECTED){
			error("No se puede conectar un cliente que ya esta conectado.");
			return;
		}
		
		if(this.host==null){
			error("No se puede conectar si no se ha definido host.");
			return;
		}
		
		if(this.name ==null){
			error("No se puede conectar si no se ha definido nombre de usuario.");
			return;
		}
		
		/* reviso permisos de coneccion*/
		System.setProperty("java.security.policy", "u.cc51s.policy");
		if (System.getSecurityManager() == null)
            System.setSecurityManager ( new RMISecurityManager() );
		try{
			System.getSecurityManager().checkConnect(this.host, this.port);
		}catch (SecurityException e) {
			error("No se tienen permisos para conectarse");
			return;
		}
		log(2,"Permisos de coneccion concedidos");	

		log(0,"Conectando a "+this.host+":"+this.port);
		
		/* me conecto al servidor */
		refreshServer();
		
		/* me intento conectar o reconectar */
		if(reconnect){
			if(!server.reconnect(this)){
				error("No es posible reconectarse");
				return;
			}
			restoreSpawn();
		} else {
			server.addClient(this);
			log(1,"Añadido a la lista del servidor");
	        if(id==null){
	        	System.err.println("[Client] No fue posible unirse a la partida");
	        	return;
	        }
	        createSpawn();
		}
	     
		/* continuo con las ultimas acciones, comunes tanto para el login manual
		 * y automatico.
		 */
		this.pressedKey = -1;
		this.status = Status.AWAITING_PLAYERS;
		saveState();
	}
	
	@Override
	public void refreshServer() throws RemoteException {
		Registry remote_registry = LocateRegistry.getRegistry(host, port);	
		log(4,"Registro RMI encontrado. Buscando \"u.cc51s.server\"");
        try {
			server = (Server) remote_registry.lookup("u.cc51s.server");
			log(4,"Conectado a Servidor");
		} catch (NotBoundException e) {
			error("No se encontro un servidor en el Registro RMI (puede haberse desconectado)");
			e.printStackTrace();
			return;
		}
	}

	
	private void restoreSpawn() throws AccessException, RemoteException{
		log(4,"Restaurando Spawn");
		log(6,"id="+this.id);
		spawn.restore(this);
		registry.rebind(spawn.getRMIName(), spawn);
	}
	
	private void createSpawn() throws RemoteException{
		log(4,"Creando Spawn");
		spawn = new SpawnImpl(this);
		registry.rebind(spawn.getRMIName(), spawn);
	}
	
	private void restoreGame() throws RemoteException{
		/* Intenta volver a conectarse con el spawn. El servidor ya debiese ser
		 * una variable de instancia valida. 
		 */
		if(status!=Status.AWAITING_PLAYERS){
			error("No se puede re-conectar al juego si es que no esta conectado al Servidor.");
			return;
		}
		server.getCurrentSpawn().reconnect(this);
		this.status = Status.PLAYING;
		/* tengo que decirle al spawn que continue (solo si es que el era quien dirigia el juego)*/
		spawn.restart();
		saveState();
	}
	
	public void notifySpawnMigration(){
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.hudSetClient();
    	}});
	}
	
	public void logout() throws RemoteException {
		if(server!=null) //puede que ya me hayan desconectado
			server.endSession();
	}
	
	public synchronized void endSession() throws RemoteException{
		if(status==Status.DISCONNECTED){
			error("No se puede desconectar un cliente desconectado.");
		}
		log(1,"Desconectando");
		this.pressedKey = -1;
		this.ready = false;
		this.status = Status.DISCONNECTED;
		server = null;
        id = null;
        SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.hudSetDisconnected();
    	}});
        deleteSavedState();
        printRMIObjects();
	}
	
	@Override
	public void addClient(final Client c) throws RemoteException {
		if(status!=Status.AWAITING_PLAYERS){
			error("El usuario no esta conectado");
			return;
		}
		saveState();
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.getLobbyDialog().addClient(c);
    	}});
		
	}

	@Override
	public void startGame(final Race r,final HumanPlayer p) throws RemoteException {
		if(status!=Status.AWAITING_PLAYERS){
			error("El usuario no esta conectado");
			return;
		}
		this.status = Status.PLAYING;
		saveState();
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.startRace(r, p);
    	}});		
	}

	@Override
	public synchronized void updateGame(final Race r,final HumanPlayer me, final HumanPlayer pauseAuthor) throws RemoteException {
		if(status!=Status.PLAYING){
			error("El usuario no esta jugando");
			return;
		}
		saveState();
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.updateHUD(
    							me.getCar().getRemainingFuel(),
    							(int)(r.getLength()-me.getCar().getBox().getY()),
    							me.getCar().getHUDVelocity());
    					mainwindow.updateRaceCanvas(r, me);
    					
    					if(r.isThereAWinner()){
    						mainwindow.showGameOverDialog(r.getWinner().getName());
    					} else if(pauseAuthor!=null){
    						log(5,"PAUSA");
    						mainwindow.showPauseDialog(pauseAuthor);
    					} else {
    						mainwindow.hidePauseDialog();
    					}
    				}
    			}
    	);
		
	}

	@Override
	public void clientReady(final Client c) throws RemoteException {
		//saveState();
		log(5,"Me llega notificacion de que el cliente "+c.getName()+" esta listo para comenzar");
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					try {
							mainwindow.getLobbyDialog().clientReady(c);
						} catch (RemoteException e) {
							error("Error al notificar al servidor que el usuario esta listo");
							e.printStackTrace();
						}
    	}});
	}
	

	@Override
	public void notifyDisconnection(final Id id) throws RemoteException {
		//saveState();
		log(5,"Cliente con id "+id+" se ha desconectado");
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
						if(mainwindow.getLobbyDialog()!=null)
							mainwindow.getLobbyDialog().clientDisconnected(id);
    	}});
	}

	@Override
	public void notifyReConnection(final Id id) throws RemoteException {
		//saveState();
		log(5,"Cliente con id "+id+" se ha re-conectado");
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					if(mainwindow.getLobbyDialog()!=null)
    						mainwindow.getLobbyDialog().clientReConnected(id);
    	}});
	}

	public boolean iAmReady() throws RemoteException {
		boolean r = false;
		if(server!=null){
			ready = true;
			server.clientReady(this);
			r = true;
		}
		saveState();
		return r;
	}

	@Override
	public void receiveSpawn(Spawn s) throws RemoteException {
		log(4,"Recibiendo Spawn de Spawn");
		spawn.updateAndContinue(s);
		saveState();
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.hudSetHost();
    	}});
	}
	
	@Override
	public void receiveSpawn(Server s) throws RemoteException {
		log(4,"Recibiendo Spawn de Server");
		spawn.updateAndContinue(s);
		saveState();
		SwingUtilities.invokeLater(
    			new Runnable() {
    				public void run() {
    					mainwindow.hudSetHost();
    	}});
	}
	
	private void log(int lvl, String msg){
		if(lvl<=loglvl){
			System.out.println("[Client] "+msg);
		}
	}
	
	private void error(String msg){
		System.err.println("[Client] "+msg);
	}
	
	private synchronized void saveState(){
		try {
			FileOutputStream fout = new FileOutputStream("client.tmp");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this);
			oos.close();
		}
		catch (Exception e) { 
			//e.printStackTrace(); 
		}
	}
	
	public synchronized static void deleteSavedState(){
		File state = new File("client.tmp");
		if(state.isFile())
			state.delete();
	}
	
	public synchronized static boolean hasSavedState(){
		File state = new File("client.tmp");
		return state.isFile();
	}
	
	public synchronized static ClientImpl loadState(){
		try {
			FileInputStream fin = new FileInputStream("client.tmp");
			ObjectInputStream ois = new ObjectInputStream(fin);
			ClientImpl c = (ClientImpl) ois.readObject();
			ois.close();
			return c;
		}
		catch (Exception e) { 
			e.printStackTrace(); 
		}
		return null;
	}
	
	private void printRMIObjects() throws AccessException, RemoteException{
		log(6,"Objetos dentro del Registro RMI");
		String[] l = registry.list();
		for(int i =0;i<l.length;i++)
			log(6,"\tobjeto="+l[i]);
	}

}
