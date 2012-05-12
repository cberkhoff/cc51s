package core;

import game.HumanPlayer;
import game.Race;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Encargado de Controlar el ciclo del juego.
 * El constructor lo deja en estado Inactivo
 * Al invocar el metodo updateAndContinue se activa hasta que termina su vida y luego se desactiva
 * @author chris
 *
 */
public interface Spawn extends Remote {
	public boolean reconnect(Client c) throws RemoteException;
	public void updateAndContinue(Spawn s) throws RemoteException;
	public void updateAndContinue(Server s) throws RemoteException;
	public String getRMIName() throws RemoteException;
	public int getNextSpawn() throws RemoteException;
	public Race getRace() throws RemoteException;
	public Client[] getClients() throws RemoteException;
	public boolean isPaused()  throws RemoteException;
	public HumanPlayer pauseAuthor() throws RemoteException;
}
