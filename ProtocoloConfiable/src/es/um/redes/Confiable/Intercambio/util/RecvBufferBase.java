package es.um.redes.Confiable.Intercambio.util;

import java.nio.ByteBuffer;

import es.um.redes.Confiable.Intercambio.data.SegmentIface;

public abstract class RecvBufferBase
{
	protected int available_start;
	protected int max_in_order;
	protected ByteBuffer in_tmp;
	
	RecvBufferBase()
	{
		 in_tmp = ByteBuffer.allocate(0);
	}
	
	/**
	 * Consumir buffer de recepción si hay datos recibidos en orden. Modifica los valores de los contadores
	 * de manera que reflejan la lectura del buffer. El número de bytes leídos es como máximo buf.length
	 * @param buf El buffer donde se acogerán los datos. Tiene que tener el espacio que se quiere obtener
	 * @return El número de bytes leídos, como máximo buf.length
	 */
	public abstract int obtainDataInOrder(byte[] buf);
	
	/**
	 * Añade un nuevo segmento recibido al Buffer.
	 * @param s el segmento a añadir. En el caso de GoBackN, si el segmento no coincide con el esperado en secuencia, se ignora.
	 *              También, si el segmento es anterior a getAvailableStart() ó el segmento ya está en el buffer, lo ignora.
	 */
	public abstract void addReceivedSegment(SegmentIface s);

	/**
	 * @return los datos que están disponibles en el buffer (siempre en bytes)
	 */
	public int getAvailableDataLength() {
		return getMaxReceivedInOrder() - getAvailableStart();
	}

	/**
	 * @return Máximo recibido en orden. En el caso de TCP especifica el siguiente byte esperado (0 inicialmente).
	 *            En el caso de GoBackN especifica el número máximo del segmento que se ha recibido en orden (-1 inicialmente) 
	 */
	public int getMaxReceivedInOrder() {
		return max_in_order;
	}
	
 	/**
 	 * @return Posición del principio del buffer actualmente. Esto es, la secuencia del segmento más antiguo recibido y no consumido
 	 *  (inicialmente 0 en TCP, -1 en GoBackN)
 	 */
	public int getAvailableStart() {
		return available_start;
	}
}