package es.um.redes.Confiable.Intercambio.data;

public interface SegmentIface 
{

	/** Devuelve el número de secuencia del segmento
	 * @return the seq_n
	 */
	public abstract int getSeq_n();

	/** Devuelve el número de ACK del segmento
	 * @return the ack_n
	 */
	public abstract int getAck_n();

	/** Devuelve la longitud de los datos
	 * @return the data_length
	 */
	public abstract int getData_length();

	/** Devuelve el contenido del campo de datos
	 * @return the data
	 */
	public abstract byte[] getData();

	/** Devuelve la codificación como array de bytes del segmento
	 * @return the data
	 */
	public abstract byte[] toByteArray();
	
	
	/** Interpreta un segmento a partir de un array de bytes
	 * @param el array de bytes
	 */
	public abstract void fromByteArray(byte[] buf);
	
	/**
	 * Serializa el segmento para su listado en el log
	 */
	public abstract String toString();
}
