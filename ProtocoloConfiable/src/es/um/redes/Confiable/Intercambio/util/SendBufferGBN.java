/**
 * 
 */
package es.um.redes.Confiable.Intercambio.util;

import es.um.redes.Confiable.Intercambio.data.SegmentIface;

/**
 * @author dsevilla
 *
 */
public class SendBufferGBN extends SendBufferBase 
{
	
	public SendBufferGBN() 
	{
		super();
	}

	public SendBufferGBN(int seq_init) 
	{
		super(seq_init);
	}

	/* (non-Javadoc)
	 * @see es.um.redes.Confiable.Intercambio.util.SendBufferBase#numerationOffset()
	 */
	@Override
	protected int numerationOffset() {
		return -1;
	}

	@Override
	protected int nextSeqOffset(SegmentIface s)
	{
		return 1;
	}

}
