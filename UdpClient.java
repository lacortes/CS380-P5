import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.Random;

// Luis Cortes
// CS 380
// Project 5

public class UdpClient {
	public static void main(String[] args) {

		try {
			// Get IP Address
			InetAddress address = InetAddress.getByName(
				new URL("http://codebank.xyz").getHost());
			String ip = address.getHostAddress();
			System.out.println(ip);


			// Connect to server
			Socket socket = new Socket(ip, 38005);
			// System.out.println("Connected to server.");

			// Destination addr
			byte[] destination = socket.getInetAddress().getAddress();

			PrintStream outStream = new PrintStream(socket.getOutputStream(), true);
			InputStream is = socket.getInputStream();

			// HandShake info
			byte[] outgoing = {(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};

			Ipv4 ipPacket;
			byte[] response = new byte[4]; 

			// Set the destination and data for packet
			ipPacket = new Ipv4(destination, outgoing);

			// Send to server
			outStream.write(ipPacket.getPacket());

			// Server handshake response
			is.read(response);

			// Show response 
			System.out.print("Handshake response: 0x");
			for (byte data : response) 
				System.out.print(Integer.toHexString(data & 0xFF));
			System.out.println();

			// Set for 2 byte port response, and read in
			byte[] responsePort = new byte[2];
			is.read(responsePort);

			int portNumber = (responsePort[0] & 0xFF);
			portNumber = ( (portNumber << 8) | (responsePort[1] & 0xFF) );
			System.out.printf("Port number received %d%n%n", portNumber);

			double avgRTT = 0;

			// Send udp packets
			for (int i = 1, j = 2; i <= 12; i++, j *= 2) {
				System.out.printf("Send packet with %s bytes of data%n", j);
				// Random data
				byte[] randomData = new byte[j];
				Random random = new Random();
				random.nextBytes(randomData);

				// Ipv4/UDP packet
				ipPacket = new Ipv4(destination, randomData, responsePort);

				// Time
				long sendTime = System.currentTimeMillis();
				outStream.write(ipPacket.getPacket());

				is.read(response);
				long receiveTime = System.currentTimeMillis();

				// RTT
				long lengthTime = receiveTime - sendTime;
				avgRTT += lengthTime;

				// Show response 
				System.out.print("Response: 0x");
				for (byte data : response) {
					System.out.print(Integer.toHexString(data & 0xFF));
				}
				System.out.println();
				System.out.printf("RTT: %dms%n", lengthTime);
				System.out.println();
			}

			// Get average RTT
			avgRTT /= 12;
			System.out.printf("Average RTT: %.2fms%n", avgRTT );




		} catch (Exception e) {e.printStackTrace();}
	}


}