package org.mitre.pophealth.client.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.mitre.pophealth.client.PatientImporter;

public class ImporterFrameLogic extends ImporterFrame {
	private static final long serialVersionUID = 6959104115108299731L;
	
	private TranslucentProgressJPanel glass = new TranslucentProgressJPanel();

	public ImporterFrameLogic() {
		super();

		glass.addMouseListener(new MouseAdapter() { });
		glass.addMouseMotionListener(new MouseMotionAdapter() { });
		glass.addKeyListener(new KeyAdapter() { });

		glass.setFocusCycleRoot(true);
		glass.requestFocus();
		setGlassPane(glass);
		glass.setOpaque(false);

		initUILogic();

	}

	private void initUILogic() {
		getCloseButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		getImportButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (validateFields()) {
					glass.setVisible(true);
					String hostname = getHostnameField().getText();
					int port = Integer.valueOf(getPortField().getText());
					String username = getUsernameField().getText();
					String password = new String(getPasswordField().getPassword());
					String directory = getFileField().getText();

					try {
						Timer timer = new Timer(500, new ImportPatientsListener(glass, hostname, port, username, password, directory));
						timer.setInitialDelay(0);
						timer.start();
					} catch (Exception err) {
						glass.setVisible(false);
					}
				}
			}
		});
		
		getChooseFileButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showDialog(ImporterFrameLogic.this, "Select Directory");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            getFileField().setText(file.getAbsolutePath());
		        }
			}
		});
	}
	
	private boolean validateFields() {
		boolean successful = true;
		successful = successful && validateField(getHostnameField(), "hostname");
		successful = successful && validateField(getPortField(), "port");
		successful = successful && validateField(getUsernameField(), "username");
		successful = successful && validateField(getPasswordField(), "password");
		successful = successful && validateField(getFileField(), "patient directory");
		
		File file = new File(getFileField().getText());
		
		if (!file.exists()) {
			successful = false; 
			JOptionPane.showMessageDialog(this, "The directory you selected does not exist", "Validation Error", JOptionPane.ERROR_MESSAGE);
		}
		if (successful && !file.isDirectory()) {
			successful = false; 
			JOptionPane.showMessageDialog(this, "You must select a patient directory", "Validation Error", JOptionPane.ERROR_MESSAGE);
		}

		return successful;
	}
	private boolean validateField(JTextField field, String name) {
		if (field.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "You must enter a value for " + name, "Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private static class ImportPatientsListener implements ActionListener {

		private TranslucentProgressJPanel glass;
		private PatientImporter importer;
		private boolean loginErrorMessage = false;
		
		public ImportPatientsListener(TranslucentProgressJPanel glass, String host, int port, String username, String password, String directory) {
			this.glass = glass;
			this.importer = new PatientImporter(host, port, username, password, new File(directory));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (!importer.isStarted()) {
				glass.setTotal(importer.getTotalPatients());
				importer.startPatientImport();
			}
			
			glass.update(importer.getPatientsLoaded(),importer.getErrorCount(), importer.getCurrentFile());
			
			if (importer.getLoginError() && !loginErrorMessage) {
				JOptionPane.showMessageDialog(glass, "Login error.  Either the credential are incorrect, or the user does not have admin permissions", "Errors loading patients.. Check the log files for details.", JOptionPane.ERROR_MESSAGE);
				loginErrorMessage = true;
			}
			
			if (importer.isComplete()) {
				glass.setVisible(false);
				((Timer)e.getSource()).stop();
				if (importer.isSuccessful()) {
					JOptionPane.showMessageDialog(glass, importer.getPatientsLoaded() + " patients successfully loaded.", "Patients Loaded", JOptionPane.INFORMATION_MESSAGE);
				} else { 
					JOptionPane.showMessageDialog(glass, importer.getErrorCount() + " of " + importer.getTotalPatients() + " patients caused errors.", "Errors loading patients.. Check the log files for details.", JOptionPane.WARNING_MESSAGE);
				}
			}
		}

	}
	
	@SuppressWarnings("serial")
	private static class TranslucentProgressJPanel extends ProgressJPanel {
		
		private int total;
		
		public TranslucentProgressJPanel() {
			super();
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();

			g2.setPaint(new Color(255, 255, 255, 190));
			g2.fillRect(0, 0, getWidth(), getHeight());

			g2.setPaint(new Color(185, 185, 185, 220));
			int x = 110;
			int y = 80;
			g2.fillRect(x, y, 410, 170);

			g2.dispose();
		}
		
		public void setTotal(int total) {
			this.total = total;
			getPatientLoadProgress().setMaximum(total);
		}
		public void update(int current, int errorCount, String currentFileName) {
			getLoadingPatintsValueLabel().setText(current + " of " + total);
			getPatientLoadProgress().setValue(current);
			getFileNameLabel().setText(currentFileName);
			getErrorCountLabel().setText(String.valueOf(errorCount));
			repaint();
		}
		
		
	}

}