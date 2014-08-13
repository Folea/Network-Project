package es.um.redes.Confiable.Medio;

import java.net.InetSocketAddress;

public class MediumPacket {

	private byte[] data;
	private int len;
	private InetSocketAddress inet_addr;
	
	// Constructor para recepción
	public MediumPacket()
	{
		data = null;
		len = 0;
		inet_addr = null;
	}
	
	public MediumPacket(byte[] data, int len, InetSocketAddress inet_addr) {
		this.data = data;
		this.len = len;
		this.inet_addr = inet_addr;
	}

	public byte[] getData() {
		return data;
	}
	
	public int getLength() {
		return len;
	}
	
	public InetSocketAddress getInetAddr() {
		return inet_addr;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public void setLength(int len) {
		this.len = len;
	}
	
	public void setInetAddr(InetSocketAddress inet_addr) {
		this.inet_addr = inet_addr;
	}

}
