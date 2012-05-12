package ui;

import game.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.rmi.RemoteException;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.SwingUtilities;
import core.Client;
import core.ClientImpl;
import core.SpawnImpl;

public class MainWindow extends JFrame implements ActionListener, KeyListener,WindowListener{
	protected ClientImpl client;
	private RaceCanvas canvas;
	private LobbyDialog ld;
	private PauseDialog pd;
	private static final long serialVersionUID = -2122517532263683896L;
	private JMenuBar menubar;
	private JMenu MainMenu;
	
	private JLabel RaceStatus;
	private JLabel ConnectionStatus;
	private JLabel HostStatus;
	private ImageIcon iconDisconnected;
	private ImageIcon iconConnected;
	private ImageIcon iconHost;
	private ImageIcon iconClient;
	
	private JPanel StatusPanel;	
	private JMenuItem JoinServerMenuItem;
	private JMenuItem ExitMenuItem;
	private JSeparator Separator;
	private String lastHost,lastName;
	private int lastPort;
	
	public static void main(String[] args) {
		try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (Exception e) {
	       e.printStackTrace();
	    }

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MainWindow inst = new MainWindow();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
	public MainWindow() {
		super();
		initGUI();
	}
	
	private void initGUI() {
		try {
			iconClient = new ImageIcon("resources/client.png");
			iconHost = new ImageIcon("resources/host.png");
			iconConnected = new ImageIcon("resources/connected.png");
			iconDisconnected = new ImageIcon("resources/disconnected.png");
			canvas = new RaceCanvas(400, 600);
			BorderLayout thisLayout = new BorderLayout();
			getContentPane().setLayout(thisLayout);
			addWindowListener(this);
			this.setTitle("CC51S");
			{
				getContentPane().add(canvas, BorderLayout.CENTER);
			}
			{
				StatusPanel = new JPanel();
				FlowLayout StatusPanelLayout = new FlowLayout();
				StatusPanelLayout.setAlignment(FlowLayout.LEFT);
				StatusPanel.setLayout(StatusPanelLayout);
				getContentPane().add(StatusPanel, BorderLayout.SOUTH);
				StatusPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
				StatusPanel.setFocusable(false);
				{
					ConnectionStatus = new JLabel(iconDisconnected);
					HostStatus = new JLabel();
					StatusPanel.add(ConnectionStatus);
					StatusPanel.add(HostStatus);
					RaceStatus = new JLabel();
					StatusPanel.add(RaceStatus);
				}
			}
			{
				menubar = new JMenuBar();
				setJMenuBar(menubar);
				{
					MainMenu = new JMenu();
					menubar.add(MainMenu);
					MainMenu.setText("Archivo");
					{
						JoinServerMenuItem = new JMenuItem();
						MainMenu.add(JoinServerMenuItem);
						JoinServerMenuItem.setText("Unirse a un Servidor...");
						JoinServerMenuItem.addActionListener(this);
					}
					{
						Separator = new JSeparator();
						MainMenu.add(Separator);
					}
					{
						ExitMenuItem = new JMenuItem();
						MainMenu.add(ExitMenuItem);
						ExitMenuItem.setText("Salir");
						ExitMenuItem.addActionListener(this);
					}
				}
			}
			setResizable(false);
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ClientImpl getClient() {
		return client;
	}
	
	public void showPauseDialog(HumanPlayer h){
		if(pd==null)
			pd = new PauseDialog(this);
		pd.setPlayer(h);
		pd.setLocationRelativeTo(this);
		pd.setVisible(true);
	}
	
	public void hidePauseDialog(){
		if(pd!=null)
			pd.setVisible(false);
	}
	
	public LobbyDialog getLobbyDialog() {
		return ld;
	}
	
	/*
	 * Este metodo espera un arreglo que "pueda contener" a todos los nombres
	 * de los jugadores y que cuyos primeros elementos sean no nulos.
	 */
	public void createLobbyDialog(Client[] clients){
		if(ld!=null){
			ld.setVisible(false);
			ld.dispose();
			ld = null;
		}
		ld = new LobbyDialog(this, clients);
		ld.setLocationRelativeTo(this);
		ld.setVisible(true);
	}
	
	/*
	 * este metodo se llama cuando el cliente es informado de que debe empezar el juego
	 */
	public void startRace(Race r,Player p){
		if(ld!=null){
			ld.setVisible(false);
			ld.dispose();
			ld = null;
			canvas.updateRacePlayer(r, p);
			addKeyListener(this);
		}else{
			System.err.println("Imposible cerrar Dialogo de Lobby. No ha sido iniciado.");
		}
	}
	
	private void terminate(){
		try {
			if(client!=null)
				client.logout();
			System.exit(0);
		} catch (RemoteException e) {
			System.err.println("Error al detener el cliente");
			e.printStackTrace();
		}
	}
	
	public void updateRaceCanvas(Race r,HumanPlayer p){
		//System.out.println("Actualizar UI");
		canvas.updateRacePlayer(r, p);
		canvas.repaint();
	}

	public void updateHUD(int fuel,int remaining, int vel){
		RaceStatus.setText("Combustible: "+fuel+" - Dist. Rest.: "+remaining+" - Vel: "+vel+"[km/p]");
	}

	public void showGameOverDialog(String name){
		removeKeyListener(this);
		Object[] options = {"Si, por supuesto","No gracias"};
		int o = JOptionPane.showOptionDialog(
				this,
				"El jugador "+name+" ha ganado la carrera!\n¿Desea Jugar una nueva?", 
				"Ha terminado la carrera",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				options,
				options[0]);
		if(o==JOptionPane.OK_OPTION){
			try {
				client.login(lastHost, lastName);
				createLobbyDialog(client.getServer().getClients());
				hudSetConnected();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			client = null;
		}
	}


	@Override
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource()==ExitMenuItem){
			terminate();
		} else if(ae.getSource()==JoinServerMenuItem){		
			/* primero reviso el caso de que haya un estado guardado */
			if(ClientImpl.hasSavedState()){
				Object[] options = {"Si ¡Conéctame!","No, borrar el estado"};
				int o = JOptionPane.showOptionDialog(
						this,
						"Perdió inesperadamente la conexión del juego anterior\n"+
						"¿Desea cargar este estado guardado y volver a conectarse al juego?", 
						"Hay un estado guardado",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.INFORMATION_MESSAGE,
						null,
						options,
						options[0]);
				if(o==JOptionPane.OK_OPTION){
					/* El usuario quiere recuperar su estado */
					client = ClientImpl.loadState();
					try {
						client.restoreSession(this);
					} catch (RemoteException e) {
						JOptionPane.showMessageDialog(this, "Error al intentar reconectar\nIntente conectarse nuevamente.", "Error", JOptionPane.ERROR_MESSAGE, null);
						e.printStackTrace();
						client = null;
						return;
					}
					
					if(!ClientImpl.reconectionSuccessfull){
						JOptionPane.showMessageDialog(this, "La partida ha terminado.\nHa perdido.", "Error", JOptionPane.ERROR_MESSAGE, null);
					}
					
					/* Cliente reinstanciado, ahora debo ver en que estado quedo 
					 * y tomar las acciones necesarias*/
					try{
						if(client.getStatus()==ClientImpl.Status.DISCONNECTED){
							/* El cliente esta practicamente vacio. Se continua con el caso
							 * normal. Primero borro lo que guarde */
							ClientImpl.deleteSavedState();
						} else if(client.getStatus()==ClientImpl.Status.AWAITING_PLAYERS){
							lastPort = client.getPort();
							lastHost = client.getHost();
							lastName = client.getName();
							createLobbyDialog(client.getServer().getClients());
							hudSetConnected();
							return;
						} else {
							lastPort = client.getPort();
							lastHost = client.getHost();
							lastName = client.getName();
							addKeyListener(this);
							if(client.getSpawnImpl().getStatus()==SpawnImpl.Status.ACTIVE)
								hudSetHost();
							else
								hudSetConnected();
							if(client.getServer().getCurrentSpawn().isPaused())
								this.showPauseDialog(client.getServer().getCurrentSpawn().pauseAuthor());
							return;
						}
					} catch(RemoteException e){
						JOptionPane.showMessageDialog(this, "Error al obtener variables\nIntente conectarse nuevamente.", "Error", JOptionPane.ERROR_MESSAGE, null);
						e.printStackTrace();
						client = null;
						return;
					}
				}
				ClientImpl.deleteSavedState();
			}
			
			/* crear nueva conexion */
			JTextField TextFieldName = new JTextField("Chris");
			JTextField TextFieldIP = new JTextField("localhost");
			JTextField TextFieldPort = new JTextField("1099");
			Object[] options = {"Conectar","Cancelar"};
			final JComponent[] inputs = new JComponent[] {
					new JLabel("Nombre de Usuario"),
					TextFieldName,
					new JLabel("Dirección del Servidor"),
					TextFieldIP,
					new JLabel("Puerto"),
					TextFieldPort};
			int o = JOptionPane.showOptionDialog(
					this, 
					inputs, 
					"Conectarse a un Servidor...",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]);
			if(o==JOptionPane.OK_OPTION){
				//conectarse al servidor
				try {
					lastPort = Integer.parseInt(TextFieldPort.getText());
					if(lastPort<0||lastPort>65535)
						throw new NumberFormatException();
					lastHost = TextFieldIP.getText();
					lastName = TextFieldName.getText();

					client = new ClientImpl(this,lastPort);
					client.login(lastHost, lastName);
					createLobbyDialog(client.getServer().getClients());
					hudSetConnected();
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (NumberFormatException nfe){
					JOptionPane.showMessageDialog(
							this, 
							"El puerto especificado no es un número entero válido", 
							"Error", 
							JOptionPane.ERROR_MESSAGE,
							null);
					nfe.printStackTrace();
				}
			}
		}
	}
	
	public void errorMessage(String m){
		JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		try {
			client.setPressedKey(e.getKeyCode());
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}
	
	public void hudSetConnected(){
		ConnectionStatus.setIcon(iconConnected);
		HostStatus.setIcon(iconClient);
	}
	
	public void hudSetDisconnected(){
		ConnectionStatus.setIcon(iconDisconnected);
		HostStatus.setIcon(null);
	}
	
	public void hudSetHost(){
		ConnectionStatus.setIcon(iconConnected);
		HostStatus.setIcon(iconHost);
	}
	
	public void hudSetClient(){
		ConnectionStatus.setIcon(iconConnected);
		HostStatus.setIcon(iconClient);
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void windowActivated(WindowEvent arg0) {}

	@Override
	public void windowClosed(WindowEvent arg0) {}

	@Override
	public void windowClosing(WindowEvent arg0) {
		terminate();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {}

	@Override
	public void windowDeiconified(WindowEvent arg0) {}

	@Override
	public void windowIconified(WindowEvent arg0) {}

	@Override
	public void windowOpened(WindowEvent arg0) {}

}
