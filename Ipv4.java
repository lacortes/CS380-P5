// Luis Cortes
// CS 380
// Project 5 (UDP/ IP)


public class Ipv4 {
	private static int datagramLength = 1;

	private byte version; // version (4 bits) 
	private byte hLen; // header length in words (4 bits)
	private byte tos; // type of service (8 bits)
	private short length;  // total length in bytes( 16 bits)
	private short identity; // identification (16 bits)
	private byte flags; // 3 bits
	private short offset; // 13 bits
	private byte ttl; // time to live  (8 bits)
	private byte protocol; // 8 bits
	private short checksum; // 16 bits
	private byte[] sourceAdd = {(byte)0x0A, (byte)0x2F, 
							(byte)0x96, (byte)0x23}; // source address 32 bits
	private byte[] destinationAdd; // source address 32 bits

	private byte[] data; // Hold data
	private byte[] packet; // Hold the whole packet

	private boolean udp = false;
	private byte[] port; 

	/**
	 *	Initialize all assumed information
	 */
	public Ipv4(byte[] destAddr, byte[] incomingData) {
		this.version = 0x4;;
		this.hLen = 0x5; // No options or padding implemented
		this.tos = 0x0; // Not implemented
		this.length = 0x14; // Initial state
		this.identity = 0x0; // Not implemented
		this.flags = 0x02; // Assuming no fragemtation
		this.offset = 0x0; // No implementation
		this.ttl = 0x32; // Assume every packet has ttl of 50
		this.protocol = 0x11; // Protocol number for UDP
		this.checksum = 0x0000; // Initially set to 0
		this.destinationAdd = destAddr;  

		this.data = incomingData;
		this.length += incomingData.length;
	}

	/**
	 *	Configure for UDP Packet
	 */
	public Ipv4(byte[] destAddr, byte[] incomingData, byte[] destPort) {
		this(destAddr, incomingData);
		// Increase default packet length
		udp = true; 
		this.port = destPort;
		this.length += 8; // Default size of a UDP Header
		this.datagramLength *= 2;
 	}

	/**
	 *	Get the packet
	 */
	public byte[] getPacket() {
		this.makePacket();
		
		if (udp) {
			this.udp();
		}
		return this.packet;
	}

	/**
	 *	Return size of data
	 */
	public int size() {
		return this.length;
	}

	/**
	 *	UDP Header   
	 */
	private void udp() {
		// Source Port
		packet[20] = 0;
		packet[21] = 0;

		// Destination Port
		packet[22] = port[0];
		packet[23] = port[1];

		// UDP Length 
		int udpLen = 8 + this.data.length;
		packet[24] = (byte) (udpLen >> 8 & 0xFF);
		packet[25] = (byte) (udpLen & 0xFF); 

		// UDP Checksum, initial values
		packet[26] = 0; 
		packet[27] = 0;

		// Data 
		for (int i=28, j=0; j < this.data.length; i++, j++) {
			packet[i] = data[j];
		}

		// Pseudo packet
		byte[] pseudoHeaderPacket = pseudoHeader();
		packet[26] = pseudoHeaderPacket[18];
		packet[27] = pseudoHeaderPacket[19];
	}

