/**
 * 
 */
package es.um.redes.Confiable.Intercambio.util;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import es.um.redes.Confiable.Intercambio.data.SegmentIface;

/**
 * @author dsevilla
 *
 */
public final class RecvBufferGBN extends RecvBufferBase
{
	private List<SegmentIface> buffer;
	private int max_in_order_packets;
	private int available_start_packets;
	
	public RecvBufferGBN()
	{
		super();
		available_start_packets = -1;  // En GBN inicialmente no se ha recibido ningún segmento
		max_in_order_packets = -1;
		available_start = 0;
		max_in_order = 0;
		buffer = new LinkedList<SegmentIface>();
	}

	public RecvBufferGBN(int seq_init)
	{
		super();
		available_start_packets = seq_init;  // En GBN inicialmente no se ha recibido ningún segmento
		max_in_order_packets = seq_init;
		available_start = 0;
		max_in_order = 0;
		buffer = new LinkedList<SegmentIface>();
	}

	// Consumir buffer de recepción si hay datos recibidos en orden
	/* (non-Javadoc)
	 * @see es.um.redes.Confiable.Intercambio.util.RecvBufferIface#consume_received()
	 */
	@Override
	public int obtainDataInOrder(byte[] buf)
	{
		int left = buf.length;
		int bufpos = 0;
		
		while (left != 0 && available_start + bufpos != max_in_order)
		{
			int remaining = in_tmp.remaining();
			
			if (remaining >= left)
			{
				in_tmp.get(buf, bufpos, left);
				bufpos += left;
				left = 0;
			} else
			{
				if (remaining != 0)
				{
					// Copy what's remaining in in_tmp, and load another one.
					in_tmp.get(buf, bufpos, remaining);
					bufpos += remaining;
					left -= remaining;
				}

				// Read next element
				if (buffer.size() == 0)
					in_tmp = ByteBuffer.allocate(0);
				else 
				{
					SegmentIface s = buffer.get(0);
					in_tmp = ByteBuffer.wrap(s.getData());
					buffer.remove(0);
					available_start_packets++; // Normal increment
				}
			}
		}

		// Lo que se ha añadido
		available_start += bufpos;
		return bufpos;
	}

	/* (non-Javadoc)
	 * @see es.um.redes.Confiable.Intercambio.util.RecvBufferIface#add_received_segment(es.um.redes.Confiable.Intercambio.data.SegmentIface)
	 */
	@Override
	public void addReceivedSegment(SegmentIface s)
	{
		if (s.getSeq_n() < available_start_packets)
			return;
		
		// Ignore if not in sequence
		if (1 + max_in_order_packets != s.getSeq_n())
			return; 

		// Add it to the end, the only possible case
		buffer.add(s);
		max_in_order_packets++;
		max_in_order += s.getData_length();
	}
	
	@Override
	public int getAvailableDataLength() {
		return max_in_order - available_start; 
	}
	
	@Override
	public int getMaxReceivedInOrder() {
		return max_in_order_packets;
	}
	
	@Override
	public int getAvailableStart() {
		return available_start_packets;
	}
	
//	@Override
//	public List<SegmentIface> getAllSegmentsInBuffer() {
//		return Collections.unmodifiableList(buffer);
//	}
}
