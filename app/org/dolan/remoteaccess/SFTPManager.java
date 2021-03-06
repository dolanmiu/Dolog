package org.dolan.remoteaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import model.play.helpers.ServerFile;

import org.apache.commons.io.FilenameUtils;
import org.dolan.tools.LogTool;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * The Class SFTPManager.
 * An implementation of ISFTPManager. 
 */
public class SFTPManager implements ISFTPManager {
	
	/** The JSch library object. */
	private JSch jsch;
	
	/** The user name. */
	private String username;
	
	/** The host. */
	private String host;
	
	/** The port. */
	private int port;
	
	/** The password. */
	private String password;
	
	/** The session. */
	private Session session;
	
	/** The path. */
	private String path;

	/**
	 * Instantiates a new SFTP manager.
	 *
	 * @param username the user name
	 * @param host the host
	 * @param port the port
	 * @param password the password
	 * @param path the path
	 */
	public SFTPManager(String username, String host, int port, String password, String path) {
		LogTool.traceC(this.getClass(), "Creating SFTPManager with user: " + username + " and pass: " + password);
		Objects.requireNonNull(username);
		Objects.requireNonNull(host);
		Objects.requireNonNull(password);
		Objects.requireNonNull(path);
		
		if (port < 0) {
			throw new IllegalArgumentException("Port number cannot be negative");
		}
		
		LogTool.traceC(this.getClass(), "Creating SFTP Manager");
		this.jsch = new JSch(); //Should really Dependancy Inject this...
		this.username = username;
		this.host = host;
		this.port = port;
		this.password = password;
		this.path = path;
	}

	/* (non-Javadoc)
	 * @see org.dolan.remoteaccess.ISFTPManager#connect()
	 */
	@Override
	public void connect() throws JSchException {
		LogTool.traceC(this.getClass(), "Connecting as " + this.username + " on " + this.host);
		session = jsch.getSession(this.username, this.host, this.port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(this.password);
		session.connect();
		LogTool.traceC(this.getClass(), "Connected");
	}

	/* (non-Javadoc)
	 * @see org.dolan.remoteaccess.ISFTPManager#disconnect()
	 */
	@Override
	public void disconnect() {
		LogTool.traceC(this.getClass(), "Disconnecting");
		this.session.disconnect();
	}

	/**
	 * Gets the files.
	 *
	 * @param directory the directory
	 * @param filterFileType the filter file type
	 * @return the files
	 * @throws JSchException the JSch exception
	 * @throws SftpException the SFTP exception
	 */
	@SuppressWarnings("unchecked")
	private List<ServerFile> getFiles(String directory, String filterFileType) throws JSchException, SftpException {
		LogTool.traceC(this.getClass(), "Begin getting files from server");
		List<ServerFile> files = new ArrayList<ServerFile>();

		connect();
		ChannelSftp sftpChannel = sftpConnect();
		Vector<LsEntry> filelist = null;
		filelist = sftpChannel.ls(directory);

		for (int i = 0; i < filelist.size(); i++) {
			String fileName = filelist.get(i).getFilename();
			if (FilenameUtils.getExtension(fileName).equals(filterFileType)) {
				SftpATTRS attrs = filelist.get(i).getAttrs();
				String fileDate = attrs.getMtimeString();
				long fileSize = attrs.getSize();
				files.add(new ServerFile(fileName, fileSize, fileDate));
				LogTool.traceC(this.getClass(), "Found file in server: " + filelist.get(i).getFilename() + "; size: " + fileSize);
			}
		}
		disconnect();
		LogTool.traceC(this.getClass(), "Finish getting files");
		return files;
	}

	/* (non-Javadoc)
	 * @see org.dolan.remoteaccess.ISFTPManager#getFiles(java.lang.String)
	 */
	@Override
	public List<ServerFile> getFiles(String filterFileType) throws JSchException, SftpException {
		Objects.requireNonNull(filterFileType);
		if (filterFileType.isEmpty()) {
			throw new IllegalArgumentException("File extention cannot be blank");
		}
		return getFiles(getPath(), filterFileType);
	}

	/* (non-Javadoc)
	 * @see org.dolan.remoteaccess.ISFTPManager#sftpConnect()
	 */
	@Override
	public ChannelSftp sftpConnect() throws JSchException {
		LogTool.traceC(this.getClass(),"Creating a SFTP connection");
		Channel channel = session.openChannel("sftp");
		channel.connect();
		ChannelSftp sftpChannel = (ChannelSftp) channel;
		return sftpChannel;
	}

	/* (non-Javadoc)
	 * @see org.dolan.remoteaccess.ISFTPManager#getPath()
	 */
	@Override
	public String getPath() {
		return this.path;
	}
}
