package core;

import game.Race;

import java.rmi.Remote;
import java.rmi.RemoteException;
import core.ServerImpl.Status;

/**
 * Encargada de crear el primer Spawn. Ademas tiene el puntero al Spawn actual.
 * @author chris
 *
 */
public interface Server extends Remote {
	public void addClient(Client client) throws RemoteException;
	public void clientReady(Client client) throws RemoteException;
	public Client[] getClients() throws RemoteException;
	public void endSession() throws RemoteException;
	public void receiveMessage(String s) throws RemoteException;
	public Status getStatus() throws RemoteException;
	public Race getRace() throws RemoteException;
	public int getNextSpawn() throws RemoteException;
	public Spawn getCurrentSpawn() throws RemoteException;
	public void setCurrentSpawn(Spawn s) throws RemoteException;
	public boolean reconnect(Client c) throws RemoteException;
	public void removeClient(int i) throws RemoteException;
}
