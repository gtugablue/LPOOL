package lpool.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Network {
	public final int maxClients;
	private int numClients;
	private ServerSocket serverSocket;
	private DatagramSocket UDPServerSocket;
	private Connector con;
	private DatagramReceiver dg;
	private Communication[] comms;

	private Queue<Integer> clientConnEvents;
	private Queue<Integer> clientCommEvents;

	private Queue<DatagramPacket> clientCommPackets;

	private ConcurrentLinkedQueue<Socket> clientSockets;
	private ConcurrentLinkedQueue<DatagramPacket> UDPreceived;
	
	// Types of service
	private static final int IPTOS_LOWCOST = 0x02;
	private static final int IPTOS_RELIABILITY = 0x04;
	private static final int IPTOS_THROUGHPUT = 0x08;
	private static final int IPTOS_LOWDELAY = 0x10;
	
	private ObservableConnection obsConn;
	private ObservableMessage obsMsg;

	public Network(int maxClients) {
		try {
			this.serverSocket = new ServerSocket(6900);
			this.UDPServerSocket = new DatagramSocket(6900);
			UDPServerSocket.setTrafficClass(IPTOS_LOWDELAY);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Server socket successfully started.");
		this.clientSockets = new ConcurrentLinkedQueue<Socket>();
		this.con = new Connector(serverSocket, maxClients, clientSockets);
		this.UDPreceived = new ConcurrentLinkedQueue<DatagramPacket>();
		dg = new DatagramReceiver(UDPServerSocket, UDPreceived);
		dg.start();
		this.comms = new Communication[maxClients];
		this.maxClients = maxClients;
		this.numClients = 0;
		this.clientConnEvents = new LinkedList<Integer>();
		this.clientCommEvents = new LinkedList<Integer>();
		this.clientCommPackets = new LinkedList<DatagramPacket>();
		
		obsConn = new ObservableConnection(this);
		obsMsg = new ObservableMessage(this);
	}

	public void tick()
	{
		while (!clientSockets.isEmpty())
		{
			Socket clientSocket = clientSockets.poll();
			if (!addClient(clientSocket))
			{
				try {
					clientSocket.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}

		for (int i = 0; i < maxClients; i++)
		{
			if (comms[i] != null)
			{
				if (comms[i].isConnClosed())
				{
					kickClient(i);
					clientConnEvents.add(i);
				}
			}
		}

		readUDP();
		
		obsConn.tick();
		obsMsg.tick();
	}

	private void readUDP()
	{
		while (!UDPreceived.isEmpty())
		{
			DatagramPacket dp = UDPreceived.poll();
			int clientID = addressToID(dp.getAddress());
			if (isClientConnected(clientID))
			{
				clientCommEvents.add(clientID);
				clientCommPackets.add(dp);
			}
		}
	}

	private boolean addClient(Socket client)
	{
		if (numClients >= maxClients)
		{
			return false; // Too many clients
		}

		for (int i = 0; i < comms.length; i++)
		{
			if (comms[i] == null)
				continue;

			if (client.getInetAddress().getHostAddress().equals(comms[i].getSocket().getInetAddress().getHostAddress()))
			{
				return false; // Client already connected
			}
		}

		for (int i = 0; i < comms.length; i++)
		{
			if (comms[i] == null)
			{
				numClients++;
				comms[i] = new Communication(client);
				clientConnEvents.add(i);
				return true; // Success
			}
		}
		return false; // No null position in clients array
	}

	public boolean kickClient(int clientID)
	{
		if (clientID >= maxClients)
			return false;

		if (comms[clientID] == null)
			return false;

		try {
			comms[clientID].getSocket().close();
		} catch (IOException e) {
			// Do nothing
		}
		comms[clientID].close();
		comms[clientID] = null;
		numClients--;
		return true;
	}

	public void startConnecting()
	{
		con.start();
	}

	public boolean isClientConnQueueEmpty()
	{
		return clientConnEvents.isEmpty();
	}

	public Integer pollClientConnQueue()
	{
		if (clientConnEvents.isEmpty())
			return null;
		else
			return clientConnEvents.poll();
	}
	
	public String pollClientCommQueue(Integer clientID)
	{
		if (!clientCommEvents.isEmpty())
		{
			clientID = clientCommEvents.poll();
			return new String(clientCommPackets.poll().getData());
		}
		else
		{
			for (int i = 0; i < maxClients; i++)
			{
				if (isClientConnected(i))
				{
					ConcurrentLinkedQueue<String> q = comms[clientID].getClientCommEvents();
					if (q.isEmpty())
						continue;
					clientID = i;
					return q.poll();
				}
			}
		}
		return null;
	}

	public boolean isClientConnected(int clientID) {
		return clientID >= 0 && clientID < maxClients && comms[clientID] != null;
	}

	public int addressToID(InetAddress ip)
	{
		String hostAddress = ip.getHostAddress();
		for (int i = 0; i < maxClients; i++)
		{
			if (!isClientConnected(i))
				continue;

			if (comms[i].getSocket().getInetAddress().getHostAddress().equals(hostAddress))
				return i;
		}
		return -1;
	}

	public int getNumClients() {return numClients;}
	
	public void addConnObserver(Observer o)
	{
		obsConn.addObserver(o);
	}
	
	public void addMsgObserver(Observer o)
	{
		obsMsg.addObserver(o);
	}
	
	public void deleteConnObserver(Observer o)
	{
		obsConn.deleteObserver(o);
	}
	
	public void deleteMsgObserver(Observer o)
	{
		obsMsg.deleteObserver(o);
	}
}