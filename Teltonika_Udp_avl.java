package de.iris.ds.teltonika.nmea.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

/**
 * 
 * Example about receiving AVL UDP packages from IRMA Hub Road. The packages
 * contain gps tracking information.
 * 
 * Via IMEI and timestamp the data can be matched with Matrix XML Counting data,
 * received on a http port.
 *
 * For detailed information please see chapter "Sending Data over UDP/IP" under
 * 
 * https://wiki.teltonika.lt/view/RUT955_Protocols
 * 
 *
 */
class Teltonika_Udp_avl {

	public static void main(String args[]) throws Exception {

		// udp server at 17050
		DatagramSocket serverSocket = new DatagramSocket(17050);

		byte[] receiveData = new byte[1024];

		while (true) {
			receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			
			int recvData = receivePacket.getLength();
			byte[] b = receivePacket.getData();
			// wrap byte order, cause Teltonika has MIPS and uses big endian
			ByteBuffer res = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN);
			short length = res.getShort();
			short packetId = res.getShort();
			System.out.println("packetId: " + packetId);
			byte packetType = res.get();
			System.out.println("packetType: " + packetType);
			byte avlPacketId = res.get();
			System.out.println("avlPacketId: " + avlPacketId);
			short imeiLength = res.getShort();
			byte[] imei = new byte[15];
			res.get(imei, 0, imei.length);
			// IMEI of device
			System.out.println("imei: " + new String(imei));
			byte codecId = res.get();
			byte avlDataCount = res.get();
			System.out.println("AVL DataCount: " + avlDataCount);

			for (int i = 0; i < avlDataCount; i++) {
				long unixTime = res.getLong();
				System.out.println("	Date: " + new Date(unixTime));
				byte prio = res.get();
				int lon = res.getInt();
				double lonD = lon / (double) 10000000;
				int lat = res.getInt();
				double latD = lat / (double) 10000000;

				System.out.println("  Lat: " + latD + " lon: " + lonD);
				short altitude = res.getShort();
				System.out.println("  Altitude: " + altitude);
				short angle = res.getShort();
				System.out.println("  Angle: " + angle);
				byte satCount = res.get();
				System.out.println("  Sat: " + satCount);
				short speed = res.getShort();
				System.out.println("  Speed: " + speed);
				// omitt i/o element
				res.position(res.position() + 6);
			}

			byte avlCount = res.get();

			// return number of accepted data
			short packetLength = 2 /* Id */ + 1 /* Type */ + 1 /* Avl packet id */ + 1 /* num of accepted elems */ + 2;
			ByteBuffer response = ByteBuffer.allocate(packetLength).order(ByteOrder.BIG_ENDIAN);
			response.putShort(packetLength);
			response.putShort(packetId);
			response.put(packetType);
			response.put(avlPacketId);
			response.put(avlDataCount);
			byte[] ret = response.array();

			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			DatagramPacket sendPacket = new DatagramPacket(ret, ret.length, IPAddress, port);
			// send back to device as ACK
			serverSocket.send(sendPacket);
		}
	}

}
