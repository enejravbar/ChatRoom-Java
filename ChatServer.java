import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

	protected int serverPort = 1234;
	protected List<Socket> clients = new ArrayList<Socket>(); // list of clients
	protected List<String> uporabniskaImena = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				synchronized(this) {
					clients.add(newClientSocket); // add client to the list of clients

				}
				DataInputStream in = new DataInputStream(newClientSocket.getInputStream()); // dodaj uporabnika na listo uporabnikov
				String uporabnik=in.readUTF();
				uporabniskaImena.add(uporabnik);
				System.out.println("Pridruzil se je uporabnik " + "\""+uporabnik+"\"");

				/*for(int i=0; i<uporabniskaImena.size(); i++){
					System.out.println(uporabniskaImena.get(i));
				}*/

				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		
		for(int i=0; i< clients.size(); i++){
			Socket socket = clients.get(i); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}

	public void posljiPrivatnoSporocilo(String message, String naslovnik, String uporabnik) throws Exception {
		Socket povratniSocket=null;
		int kontrola=0;
		for(int i=0; i< clients.size(); i++){
			if((uporabniskaImena.get(i)).equals(uporabnik)){
				povratniSocket= clients.get(i);
			}
			if((uporabniskaImena.get(i)).equals(naslovnik)){
				Socket privatniSocket = clients.get(i); // get the socket for communicating with this client

				try {
				DataOutputStream out = new DataOutputStream(privatniSocket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
				kontrola=1;
				} catch (Exception e) {
				System.err.println("[system] Napaka pri posiljanju!");
				e.printStackTrace(System.err);
				kontrola=0;
				}
				break;
			}if(i==clients.size()-1 && kontrola == 0){

				DataOutputStream out = new DataOutputStream(povratniSocket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF("Privatnega sporocila ni bilo mogoce posredovati! Uporabnik ni prijavljen."); // send message to the client
				break;
			}
			
		}
	}

	public void posljiObvestiloOOdjaviUporabnika(Socket socket) {
		for(int i=0; i<clients.size();i++){
			if(clients.get(i)==socket){
				try{
				removeClient(socket);
				sendToAllClients("Uporabnik "+"\""+uporabniskaImena.get(i)+"\" je zapustil klepetalnico.");
				System.out.println("Uporabnik "+"\""+uporabniskaImena.get(i)+"\" je zapustil klepetalnico.");
				uporabniskaImena.remove(i);
				}catch(Exception ex){

				}

			}
		}
	}

	public void removeClient(Socket socket) {
		synchronized(this) {
			clients.remove(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;

	public ChatServerConnector(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
	}

	public void run() {
		
		DataInputStream in;

		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}
		int stevec=0;
		String[] podatkiOSporocilu;
		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;

			try {
				msg_received = in.readUTF(); // read the message from the client
				podatkiOSporocilu =tolmaciSporocilo(msg_received);
				//izpisiTabelo(tabela);
			} catch (Exception e) {
				//System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort());
				//e.printStackTrace(System.err);

				this.server.posljiObvestiloOOdjaviUporabnika(this.socket);
				//this.server.removeClient(this.socket);
				return;
			}

			if (msg_received.length() == 0) // invalid message
				continue;

			System.out.println("[RKchat] [" + this.socket.getPort() + "] " + "Uporabnik: \"" + podatkiOSporocilu[0] + "\" Cas: "+ podatkiOSporocilu[3] + "\nSporocilo: "+ podatkiOSporocilu[4]); // print the incoming message in the console

			try {	
				if(podatkiOSporocilu[1].equals("0")){  // imamo javno sporocilo
						this.server.sendToAllClients(podatkiOSporocilu[0]+ ": " + podatkiOSporocilu[4]); // send message to all clients
				}else{
						this.server.posljiPrivatnoSporocilo( ("Privatno od "+podatkiOSporocilu[0]+ ": " + podatkiOSporocilu[4] ), podatkiOSporocilu[2], podatkiOSporocilu[0] );
				}
			} catch (Exception e) {
				System.err.println("[system] there was a problem while sending the message to all clients");
				e.printStackTrace(System.err);
				continue;
			}
		}
	}
	private static String[] tolmaciSporocilo(String sporocilo){
		String podatkiOSporocilu[]=new String[5];
		int stevec=-1;
		String temp="";
		for(int i=0; i< sporocilo.length(); i++){
			if(sporocilo.charAt(i)=='{'){
				stevec++;
				continue;
			}
			if(sporocilo.charAt(i)=='}'){
				podatkiOSporocilu[stevec]=temp;
				temp="";
				continue;
			}
			temp=temp+sporocilo.charAt(i);
		}
		return podatkiOSporocilu;
	}
	private static void izpisiTabelo(String[] tabela){
		for(int i=0; i<tabela.length; i++){
			System.out.println(tabela[i]);
		}
	}
}
