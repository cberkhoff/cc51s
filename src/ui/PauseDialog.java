package ui;
import game.HumanPlayer;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;


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
public class PauseDialog extends javax.swing.JDialog {

	private static final long serialVersionUID = -4233912793074832202L;
	private JLabel message;
	
	public PauseDialog(JFrame frame) {
		super(frame);
		initGUI();
	}
	
	private void initGUI() {
		try {
			{
				this.setTitle("Pausa");
				this.setAlwaysOnTop(true);
				this.setUndecorated(true); 
			}
			{
				message = new JLabel();
				getContentPane().add(message, BorderLayout.CENTER);
				message.setText("El jugador X ha pausado la partida");
				message.setPreferredSize(new java.awt.Dimension(274, 51));
				message.setHorizontalTextPosition(JLabel.CENTER);
				message.setVerticalTextPosition(JLabel.CENTER);
			}
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setPlayer(HumanPlayer h){
		message.setText("El jugador "+h.getName()+" ha pausado la partida");
	}

}
