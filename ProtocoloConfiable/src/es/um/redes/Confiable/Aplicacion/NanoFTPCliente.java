package es.um.redes.Confiable.Aplicacion;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import es.um.redes.Confiable.Intercambio.ReliableConnection;

/**
 * La clase que representa el cliente.
 * @author Folea Ilie Cristian
 *
 */

public class NanoFTPCliente {

	private static final String CRLF = "\r\n";
	private static ReliableConnection conexion;

	/**
	 * Este metodo se conecta al servidor y despues espera que se le pase un comando para que
	 * envie algo al servidor o para que reciba algo del servidor.Si se le pasa un put y 
	 * un nombre de fichero, comprueba que el fichero este en el directorio que esta predefinido
	 * y en el caso de que este se lo envia al servido. Si se le hace un get, envia una petición
	 * al servidor para que le envie el fichero con el nombre que se le ha pasado.
	 * @throws UnknownHostException Excepción que se lanza si falla el connect.
	 * @throws IOException Excepción que se lanza si falla el connect.
	 */
	private void startConversation(String IP) throws UnknownHostException, IOException {
		conexion = new ReliableConnection(700);

		try {
			conexion.connect(InetAddress.getByName(IP));
			// Envía un campo
			String cadena = "";
			Scanner dato = new Scanner(System.in);
			while (!cadena.equals("exit")) {
				cadena = "";
				System.out.println("Introduzca un comando:");
				cadena = dato.nextLine();
				String[] partes = cadena.split(" ");

				if (partes[0].equals("put")) {
					this.sendField(conexion, "PUT");
					this.sendField(conexion, partes[1]);
					File f = new File(
							"C:/Users/IlieCristian/Desktop/Ficheros/Envio",
							partes[1]);
					if (f.exists()) {
						FileInputStream fis = new FileInputStream(f);
						// leemos los datos del fichero
						byte data[] = new byte[(int) f.length()];
						fis.read(data);
						fis.close();

						String longitud = Long.toString(data.length);
						sendField(conexion, longitud);
						sendData(conexion, data);
						
						cadena = readField(conexion);
						if (cadena.equals("ERROR")) {
							System.out.println("El fichero " + readField(conexion)
									+ " no se ha enviado corectamente o ya esta en el servidor");
						} else {
							System.out.println("El fichero " + readField(conexion)
									+ " se ha enviado corectamente");
						}

					} else {
						System.out.println("El fichero no existe");
					}
				} else if (partes[0].equals("get")) {
					sendField(conexion, "GET");
					sendField(conexion, partes[1]);
					
					cadena = readField(conexion);
					if(cadena.equals("FILE")){
						String nombreFichero = readField(conexion);
	    				int longitudFichero = Integer.parseInt(readField(conexion));
	    				
	    				ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    				while(baos.size() < longitudFichero){
	    					baos.write(readData());
	    				}
	    				File fichero = new File(
	    						"C:/Users/IlieCristian/Desktop/Ficheros/Envio",
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
					} else if(cadena.equals("ERROR")){
						String nombreFichero = readField(conexion);
						System.out.println("Se ha producido un error al pedir el fichero con nombre" 
								+ nombreFichero);
					}
				}
			}
			if (cadena.equals("exit")) {
				sendField(conexion, "EXIT");
			}

		} catch (IOException e) {
			System.err
					.println("¡La comunicación no se pudo realizar correctamente!");
			e.printStackTrace();
			
		} finally {
			try {
				conexion.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * Metodo para enviar una cadena de caracteres. Se le añade CLRF al final de la cadena.
	 * @param rc Canal por el que se va a enviar.
	 * @param st Cadena que se va a enviar.
	 * @throws IOException Excepción que se lanza en el caso de que falle el envio.
	 */
	private void sendField(ReliableConnection rc, String st) throws IOException {
		rc.write((st+CRLF).getBytes());
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
	 * Metodo para leer una cadena de caracteres que le llega por un canal.
	 * @param rc Canal por el cual se espera que se lea.
	 * @return Cadena de caracteres leida.
	 * @throws IOException Excepción que se lanza en el caso de que falle la recepción.
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

	public static void main(String[] args) {
		NanoFTPCliente cliente = new NanoFTPCliente();
		System.out.println("Introduce la direccion IP del servidor :");
		Scanner dato = new Scanner(System.in);
		String IP = dato.nextLine();
		try {
			cliente.startConversation(IP);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
