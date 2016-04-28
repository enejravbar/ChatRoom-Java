import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.*;

public class ChatClient extends Thread
{
	protected int serverPort = 1234;

	public static void main(String[] args) throws Exception {
		new ChatClient();
		
	}

	public ChatClient() throws Exception {
		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		Scanner sc = new Scanner(System.in);
		String uporabnik="";
		// connect to the chat server
		try {
			System.out.println("[system] connecting to chat server ...");
			socket = new Socket("localhost", serverPort); // create socket connection
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages
			System.out.println("[system] connected");
			uporabnik=JOptionPane.showInputDialog("Vpisite svoje uporabnisko ime."); // posreduj imeUporabnika
			while(uporabnik.length()==0){
				uporabnik=JOptionPane.showInputDialog("Vpisite veljavno uporabnisko ime."); // posreduj imeUporabnika
			}
			System.out.println("Prijavljen si kot " + "\""+uporabnik+"\".");
			this.sendMessage(uporabnik,out);
			ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in); // create a separate thread for listening to messages from the chat server
			message_receiver.start(); // run the new thread
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		

		// read from STDIN and send messages to the chat server
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		while ((userInput = std_in.readLine()) != null) { // read a line from the console
			this.sendMessage( kreirajSporocilo(uporabnik, preveriAliJeSporociloPrivatno(userInput), pridobiNaslovnika(userInput), userInput) , out); // send the message to the chat server
		}

		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
	}

	private void sendMessage(String message, DataOutputStream out) {
		try {
			out.writeUTF(message); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}


	/* Metoda kreira sporocilo, ki vsebuje vse potrebno podatke, ki jih mora prejeti strežnik. Gre za neke vrste moj JSON format */
	/*
	sporocilo= "uporabnik:Enej nacin:1=privatno 0=javno cas: trenutniCas sporocilo: "
	*/


	private static String kreirajSporocilo(String uporabnik, String nacin, String naslovnik,String sporocilo ){
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
       	String cas=sdf.format(cal.getTime());

       	String tabela[] = new String[1000];
       	if(preveriAliJeSporociloPrivatno(sporocilo).equals("1")){
			tabela=sporocilo.split("\"");
			sporocilo="";
			for(int i=2; i<tabela.length;i++){
				sporocilo=sporocilo+tabela[i];
			}
			if(sporocilo.charAt(0)==' '){
				sporocilo=sporocilo.substring(1, sporocilo.length());
			}
			
		}

		String message= "{"+uporabnik + "}" + "{"+nacin+"}" + "{"+naslovnik +"}" + "{"+ cas+"}"+ "{"+sporocilo+"}";
		return message;
	}
	private static String preveriAliJeSporociloPrivatno(String sporocilo){
		if(sporocilo.indexOf("/privatno")>-1){
			return "1"; // sporocilo je privatno
		}else{
			return "0"; // sporocilo je javno
		}

	}
	private static String pridobiNaslovnika(String sporocilo){
		String tabela[] = new String[1000];

		if(preveriAliJeSporociloPrivatno(sporocilo).equals("1")){
			tabela=sporocilo.split("\"");
			//System.out.println("Naslovnik je: " + tabela[1]);
			return tabela[1]; // vrni naslovnika
		}
		return null;

	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String message;
			while ((message = this.in.readUTF()) != null) { // read new message
				System.out.println("[RKchat] " + message); // print the message to the console
			}
		} catch (Exception e) {
			System.out.println("[system] Povezava s streznikom je bila izgubljena!");
			//e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
