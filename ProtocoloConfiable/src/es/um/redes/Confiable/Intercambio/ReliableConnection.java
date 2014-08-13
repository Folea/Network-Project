package es.um.redes.Confiable.Intercambio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import es.um.redes.Confiable.Intercambio.data.Operacion;
import es.um.redes.Confiable.Intercambio.data.Segmento;
import es.um.redes.Confiable.Intercambio.util.RecvBufferGBN;
import es.um.redes.Confiable.Intercambio.util.SendBufferGBN;
import es.um.redes.Confiable.Medio.MediumPacket;
import es.um.redes.Confiable.Medio.MediumSocket;

/**
 * 
 * @author Folea Ilie Cristian
 *
 */
public class ReliableConnection implements ReliableConnectionIface {

	private PrintWriter log; 
	private int MSS; // datos del segmento
	private final int puerto = 8009; // el puerto por el cual se va a conectar
	private boolean conectado; // Variable que indica si hay una conexion establecida o no
	private MediumSocket socketConectado; // Socket activo
	private InetSocketAddress direccionConexion; 
	private RecvBufferGBN bufferRecepcion;
	private SendBufferGBN bufferEnvio;
	
	/**
	 * Constructor de ReliableConnectio.
	 * @param MSS Longitud maxima del segmento, para que se lo pueda indicar al hacer la conexion.
	 */
	public ReliableConnection(int MSS){
		this.conectado = false;
		this.bufferRecepcion = new RecvBufferGBN();
		this.bufferEnvio = new SendBufferGBN();
		this.MSS = MSS;
	}
	
