package es.um.redes.Confiable.Medio;

import java.io.IOException;

public interface MediumSocketIface {

	public abstract void send(MediumPacket seg) throws IOException;

	// Ojo, siempre no bloqueante
	public abstract MediumPacket receive() throws IOException;

}