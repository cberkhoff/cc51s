package core;

import game.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Esta clase se encargara de comunicarse con el servidor y de actualizar la ui
 * en la medida que el servidor lo requiera. Todos estos metodos son llamados o
 * por el Servidor o por el Spawn
 */
public interface Client extends Remote {
	/**
	 * Desconecta al usuario.
	 * @throws RemoteException
	 */
	public void endSession() throws RemoteException;
	/**
	 * Retorna el Id asignado por el servidor
	 * @return El Id
	 * @throws RemoteException
	 */
	public Id getId() throws RemoteException;
	/**
	 * Establece el Id
	 * @param id Id nuevo
	 * @throws RemoteException
	 */
	public void setId(Id id) throws RemoteException;
	/**
	 * Dice si este cliente esta listo para jugar
	 * @return Si esta listo
	 */
	public boolean isReady() throws RemoteException;
	/**
	 * Notifica a este cliente que otro cliente ha dicho estar listo para comenzar
	 * @param client El cliente que dice estar listo 
	 * @throws RemoteException
	 */
	public void clientReady(Client client) throws RemoteException;
	/**
	 * Notifica a este cliente que otro cliente se ha unido a la sesion.
	 * @param client Cliente que se ha unido
	 * @throws RemoteException
	 */
	public void addClient(Client client) throws RemoteException;
	/**
	 * Notifica a este cliente que la partida ha comenzado
	 * @param r La carrera inicializada por el servidor
	 * @param p El jugador de este cliente inicializado por el servidor
	 * @throws RemoteException
	 */
	public void startGame(Race r,HumanPlayer p) throws RemoteException;
	/**
	 * Notifica a este cliente que la partida debe ser actualizada
	 * @param r La carrera actualizada por el servidor
	 * @param p El jugador de este cliente actualizado por el servidor
	 * @throws RemoteException
	 */
	public void updateGame(Race r,HumanPlayer me, HumanPlayer pauseAuthor) throws RemoteException;
	public int getPressedKey() throws RemoteException;
	public String getName()throws RemoteException;;
	public Spawn getSpawn() throws RemoteException;
	public void receiveSpawn(Spawn s) throws RemoteException;
	public void receiveSpawn(Server s) throws RemoteException;
	public String getLocalhost() throws RemoteException;
	public int getPort() throws RemoteException;
	public void notifyDisconnection(Id id) throws RemoteException;
	public void notifyReConnection(Id id) throws RemoteException;
	public void refreshServer() throws RemoteException;
}
