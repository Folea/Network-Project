package es.um.redes.Confiable.Intercambio;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Interfaz que especifica los métodos que contiene una conexión confiable
 */
public interface ReliableConnectionIface
{
	/**
	 * Usado por el cliente. 	
	 * @param to
	 * @throws IOException
	 */
	public abstract void connect(InetAddress to) throws IOException;

	/**
	 * Usado por el servidor	
	 * @throws IOException
	 */
	public abstract void accept() throws IOException;

	/**
	 * @return El MSS de esta conexión
	 */
	public abstract int getMSS();

	/**
	 * Cierra la conexión
	 * @throws IOException
	 */
	public abstract void close() throws IOException;

	/**
	 * @return El valor del rimeout
	 */
	public abstract int getTimeoutValue();

	/**
	 * @return La dirección de la otra parte
	 */
	public abstract InetAddress getPeerAddress();

	/**
	 * Escribe en la conexión el contenido del buffer
	 * @param buffer
	 * @return El número de bytes escritos
	 * @throws IOException
	 */
	public abstract int write(byte[] buffer) throws IOException;

	/**
	 * Lee nuevos datos
	 * @return Un array de bytes, o null si no se ha leído nada
	 * @throws IOException
	 */
	public abstract byte[] read() throws IOException;

}