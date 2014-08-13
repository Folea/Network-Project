package es.um.redes.Confiable.Medio;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class MediumSocket implements MediumSocketIface
{
	private DatagramSocket socket;

	private final String CONF_FILE_NAME = "MediumLayer.conf";
	private String errorPattern;
	private int indexPattern;
	private boolean iterative;

	/**
	 * Constructor para el cliente.
	 * 
	 * @throws IOException
	 */
	public MediumSocket() throws IOException
	{
		readConfigFile();

		socket = new DatagramSocket();
		socket.setReuseAddress(true);
	}

	/**
	 * Constructor para el servidor.
	 * 
	 * @param port El puerto
	 * @throws IOException
	 */
	public MediumSocket(int port) throws IOException
	{ //Constructor para el servidor
		readConfigFile();
		InetSocketAddress inet_addr = new InetSocketAddress(port);
		socket = new DatagramSocket(inet_addr);
		socket.setReuseAddress(true);
	}

	private void readConfigFile()
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(CONF_FILE_NAME));
	        errorPattern = br.readLine();
	        indexPattern = 0;
	        if (errorPattern.charAt(errorPattern.length() - 1) == '*') {
	        	iterative = true;
	        	errorPattern = errorPattern.substring(0, errorPattern.length() - 1);
	        } else {
	        	iterative = false;
	        	errorPattern += 'B';
	        }
	        br.close();
		} catch (IOException e) {
	    	System.err.println("No hay fichero de configuración. No se perderá ningún paquete.");
	    	errorPattern = "B";
	    	indexPattern = 0;
	    	iterative = true;
	    }
	}

	/* (non-Javadoc)
	 * @see es.um.redes.ComConfiable.MediumLayer.MediumSocketIface#send(es.um.redes.ComConfiable.MediumLayer.MediumSegment)
	 */
	@Override
	public void send(MediumPacket seg) throws IOException
	{
		DatagramPacket p;
		switch (errorPattern.charAt(indexPattern)) {
		case 'B':
			// Send packet correctly
			p = new DatagramPacket(seg.getData(), seg.getLength(), seg.getInetAddr());
			socket.send(p);
			break;
		case 'P':
			// Do not send packet
			break;
		case 'E':
			// Corrupt packet and send
			byte[] data = seg.getData().clone();
			Random r = new Random();

			// Modificar como máximo el 10% de los bytes
			int j = 1 + r.nextInt (1 + (data.length / 10));
			for (int i = 0 ; i < j; ++i)
				data[r.nextInt(data.length)] ^= 1 + r.nextInt(255);

			p = new DatagramPacket(data, seg.getLength(), seg.getInetAddr());
			socket.send(p);
			break;
		default:
			System.err.println("Invalid character in the pattern.");
			break;
		}
		if (iterative) {
			indexPattern = (indexPattern + 1) % errorPattern.length();
		} else {
			if (indexPattern < errorPattern.length() - 1) indexPattern++;
		}
	}

	// Ojo, siempre no bloqueante
	/* (non-Javadoc)
	 * @see es.um.redes.ComConfiable.MediumLayer.MediumSocketIface#receive(es.um.redes.ComConfiable.MediumLayer.MediumSegment)
	 */
	@Override
	public MediumPacket receive() throws IOException
	{
		MediumPacket seg = new MediumPacket();
		byte[] buf = new byte[64*1024 + 1024]; //FIXME: fijo
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		try {
			socket.setSoTimeout(50);
			socket.receive(p);
		} catch (SocketTimeoutException e) {
			return null;
		}
		seg.setData(Arrays.copyOf(buf, p.getLength()));
		seg.setLength(p.getLength());
		seg.setInetAddr((InetSocketAddress)p.getSocketAddress());
		return seg;
	}
}
