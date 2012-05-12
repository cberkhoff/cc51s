package ui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.JDialog;
import core.Client;
import core.Id;

import java.awt.BorderLayout;
import java.rmi.RemoteException;
import javax.swing.JOptionPane;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class LobbyDialog extends JDialog implements ActionListener, WindowListener {
	
	private static final long serialVersionUID = -8567598828716451517L;
	private JButton iAmReadyButton;
	private JLabel MessageLabel;
	private JTable playersTable;
	private JPanel SouthPanel;
	private JPanel NorthPanel;
	private JPanel panelJugadores;
	private MainWindow mainwindow;
	protected PlayerTableModel playersTableModel;
	
	public LobbyDialog(MainWindow mainwindow,Client[] clients) {
		super(mainwindow,true);
		this.playersTableModel = new PlayerTableModel(clients);
		this.mainwindow = mainwindow;
		initGUI();
	}
	
	public void clientReady(Client c) throws RemoteException {
		playersTableModel.clientReady(c);
	}

	public void addClient(Client c) {
		playersTableModel.addClient(c);
	}
	
	public void clientDisconnected(Id id){
		playersTableModel.clientDisconected(id);
	}
	
	public void clientReConnected(Id id){
		playersTableModel.clientReConnected(id);
	}
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			getContentPane().setLayout(thisLayout);
			addWindowListener(this);
			//setUndecorated(true); 
			this.setTitle("Esperando jugadores...");
			{
				NorthPanel = new JPanel();
				getContentPane().add(NorthPanel, BorderLayout.NORTH);
				NorthPanel.setPreferredSize(new java.awt.Dimension(350, 40));
				{
					MessageLabel = new JLabel();
					NorthPanel.add(MessageLabel);
					MessageLabel.setText("Nuevo Juego");
				}
			}
			{
				SouthPanel = new JPanel();
				getContentPane().add(SouthPanel, BorderLayout.SOUTH);
				SouthPanel.setPreferredSize(new java.awt.Dimension(350, 40));
				{
					iAmReadyButton = new JButton();
					SouthPanel.add(iAmReadyButton);
					iAmReadyButton.addActionListener(this);
					iAmReadyButton.setText("Estoy Listo");
					if(mainwindow.getClient().isReady())
						iAmReady();
				}
			}
			{
				panelJugadores = new JPanel();
				panelJugadores.setPreferredSize(new java.awt.Dimension(390, 120));
				panelJugadores.setLayout(new BorderLayout());
				getContentPane().add(panelJugadores, BorderLayout.CENTER);
				{
					playersTable = new JTable(playersTableModel);
					playersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
					playersTable.getColumnModel().getColumn(0).setPreferredWidth(70);
					playersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
					playersTable.getColumnModel().getColumn(2).setPreferredWidth(150);
					playersTable.getColumnModel().getColumn(3).setPreferredWidth(70);
					panelJugadores.add(playersTable);
					panelJugadores.add(playersTable.getTableHeader(), BorderLayout.PAGE_START);
					panelJugadores.add(playersTable, BorderLayout.CENTER);
					playersTable.setPreferredSize(new java.awt.Dimension(350, 120));
				}
			}
			//setResizable(false);
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			if(!mainwindow.getClient().iAmReady()){
				//se perdio la conexion con el servidor
				JOptionPane.showMessageDialog(this, "Se perdio la conexion con el servidor. Intente conectarse nuevamente.", "Error", JOptionPane.ERROR_MESSAGE);
			} else {
				iAmReady();
			}
		} catch (RemoteException e) {
			System.err.println("Error en la red al notificar que el cliente estaba listo");
			e.printStackTrace();
		}
	}
	private void iAmReady(){
		iAmReadyButton.setEnabled(false);
		iAmReadyButton.setText("Esperando a los otros jugadores...");
	}

	@Override
	public void windowActivated(WindowEvent arg0) {}

	@Override
	public void windowClosed(WindowEvent arg0) {}

	@Override
	public void windowClosing(WindowEvent arg0) {
		try {
			mainwindow.getClient().logout();
		} catch (RemoteException e) {
			System.err.println("Error al cerrar la sesion");
			e.printStackTrace();
		}
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {}

	@Override
	public void windowDeiconified(WindowEvent arg0) {}

	@Override
	public void windowIconified(WindowEvent arg0) {}

	@Override
	public void windowOpened(WindowEvent arg0) {}
	
	private class PlayerTableModel extends AbstractTableModel{
		
		private static final long serialVersionUID = 6233516029375716401L;
		int currentSlot;
		int n_players;
		Id[] ids;
		String [] names;
		boolean[] status;
		boolean[] connected;
		
		public PlayerTableModel(Client[] clients){
			int i;
			ids = new Id[clients.length];
			names = new String[clients.length];
			status = new boolean[clients.length];
			connected = new boolean[clients.length];
			n_players = clients.length;
			currentSlot = 0;
			for(i=0;i<clients.length;i++){
				if(clients[i]==null)
					break;
				addClient(clients[i]);
			}
		}
		
		public void addClient(Client c) {
			if(currentSlot==n_players){
				System.err.println("No quedan espacios disponibles para agregar a la tabla");
				return;
			}
			try {
				ids[currentSlot] = c.getId();
				names[currentSlot] = c.getName();
				status[currentSlot] = c.isReady();
				connected[currentSlot] = true;
				fireTableRowsUpdated(currentSlot, currentSlot);
				currentSlot++;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		public void clientReady(Client c) throws RemoteException{
			//busco al cliente basado en su id
			for(int i=0;i<currentSlot;i++){
				if(ids[i].equals(c.getId())){
					status[i] = true;
					fireTableCellUpdated(i, 2);
				}
			}
			
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public int getRowCount() {
			return n_players;
		}
		
		@Override
		public String getColumnName(int col) {
	        switch(col){
	        	case 0: return "ID";
	        	case 1: return "Nombre";
	        	case 2: return "Estado";
	        	case 3: return "";
	        }
	        return null;
	    }
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch(columnIndex){
    		case 0: return Integer.class;
    		case 1: return String.class;
    		case 2: return String.class;
    		case 3: return String.class;
		}
		return null;
		}
		
		@Override
		public boolean isCellEditable(int row, int col){ 
			return false; 
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch(columnIndex){
        		case 0: return ids[rowIndex];
        		case 1: return names[rowIndex];
        		case 2: return (status[rowIndex])?("Listo para jugar"):("Esperando jugador");
        		case 3: return (connected[rowIndex])?("Conectado"):("Desconectado");
			}
			return null;
		}
		
		public void clientDisconected(Id id){
			//busco al cliente basado en su id
			for(int i=0;i<currentSlot;i++){
				if(ids[i].equals(id)){
					connected[i] = false;
					fireTableCellUpdated(i, 3);
				}
			}
		}
		
		public void clientReConnected(Id id){
			//busco al cliente basado en su id
			for(int i=0;i<currentSlot;i++){
				if(ids[i].equals(id)){
					connected[i] = true;
					fireTableCellUpdated(i, 3);
				}
			}
		}
		
	}

}
