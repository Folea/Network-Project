package es.um.redes.Confiable.Intercambio.data;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
/**
 * 
 * @author Folea Ilie Cristian
 *
 */
public class Segmento implements SegmentIface {

	private int datosCabecera = 25;
	private int MSS;
	private int numeroSecuencia;
	private int numeroConfirmacion;
	private int longitudDatos;
	private byte[] datos;
	private long checksum;
	private byte flags;
	
	/**
	 * Constructor vacio de segmento. Este constructor se usara para cuando se quiere 
	 * crear un segmento vacio para que posteriormente se le asignen los datos recibidos
	 * por el paquete
	 */
	public Segmento(){
		
	}
	
	/**
	 * Constructor con varios parametros. Se actualizan los flags en funcion de la operacion
	 * que se le pasa como parametro y se le asigna los otros datos recibidos a los campos del
	 * segmento.
	 * @param op Se utiliza para indicar el tipo de segmento que se va a crear.
	 * @param numeroSecuencia Se utiliza para indicar el numero de secuencia del segmento.
	 * @param datos Los datos que va a contener el segmento.
	 * @param MSS El numero maximo de datos que puede enviar el semento, se utiliza especialmente
	 * para la conexión para que el servidor sepa el MSS del emisor y elija el menor.
	 */
	public Segmento (Operacion op, int numeroSecuencia, byte[] datos, int MSS){		
		this.numeroSecuencia = numeroSecuencia;
		this.MSS =  MSS;
		this.longitudDatos =  0;
		if(op == Operacion.CONEXION){
			this.setFlagConexion();
		}
		else if(op == Operacion.ACK_CONEXION){
			this.setFlagACKConexion();
		}
		else if(op == Operacion.DESCONEXION){
			this.setFlagDesconexion();
		}
		else if(op == Operacion.ACK_DESCONEXION){
			this.setFlagACKDesconexion();
		}
		else {
			if(datos != null){
				this.datos = datos;
				longitudDatos = datos.length;
			}
			if(op == Operacion.DATOS){
				this.setFlagDatos();
			}
			else{
				this.setFlagsACKDatos();
			}
		}
	}

	@Override
	public int getSeq_n() {
		return numeroSecuencia;
	}

	/**
	 * Se utiliza para asignar el numero de ACK que se le va a asignar al segmento
	 * @param nConfirmacion
	 */
	public void setAck_n(int nConfirmacion){
		this.numeroConfirmacion = nConfirmacion;
	}
	
	@Override
	public int getAck_n() {
		return numeroConfirmacion;
	}

	@Override
	public int getData_length() {
		return longitudDatos;
	}

	@Override
	public byte[] getData() {
		return datos;
	}
	
	/**
	 * Crea el flag y pone sus bits correspondientes a uno utilizando el desplazamiento.
	 * @param desplazamiento El numero de bits que se van a desplazar a la izquierda.
	 */
	private void setFlagsSegmento(int desplazamiento){
		this.flags = (byte) (flags |(1 << desplazamiento));
	}

	/**
	 * Crea el flag de conexion.
	 */
	public void setFlagConexion(){
		setFlagsSegmento(6);
	}
	
	/**
	 * Crea el flag de ACK para la conexion.
	 */
	public void setFlagACKConexion(){
		setFlagConexion();
		setFlagsSegmento(3);
	}
	
	/**
	 * Devuelve el numero MSS.
	 * @return Numero MSS del segmento.
	 */
	public int getMSS(){
		return MSS;
	}
	
	/**
	 * Asigna el numero recibido al MSS.
	 * @param MSS El numero que se le va a asignar al MSS del segmento.
	 */
	public void setMSS(int MSS){
		this.MSS = MSS;
	}
	
	/**
	 * Crea el flag de desconexion.
	 */
	public void setFlagDesconexion(){
		setFlagsSegmento(5);
	}
	
	/**
	 * Crea el flag de ACK desconexion
	 */
	public void setFlagACKDesconexion(){
		setFlagDesconexion();
		setFlagsSegmento(3);
	}
	
	/**
	 * Crea el flag de datos.
	 */
	public void setFlagDatos(){
		setFlagsSegmento(4);
	}
	
	/**
	 * Crea el flag de ACK datos.
	 */
	public void setFlagsACKDatos(){
		setFlagDatos();
		setFlagsSegmento(3);
	}
	
	/**
	 * Se utiliza para obtener el enumerado de la operación basandose en los flags.
	 * @return El enumerado correspondiente a los flags.
	 */
	public Operacion getOperacion(){
		switch(flags){
		case 64: 
			return Operacion.CONEXION;	
		case 72: 
			return Operacion.ACK_CONEXION;
		case 32: 
			return Operacion.DESCONEXION;
		case 40: 
			return Operacion.ACK_DESCONEXION;
		case 16: 
			return Operacion.DATOS;
		case 24: 
			return Operacion.ACK_DATOS;
		default: 
			return null;
		}
	}
	
	@Override
	public byte[] toByteArray(){
		ByteBuffer byteBuffer = ByteBuffer.allocate((datosCabecera-8)+longitudDatos);
		
		Checksum chck = new CRC32();
		
		byteBuffer.put(this.flags);
		byteBuffer.putInt(this.numeroSecuencia);
		byteBuffer.putInt(numeroConfirmacion);
		byteBuffer.putInt(longitudDatos);
		byteBuffer.putInt(this.MSS);
		if(datos != null){
			byteBuffer.put(datos);
		}
		chck.update(byteBuffer.array(), 0, byteBuffer.array().length);
		checksum = chck.getValue();
		
		byteBuffer = ByteBuffer.allocate(datosCabecera+longitudDatos);
		byteBuffer.put(this.flags);
		byteBuffer.putInt(this.numeroSecuencia);
		byteBuffer.putInt(numeroConfirmacion);
		byteBuffer.putInt(longitudDatos);
		byteBuffer.putInt(this.MSS);
		byteBuffer.putLong(checksum);
		if(datos != null){
			byteBuffer.put(datos);
		}
		
		return byteBuffer.array();
	}

	@Override
	public void fromByteArray(byte[] buf) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(buf);

		this.flags = byteBuffer.get(0);
		this.numeroSecuencia = byteBuffer.getInt(1);
		this.numeroConfirmacion = byteBuffer.getInt(5);
		this.longitudDatos = byteBuffer.getInt(9);
		this.MSS = byteBuffer.getInt(13);
		this.checksum = byteBuffer.getLong(17);

		byte[] extraccionDatos = new byte[longitudDatos];
		
		for(int i = 0; i < extraccionDatos.length; i++){
			extraccionDatos[i] = byteBuffer.get(i+datosCabecera);
		}
		this.datos = extraccionDatos;
	}
	
	/**
	 * Comprueba si el checksum de los datos recibidos por el segmento, sin el campo del checksum,
	 * es igual al checksum enviado. 
	 * @return true Si los checksums son distintos.
	 * @return false Si los checksums son iguales.
	 */
	public boolean isCorrupto(){
		ByteBuffer byteBuffer = ByteBuffer.allocate((datosCabecera-8)+longitudDatos);
		
		Checksum chck = new CRC32();
		
		byteBuffer.put(this.flags);
		byteBuffer.putInt(this.numeroSecuencia);
		byteBuffer.putInt(numeroConfirmacion);
		byteBuffer.putInt(longitudDatos);
		byteBuffer.putInt(MSS);
		if(datos != null){
			byteBuffer.put(datos);
		}
		chck.update(byteBuffer.array(), 0, byteBuffer.array().length);
		
		return checksum != chck.getValue();
	}
}
