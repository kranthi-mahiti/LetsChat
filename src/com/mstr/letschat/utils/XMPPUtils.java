package com.mstr.letschat.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import android.app.Application;
import android.util.Log;

import com.mstr.letschat.tasks.AddAccountTask.AccountCreationResult;

public class XMPPUtils {
	private static final boolean DEBUG = true;
	private static final String LOG_TAG = "XMPPUtils";
	
	private static final String HOST = "10.197.34.151";
	private static final int PORT = 5223;
	
	private static XMPPConnection con;
	
	private static Application application;
	
	public static void init(Application app) {
		SmackAndroid.init(app);
		
		application = app;
	}
	
	public static boolean login(String username, String password) {
		try {
			connectIfNecessary();
			
			con.login(username, password);
			
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			con.addPacketListener(new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					Log.d(LOG_TAG, message.getBody());
				}}, filter);
			
			return true;
		} catch (SmackException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static AccountCreationResult addAccount(String name, String password) {
		try {
			connectIfNecessary();
			
			AccountManager.getInstance(con).createAccount(name, password);
			
			return AccountCreationResult.SUCCESS;
		} catch (SmackException e) {
			e.printStackTrace();
		} catch(XMPPErrorException e) {
			if (e.getXMPPError().getCondition().equals(Condition.conflict)) {
				return AccountCreationResult.CONFLICT;
			}
		}
		
		return AccountCreationResult.FAILURE;
	}
	
	public static boolean sendChatMessage(String to, String body) {
		connectIfNecessary();
		
		Message message = new Message(to, Message.Type.chat);
		message.setBody(body);
		try {
			con.sendPacket(message);
			return true;
		} catch (NotConnectedException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static List<RosterEntry> getRosterEntries() {
		connectIfNecessary();
		
		List<RosterEntry> result = new ArrayList<RosterEntry>();
		
		Roster roster = con.getRoster();
		Collection<RosterGroup> groups = roster.getGroups();
		for (RosterGroup group : groups) {
			result.addAll(group.getEntries());
		}
		
		return result;
	}
	
	private static void connectIfNecessary() {
		try {
			if (con == null) {
				ConnectionConfiguration config = new ConnectionConfiguration(HOST, PORT);
				loadCA(config);
				con = new XMPPTCPConnection(config);
				con.connect();
			} else {
				if (!con.isConnected()) {
					con.connect();
				}
			}
		} catch(SmackException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(XMPPException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadCA(ConnectionConfiguration config){
		// Load CAs from an InputStream
		// (could be from a resource or ByteArrayInputStream or ...)
		InputStream caInput = null;
		try {
			caInput = application.getAssets().open("server.crt");
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			// From https://www.washington.edu/itconnect/security/ca/load-der.crt
			Certificate ca = cf.generateCertificate(caInput);
			// Create a KeyStore containing our trusted CAs
			String keyStoreType = KeyStore.getDefaultType();
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);
	
			// Create a TrustManager that trusts the CAs in our KeyStore
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);
	
			// Create an SSLContext that uses our TrustManager
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
			
			config.setSocketFactory(sslContext.getSocketFactory());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if (caInput != null) {
				try {
					caInput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}