	@Override
	public void connect(InetAddress to) throws IOException {
		log =  new PrintWriter("Cliente");
		socketConectado = new MediumSocket();
		direccionConexion = new InetSocketAddress(to, puerto);
	
		Segmento segmento = new Segmento(Operacion.CONEXION, bufferEnvio.getMaxSeq()+1, null, this.MSS);
		byte[] datos = segmento.toByteArray();
			
		MediumPacket paqueteAEnviar = new MediumPacket(datos, datos.length, direccionConexion);
		
		log.println("Envio " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
				+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS = " + segmento.getMSS());
		bufferEnvio.addSentSegment(segmento);
		
		socketConectado.send(paqueteAEnviar);
		MediumPacket paqueteRecibido = null;
		
		paqueteRecibido = null;
		while(paqueteRecibido == null){
			paqueteRecibido = socketConectado.receive();
		}
			
		byte[] datosRecibidos =  paqueteRecibido.getData();
		segmento.fromByteArray(datosRecibidos);
		
		if(!segmento.isCorrupto() && segmento.getOperacion() == Operacion.ACK_CONEXION){
			conectado = true;
			if(MSS > segmento.getMSS()){
				MSS = segmento.getMSS();
			}
			bufferEnvio.receivedACK(segmento.getAck_n());
		}
		log.println("Recibe " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
					+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS = " + segmento.getMSS());
	}
	
	@Override
	public void accept() throws IOException {
		log =  new PrintWriter("Servidor");
		socketConectado = new MediumSocket(puerto);
		MediumPacket paquete =  null;
		
		while(paquete == null){
			paquete = socketConectado.receive();
		}
		
		byte[] datosSegmento = paquete.getData();
		Segmento segmentoRecibido = new Segmento();
		segmentoRecibido.fromByteArray(datosSegmento);
		direccionConexion = paquete.getInetAddr();
		log.println("Recibe " + segmentoRecibido.getOperacion() + "; numero de segmento = " + segmentoRecibido.getSeq_n() 
				+ "; numero de ACK = " + segmentoRecibido.getAck_n() + "; numero de MSS = " + segmentoRecibido.getMSS());
		bufferRecepcion.addReceivedSegment(segmentoRecibido);
		
		if(!segmentoRecibido.isCorrupto()){
			if(segmentoRecibido.getOperacion() == Operacion.CONEXION){
				segmentoRecibido.setFlagACKConexion();
				segmentoRecibido.setAck_n(bufferRecepcion.getMaxReceivedInOrder());
				if(MSS > segmentoRecibido.getMSS()){
					this.MSS = segmentoRecibido.getMSS();
				} else {
					segmentoRecibido.setMSS(this.MSS);
				}
				conectado=true;
				byte[] datosEnviar = segmentoRecibido.toByteArray();
				paquete = new MediumPacket(datosEnviar, datosEnviar.length, direccionConexion);
			}
			log.println("Envio " + segmentoRecibido.getOperacion() + "; numero de segmento = " + segmentoRecibido.getSeq_n() 
					+ "; numero de ACK = " + segmentoRecibido.getAck_n() + "; numero de MSS = " + segmentoRecibido.getMSS());
			socketConectado.send(paquete); 
		}
	}

	/**
	 * Indica si esta conectado o no.
	 * @return true Si esta conectado.
	 * @return false Si no esta conectado.
	 */
	public boolean isConectado(){
		return conectado;
	}
	
	@Override
	public int getMSS() {
		return MSS;
	}

	/**
	 * Cambia la longitud maxima del segmento.
	 * @param MSS La nueva longitud del segmento.
	 */
	public void setMSS(int MSS){
		this.MSS = MSS;
	}
	
	@Override
	public void close() throws IOException {
		Segmento segmento = new Segmento(Operacion.DESCONEXION, bufferEnvio.getMaxSeq()+1, null, this.MSS);
		byte[] datos = segmento.toByteArray();
		MediumPacket paqueteAEnviar = new MediumPacket(datos, datos.length, direccionConexion);
		bufferEnvio.addSentSegment(segmento);
		socketConectado.send(paqueteAEnviar);
		log.println("Envio " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
				+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS= " + segmento.getMSS());
		
		MediumPacket paqueteRecibido = null;
		segmento = new Segmento();
		
		while(paqueteRecibido == null){
			paqueteRecibido = socketConectado.receive();
		}
		
		byte[] datosRecibidos =  paqueteRecibido.getData();
		segmento.fromByteArray(datosRecibidos);
		
		log.println("Recibe " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
				+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS = " + segmento.getMSS());

		if(!segmento.isCorrupto()){
			bufferEnvio.receivedACK(segmento.getAck_n());
		}
		log.close();
		conectado = false;
	}

	@Override
	public int getTimeoutValue() {
		return 0;
	}

	@Override
	public InetAddress getPeerAddress() {
		return direccionConexion.getAddress();
	}

	@Override
	public int write(byte[] buffer) throws IOException {	
		MediumPacket paqueteRecibido = null;
		Segmento segmento = new Segmento();
		int bytesEnviados = 0;
		
		if(buffer.length >(MSS-25)){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b = 0;
			while(b < buffer.length){
				baos.write(buffer[b]);
				b++;
				if(baos.size() == (MSS-25)){
					Segmento segAEnviar = new Segmento(Operacion.DATOS, bufferEnvio.getMaxSeq()+1, baos.toByteArray(), this.MSS);
					bytesEnviados += segAEnviar.toByteArray().length;
					MediumPacket paqueteAEnviar = new MediumPacket(segAEnviar.toByteArray(),
							segAEnviar.toByteArray().length, direccionConexion);
					bufferEnvio.addSentSegment(segAEnviar);
					log.println("Envio " + segAEnviar.getOperacion() + "; numero de segmento = " + segAEnviar.getSeq_n() 
							+ "; numero de ACK = " + segAEnviar.getAck_n() + "; numero de MSS= " + segAEnviar.getMSS());
					socketConectado.send(paqueteAEnviar);
					
					// ESPERO LA CONFIRMACION DEL PAQUETE QUE ACABO DE ENVIAR
					paqueteRecibido = new MediumPacket();
					paqueteRecibido = socketConectado.receive();

					if(paqueteRecibido != null){
						byte[] datosRecibidos =  paqueteRecibido.getData();
						segmento.fromByteArray(datosRecibidos);
						
						if(!segmento.isCorrupto()){
							bufferEnvio.receivedACK(segmento.getAck_n());
						}
						
						log.println("Recibe " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
								+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS = " + segmento.getMSS());
					}
					baos = new ByteArrayOutputStream();
				}
			}
			if(baos.size() != 0){
				Segmento segAEnviar = new Segmento(Operacion.DATOS, bufferEnvio.getMaxSeq()+1, baos.toByteArray(), this.MSS);
				bytesEnviados += segAEnviar.toByteArray().length;
				MediumPacket paqueteAEnviar = new MediumPacket(segAEnviar.toByteArray(),
						segAEnviar.toByteArray().length, direccionConexion);
				log.println("Envio " + segAEnviar.getOperacion() + "; numero de segmento = " + segAEnviar.getSeq_n() 
						+ "; numero de ACK = " + segAEnviar.getAck_n() + "; numero de MSS= " + segAEnviar.getMSS());
				bufferEnvio.addSentSegment(segAEnviar);
				socketConectado.send(paqueteAEnviar);
				
				paqueteRecibido = new MediumPacket();
				paqueteRecibido = socketConectado.receive();
				
				if(paqueteRecibido != null){
					byte[] datosRecibidos =  paqueteRecibido.getData();
					segmento.fromByteArray(datosRecibidos);
					
					if(!segmento.isCorrupto()){
						bufferEnvio.receivedACK(segmento.getAck_n());
					}
					
					log.println("Recibe " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
							+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS = " + segmento.getMSS());
				}
			}
		} else {
			Segmento segAEnviar = new Segmento(Operacion.DATOS, bufferEnvio.getMaxSeq()+1, buffer, this.MSS);
			bytesEnviados = segAEnviar.toByteArray().length;
			MediumPacket paqueteAEnviar = new MediumPacket(segAEnviar.toByteArray(), bytesEnviados, direccionConexion);
			bufferEnvio.addSentSegment(segAEnviar);

			log.println("Envio " + segAEnviar.getOperacion() + "; numero de segmento = " + segAEnviar.getSeq_n() 
					+ "; numero de ACK = " + segAEnviar.getAck_n() + "; numero de MSS= " + segAEnviar.getMSS());
			socketConectado.send(paqueteAEnviar);
			
			paqueteRecibido = new MediumPacket();
			paqueteRecibido = socketConectado.receive();
			
			if(paqueteRecibido != null){
				byte[] datosRecibidos =  paqueteRecibido.getData();
				segmento.fromByteArray(datosRecibidos);
				
				if(!segmento.isCorrupto()){
					bufferEnvio.receivedACK(segmento.getAck_n());
				}
				log.println("Recibe " + segmento.getOperacion() + "; numero de segmento = " + segmento.getSeq_n() 
						+ "; numero de ACK = " + segmento.getAck_n() + "; numero de MSS = " + segmento.getMSS());
			}
			
			/*while(paqueteRecibido == null){
				paqueteRecibido = socketConectado.receive();
			}*/
			
			
		}
		
		return bytesEnviados;
	}

	@Override
	public byte[] read() throws IOException {
		
		MediumPacket paqueteRecibido = null;
		
		while(paqueteRecibido == null){
			paqueteRecibido = socketConectado.receive();
		}
		
		byte[] datosRecibidos = paqueteRecibido.getData();
		Segmento segRecibido = new Segmento();
		segRecibido.fromByteArray(datosRecibidos);
		byte[] datosOriginales = segRecibido.getData();
		log.println("Recibe " + segRecibido.getOperacion() + "; numero de segmento = " + segRecibido.getSeq_n() 
				+ "; numero de ACK = " + segRecibido.getAck_n() + "; numero de MSS = " + segRecibido.getMSS());
		
		if(!segRecibido.isCorrupto()){
			bufferRecepcion.addReceivedSegment(segRecibido);
			if(segRecibido.getOperacion() == Operacion.DATOS){
				segRecibido.setFlagsACKDatos();
				segRecibido.setAck_n(bufferRecepcion.getMaxReceivedInOrder());
			} else if(segRecibido.getOperacion() == Operacion.DESCONEXION){
				this.conectado = false;
				segRecibido.setFlagACKDesconexion();
				log.close();
			}
			
			paqueteRecibido =  new MediumPacket(segRecibido.toByteArray(), segRecibido.toByteArray().length,direccionConexion);
			log.println("Envio " + segRecibido.getOperacion() + "; numero de segmento = " + segRecibido.getSeq_n() 
					+ "; numero de ACK = " + segRecibido.getAck_n() + "; numero de MSS= " + segRecibido.getMSS());
			socketConectado.send(paqueteRecibido);
		}
		return datosOriginales;
	}
}
