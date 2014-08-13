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
	 * @return El n�mero de secuencia del segmento m�s antiguo en el buffer
	 *            (el primero que se a�adi� al buffer y que no ha sido confirmado).
	 *            Inicialmente es 0 para TCP (el siguiente esperado), -1 para GoBackN.
	 */
	public int getOlderSeq()
	{
		return older_seq;
	}
	
	/**
	 * @return El tope del buffer de env�o.
	 *            En el caso de TCP, ser� el n�mero de secuencia siguiente que entrar�a en el buffer
	 *            En el caso de GoBackN ser� el n�mero del �ltimo segmento a�adido al buffer (el m�s reciente)
	 */
	public int getMaxSeq()
	{
		return max_seq;
	}
	
	/**
	 * @return La diferencia que queda en el buffer. 0 si no queda ninguna. 
	 *            En el caso de TCP ser� el n�mero de bytes que quedan por confirmar.
	 *            En el caso de GoBackN ser� el n�mero de segmentos por confirmar.
	 */
	public int stillPending()
	{
		return getMaxSeq() - getOlderSeq();
	}
	
	/**
	 * Retorna una vista de s�lo lectura de la lista de segmentos que hay almacenados en el buffer.
	 * @return la vista de la lista de segmentos.
	 */
	public List<SegmentIface> segmentsPending()
	{
		return Collections.unmodifiableList(buffer);
	}
	
	/**
	 * @return El segmento m�s antiguo que hay almacenado en el buffer de env�o. Este 
	 *            segmento se puede usar para reenviar el segmento m�s antiguo.
	 * @throws IndexOutOfBoundsException
	 */
	public SegmentIface getOlderSegment() throws IndexOutOfBoundsException
	{
		return buffer.get(0);
	}
	
	/**
	 * A�ade un nuevo segmento enviado al buffer de env�o. Si el segmento no es el esperado,
	 * se lanza la excepci�n {@link IllegalArgumentException}. Actualiza el contador getMaxSeq()
	 * acorde con el nuevo segmento. 
	 * @param seg El segmento a a�adir.
	 * @throws IllegalArgumentException si el segmento no es el esperado
	 */
	public void addSentSegment(SegmentIface seg) throws IllegalArgumentException
	{
		if (seg.getSeq_n() + numerationOffset() != max_seq)
			throw new IllegalArgumentException("El segmento a�adido al buffer de enviados no est� en orden.");
		buffer.add(seg);
		max_seq += nextSeqOffset(seg);
	}
	
	/**
	 * Indica al buffer que se ha recibido un segmento con n�mero de ACK n_ack. Se actualiza el contador
	 * retornado por getOlderSeq() para reflejar este ACK.
	 * @param n_ack El n�mero de ACK.
	 * @throws IndexOutOfBoundsException Si el n�mero de ACK es mayor que el m�ximo del buffer getMaxSeq() 
	 * @throws IllegalArgumentException Si el n�mero de ACK no coincide con ning�n segmento
	 *            almacenado en el buffer de env�o 
	 */
	public void receivedACK(int n_ack) throws IndexOutOfBoundsException, IllegalArgumentException
	{
		if (n_ack > getMaxSeq())
			throw new IndexOutOfBoundsException("ACK recibido: " + n_ack + " est� fuera de rango.");
		
		// Primera comprobaci�n r�pida, el buffer est� vac�o
		if (n_ack + numerationOffset() < getOlderSeq() || 0 == stillPending())
			return;
		
		// �Todos confirmados?
		// NOTA: Esto hay que hacerlo para que funcione bien este c�digo tanto en GBN como en TCP
		if (n_ack == getMaxSeq())
		{
			older_seq = max_seq;
			buffer.clear();
			return;
		}
		
		// Para hacer la comprobaci�n de IllegalArgumentException debemos buscar en la lista
		// para ver si hay un segmento que tiene exactamente ese n�mero de secuencia
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
					+ " no coincide con ning�n segmento enviado.");
		
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
