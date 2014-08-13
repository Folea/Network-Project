package es.um.redes.Confiable.Aplicacion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import es.um.redes.Confiable.Intercambio.ReliableConnection;

/**
 * Esta clase representa la parte del servidor.
 * 
 * @author Folea Ilie Cristian
 *
 */
public class NanoFTPServer {

    private static ReliableConnection conexion; 
	private static final String CRLF = "\r\n";

	/**
	 * Constructor del servidor
	 */
    private NanoFTPServer(){
    	conexion = new ReliableConnection(512);
    }

    /**
     * El servidor empieza a recibir. Espera que le llegue una petición de conexión, la accepta
     * y luego espera a que le lleguen mas datos. En el caso de que le llegue un put espera a que
     * le llegue el nombre de fichero, longitud y los datos para que lo cree. En el caso de que 
     * reciba un get, busca a ver si existe el fichero cuyo nombre se la ha pasado y en el caso
     * de que exista se lo envia al cliente.
     * @throws IOException
     */
    public void startRecive() throws IOException{
    	conexion.accept();
    	try{
    		while(conexion.isConectado()){
    			String comando = "";
    			comando = readField(conexion);

    			if(comando.equals("PUT")){
    				String nombreFichero = readField(conexion);
    				Long longitudFichero = Long.parseLong(readField(conexion));
    				
    				ByteArrayOutputStream baos = new ByteArrayOutputStream();
    				while(baos.size() < longitudFichero){
    					baos.write(readData());
    				}
    				File fichero = new File(
    						"C:/Users/IlieCristian/Desktop/Ficheros/Recepcion",
    						nombreFichero);
    				if (fichero.createNewFile()) {
    					FileOutputStream fos = new FileOutputStream(fichero);
    					fos.write(baos.toByteArray());
    					fos.close();
    					sendField(conexion,"OK");
    					sendField(conexion, nombreFichero);
    				} else {
    					sendField(conexion, "ERROR");
    					sendField(conexion,(nombreFichero));
    				}
    			}
    			else if(comando.equals("GET")){
    				String nombreFichero = readField(conexion);
    				
    				File f = new File(
							"C:/Users/IlieCristian/Desktop/Ficheros/Recepcion",
							nombreFichero);
					if (f.exists()) {
						FileInputStream fis = new FileInputStream(f);
						// leemos los datos del fichero
						byte data[] = new byte[(int) f.length()];
						fis.read(data);
						fis.close();
						
						sendField(conexion, "FILE");
						sendField(conexion, nombreFichero);
						sendField(conexion, Integer.toString(data.length));
						sendData(conexion,data);
					} else {
						sendField(conexion, "ERROR");
						sendField(conexion, nombreFichero);
					}
    			}
    			else if(comando.equals("EXIT")){
    				conexion.read();
    			}
    		}
    	} finally{
    	}
    }
	
    /**
	 * Metodo para enviar una cadena de caracteres. Se le añade CLRF al final de la cadena.
	 * @param rc Canal por el que se va a enviar.
	 * @param st Cadena que se va a enviar.
	 * @throws IOException Excepción que se lanza en el caso de que falle el envio.
	 */
	private void sendField(ReliableConnection rc, String st) throws IOException {
		rc.write((st + CRLF).getBytes());
	}
    
	/**
	 * Metodo para enviar datos por un canal.
	 * @param rc Canal por el que se va a enviar.
	 * @param datos Datos que se van a enviar
	 * @throws IOException Excepción que se lanza en el caso de que falle la recepción.
	 */
	private void sendData(ReliableConnection rc, byte[] datos)
			throws IOException {
		rc.write(datos);
	}
	
	/**
	 * Metodo para leer del canal.
	 * @return Devuelve el array de bytes leido.
	 * @throws IOException Excepción que se lanza en el caso de que falle el envio.
	 */
	private byte[] readData() throws IOException{
		return conexion.read();
	}
	
	/**
	 * Metodo para enviar una cadena de caracteres. Se le añade CLRF al final de la cadena.
	 * @param rc Canal por el que se va a enviar.
	 * @param st Cadena que se va a enviar.
	 * @throws IOException Excepción que se lanza en el caso de que falle el envio.
	 */
	private String readField(ReliableConnection rc) throws IOException {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte datos[] = rc.read();
		int b = datos.length;
		int i = 0;
		// Leemos, mientras que no lleguemos al final
		while(i<b){
			baos.write(datos[i]);
			i++;
		}

		// Extraer los bytes leídos
		byte[] bytes_leidos = baos.toByteArray();

		// Si no se ha leído nada, fin del Stream, retornar null
		if (bytes_leidos.length == 0) {
			return null;
		}

		int nbytes_a_copiar = bytes_leidos.length;
		if (bytes_leidos[nbytes_a_copiar - 1] == '\n') {
			nbytes_a_copiar--;
			if (nbytes_a_copiar > 0
					&& bytes_leidos[nbytes_a_copiar - 1] == '\r')
				nbytes_a_copiar--;
		}

		// Retornar la cadena de caracteres sin el \r\n final
		return new String(bytes_leidos, 0, nbytes_a_copiar);

	}

	public static void main(String[] args) throws IOException
    {
	 NanoFTPServer server =  new NanoFTPServer();
	 server.startRecive();
    }
}
