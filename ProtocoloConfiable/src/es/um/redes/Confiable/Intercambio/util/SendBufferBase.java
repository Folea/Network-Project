/**
 * 
 */
package es.um.redes.Confiable.Intercambio.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import es.um.redes.Confiable.Intercambio.data.SegmentIface;

/**
 * @author dsevilla
 *
 */
public abstract class SendBufferBase
{
	protected int older_seq;
	protected int max_seq;
	
	protected List<SegmentIface> buffer;

	public SendBufferBase()
	{
		older_seq = numerationOffset();
		max_seq = numerationOffset();
		buffer = new LinkedList<SegmentIface>();
	}
	
	public SendBufferBase(int init_seq)
    {
        older_seq = init_seq;
        max_seq = init_seq;
        buffer = new LinkedList<SegmentIface>();
    }

	/**
	 * @return El número de secuencia del segmento más antiguo en el buffer
	 *            (el primero que se añadió al buffer y que no ha sido confirmado).
	 *            Inicialmente es 0 para TCP (el siguiente esperado), -1 para GoBackN.
	 */
	public int getOlderSeq()
	{
		return older_seq;
	}
	
	/**
	 * @return El tope del buffer de envío.
	 *            En el caso de TCP, será el número de secuencia siguiente que entraría en el buffer
	 *            En el caso de GoBackN será el número del último segmento añadido al buffer (el más reciente)
	 */
	public int getMaxSeq()
	{
		return max_seq;
	}
	
	/**
	 * @return La diferencia que queda en el buffer. 0 si no queda ninguna. 
	 *            En el caso de TCP será el número de bytes que quedan por confirmar.
	 *            En el caso de GoBackN será el número de segmentos por confirmar.
	 */
	public int stillPending()
	{
		return getMaxSeq() - getOlderSeq();
	}
	
	/**
	 * Retorna una vista de sólo lectura de la lista de segmentos que hay almacenados en el buffer.
	 * @return la vista de la lista de segmentos.
	 */
	public List<SegmentIface> segmentsPending()
	{
		return Collections.unmodifiableList(buffer);
	}
	
	/**
	 * @return El segmento más antiguo que hay almacenado en el buffer de envío. Este 
	 *            segmento se puede usar para reenviar el segmento más antiguo.
	 * @throws IndexOutOfBoundsException
	 */
	public SegmentIface getOlderSegment() throws IndexOutOfBoundsException
	{
		return buffer.get(0);
	}
	
	/**
	 * Añade un nuevo segmento enviado al buffer de envío. Si el segmento no es el esperado,
	 * se lanza la excepción {@link IllegalArgumentException}. Actualiza el contador getMaxSeq()
	 * acorde con el nuevo segmento. 
	 * @param seg El segmento a añadir.
	 * @throws IllegalArgumentException si el segmento no es el esperado
	 */
	public void addSentSegment(SegmentIface seg) throws IllegalArgumentException
	{
		if (seg.getSeq_n() + numerationOffset() != max_seq)
			throw new IllegalArgumentException("El segmento añadido al buffer de enviados no está en orden.");
		buffer.add(seg);
		max_seq += nextSeqOffset(seg);
	}
	
	/**
	 * Indica al buffer que se ha recibido un segmento con número de ACK n_ack. Se actualiza el contador
	 * retornado por getOlderSeq() para reflejar este ACK.
	 * @param n_ack El número de ACK.
	 * @throws IndexOutOfBoundsException Si el número de ACK es mayor que el máximo del buffer getMaxSeq() 
	 * @throws IllegalArgumentException Si el número de ACK no coincide con ningún segmento
	 *            almacenado en el buffer de envío 
	 */
	public void receivedACK(int n_ack) throws IndexOutOfBoundsException, IllegalArgumentException
	{
		if (n_ack > getMaxSeq())
			throw new IndexOutOfBoundsException("ACK recibido: " + n_ack + " está fuera de rango.");
		
		// Primera comprobación rápida, el buffer está vacío
		if (n_ack + numerationOffset() < getOlderSeq() || 0 == stillPending())
			return;
		
		// ¿Todos confirmados?
		// NOTA: Esto hay que hacerlo para que funcione bien este código tanto en GBN como en TCP
		if (n_ack == getMaxSeq())
		{
			older_seq = max_seq;
			buffer.clear();
			return;
		}
		
		// Para hacer la comprobación de IllegalArgumentException debemos buscar en la lista
		// para ver si hay un segmento que tiene exactamente ese número de secuencia
	    boolean found = false;
		for (SegmentIface s : buffer)
		{
			if (s.getSeq_n() == n_ack)
			{
				found  = true;
				break;
			}
		}
		
		if (!found)
			throw new IllegalArgumentException("ACK recibido: " + n_ack
					+ " no coincide con ningún segmento enviado.");
		
		Iterator<SegmentIface> it = buffer.iterator();
		while (it.hasNext())
		{
			SegmentIface s = it.next();			
			if (s.getSeq_n() + nextSeqOffset(s) + numerationOffset() > n_ack)
				break;

			it.remove();
		}
		
		older_seq = n_ack;
	}
	
	protected abstract int numerationOffset();
	protected abstract int nextSeqOffset(SegmentIface s);
}