	private byte[] pseudoHeader() {
		// Size of packet is 20 bytes (initial size) plus data
		byte[] pseudoHeader = new byte[this.datagramLength + 20];
		int index = 0; 

		// Source address
		pseudoHeader[index++] = sourceAdd[0];
		pseudoHeader[index++] = sourceAdd[1];
		pseudoHeader[index++] = sourceAdd[2];
		pseudoHeader[index++] = sourceAdd[3];

		// Destination address
		pseudoHeader[index++] = destinationAdd[0];
		pseudoHeader[index++] = destinationAdd[1];
		pseudoHeader[index++] = destinationAdd[2];
		pseudoHeader[index++] = destinationAdd[3];

		// Zeroes
		pseudoHeader[index++] = 0; 

		// Protocol; 
		pseudoHeader[index++] = 0x11;

		// UDP Length
		pseudoHeader[index++] = packet[24];
		pseudoHeader[index++] = packet[25];

		// Source port
		pseudoHeader[index++] = packet[20];
		pseudoHeader[index++] = packet[21];

		// Destination port
		pseudoHeader[index++] = packet[22];
		pseudoHeader[index++] = packet[23];

		// Length
		pseudoHeader[index++] = packet[24];
		pseudoHeader[index++] = packet[25];

		// Checksum 
		pseudoHeader[index++] = packet[26]; 
		pseudoHeader[index++] = packet[27];

		// Data 
		for (byte info : data) {
			pseudoHeader[index++] = info;
		}

		// Checksum
		short chkSum = calculateChecksum(pseudoHeader);
		int upper = (chkSum >> 8 & 0xFF);
		int lower = (chkSum & 0xFF);

		// Set checksum values in packet
		pseudoHeader[18] = (byte) upper;
		pseudoHeader[19] = (byte) lower;


		return pseudoHeader;
	}

	/**
	 *	Make a packet of bytes
	 */
	private void makePacket() {
		this.packet = new byte[this.length];
		int index = 0; 

		// Put together version and Hlen
		byte versionAndHlen = (byte) ( (this.version << 4) | (this.hLen & 0xF) );
		this.packet[index++] = versionAndHlen;

		// TOS
		this.packet[index++] = 0x00;

		// Length, break down into two bytes
		this.packet[index++] =  (byte) ((this.length >> 8) & 0xFF);
		this.packet[index++] = (byte) (this.length & 0xFF);

		// Identitiy
		this.packet[index++] = 0x00;
		this.packet[index++] = 0x00;

		// Combine flags, and upper part of Offset
		this.packet[index++] = (byte) ((flags<<5) | (0xFF & 0x00));

		// Second half of offset
		this.packet[index++] = 0x00; 

		// TTL
		this.packet[index++] = (byte) (this.ttl & 0xFF);

		// Protocol
		this.packet[index++] = (byte) (this.protocol & 0xFF);

		// Checksum
		this.packet[index++] = (byte) 0x00; 
		this.packet[index++] = (byte) 0x00; 

		// Source IP
		for (int i = 0; i < sourceAdd.length; i++) {
			packet[index++] = sourceAdd[i]; 
		}

		// Destination IP broken down into 4 bytes
		// Destination Address
		for (int i = 0; i < destinationAdd.length; i++) {
			packet[index++] = destinationAdd[i];
		}

		// Checksum
		short chkSum = calculateChecksum(packet);
		int upper = (chkSum >> 8 & 0xFF);
		int lower = (chkSum & 0xFF);

		// Set checksum values in packet
		this.packet[10] = (byte) upper;
		this.packet[11] = (byte) lower;

		// Add data after destinaion address if no UDP
		if (!udp) { 
			// Copy data on to packet
			for (byte info : data) {
				this.packet[index++] = info; 
			}
		}
	}

	/**
	 *  Calculate checksum and put it as a byte arraay
	 */
	private short calculateChecksum(byte[] buf) {
    	int i = 0;
    	int sum = 0;

    	// Handle all pairs
    	while (i < buf.length - 1) {
		    byte first =  buf[i];
		    byte second = buf[i + 1];

		    sum = sum + ( (first << 8 & 0xFF00) + (second & 0xFF) );
 
      		if ((sum & 0xFFFF0000) > 0) {
        		sum &= 0xFFFF;
        		sum++;
      		}
      		i += 2;
    	}

    	// Handle remaining byte in odd length buffers
    	if (buf.length % 2 == 1) { 
    		byte last = buf[buf.length -1];
      		sum += ( (last << 8) & 0xFF00);
      		
      		if ((sum & 0xFFFF0000) > 0) {
        		sum = sum & 0xFFFF;
        		sum += 1;
      		}
    	}
    	return (short) ~(sum & 0xFFFF);
  	}

  	// Packet representation in hex values
  	public String toString() {
  		String x = "[ ";
  		for (byte info : packet) {
  			x += ""+Integer.toHexString(info & 0xFF);
  			x += " ";
  		}
  		x += "]";
  		return x;
  	}

}