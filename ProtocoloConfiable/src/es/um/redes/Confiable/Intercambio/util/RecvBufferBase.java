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
	 * Consumir buffer de recepci�n si hay datos recibidos en orden. Modifica los valores de los contadores
	 * de manera que reflejan la lectura del buffer. El n�mero de bytes le�dos es como m�ximo buf.length
	 * @param buf El buffer donde se acoger�n los datos. Tiene que tener el espacio que se quiere obtener
	 * @return El n�mero de bytes le�dos, como m�ximo buf.length
	 */
	public abstract int obtainDataInOrder(byte[] buf);
	
	/**
	 * A�ade un nuevo segmento recibido al Buffer.
	 * @param s el segmento a a�adir. En el caso de GoBackN, si el segmento no coincide con el esperado en secuencia, se ignora.
	 *              Tambi�n, si el segmento es anterior a getAvailableStart() � el segmento ya est� en el buffer, lo ignora.
	 */
	public abstract void addReceivedSegment(SegmentIface s);

	/**
	 * @return los datos que est�n disponibles en el buffer (siempre en bytes)
	 */
	public int getAvailableDataLength() {
		return getMaxReceivedInOrder() - getAvailableStart();
	}

	/**
	 * @return M�ximo recibido en orden. En el caso de TCP especifica el siguiente byte esperado (0 inicialmente).
	 *            En el caso de GoBackN especifica el n�mero m�ximo del segmento que se ha recibido en orden (-1 inicialmente) 
	 */
	public int getMaxReceivedInOrder() {
		return max_in_order;
	}
	
 	/**
 	 * @return Posici�n del principio del buffer actualmente. Esto es, la secuencia del segmento m�s antiguo recibido y no consumido
 	 *  (inicialmente 0 en TCP, -1 en GoBackN)
 	 */
	public int getAvailableStart() {
		return available_start;
	}
}