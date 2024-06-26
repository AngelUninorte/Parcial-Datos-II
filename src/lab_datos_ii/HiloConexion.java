package lab_datos_ii;

import Interfaz.HostPanel;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloConexion extends Thread {
    private Socket cs;
    private final String nombre;
    private final String ip;
    private boolean estado;
    private String mensajeServidor;
    private Servidor servidor;
    private volatile boolean procesando_password = false;
    private DataOutputStream salidaCliente;
    
    private String user;
    private Queue<String> passwords;
    private ArrayList<String[]> resultados;
    private volatile boolean mensaje_activacion = false;
    private volatile boolean finalizado = false;
    
    private long cont;
    
    public HiloConexion (Socket cs, String nombre, String ip, boolean estado, Servidor servidor) {
        this.cs = cs;
        this.nombre = nombre;
        this.ip = ip;
        this.estado = estado;
        this.servidor = servidor;
        this.passwords = new LinkedList<>();
        System.out.println("Conexión Establecida con [" + nombre + "]");
    }
    
    public void asignarPassword (String user, ArrayList<String> passwords) {
        this.user = user;
        this.passwords = new LinkedList<>();
        this.passwords.addAll(passwords);
        this.resultados = new ArrayList<>();
    }
    
    public void activarActividad () {
        this.mensaje_activacion = true;
    }
    
    public void desactivarActividad () {
        this.procesando_password = false;
    }
    
    @Override
    public void run () {
        
        try {
            salidaCliente = new DataOutputStream(cs.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(HiloConexion.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Inhabilitado el envio de mensajes");
        }        
        
        try {            
            while (estado) {                 
                 if (this.mensaje_activacion) {
                        System.out.println("Enviado mensaje de Activación a " + this.nombre);
                        this.enviarMensaje("ACTIVAR");
                        
                        mensajeServidor = recibirMensaje();
                        
                        System.out.println(mensajeServidor);
                        
                        if (mensajeServidor.equals(" 	ACTIVADO")) {
                            this.mensaje_activacion = false;
                            this.procesando_password = true;
                            this.servidor.actualizarEstadosInterfaz();
                        }
                 }
                
                if (this.procesando_password) { 
                    //Enviar Info contraseña
                    String password = this.passwords.poll();
                    System.out.println(password);
                    System.out.println("n = "+this.passwords.size());
                    
                    this.enviarMensaje(";" + this.user + ";" + password + ";");
                    
                    mensajeServidor = this.recibirMensaje();
                    
                    //Tarea finalizada
                    if (mensajeServidor.equals(" CERRAR")) {
                            System.out.println("FINALIZADO");
                            this.finalizado = true;
                            servidor.actualizarEstadosInterfaz();
                            return;
                    }
                    
                    if (mensajeServidor.equals(" ERROR")) {
                        System.out.println("ERROR EN EL ENVIO DE DATOS");
                        continue;
                    }
                    
                    //Esperar a recibir el resultado
                    String[] datos = mensajeServidor.split(";");
                    
                    if (datos.length < 4)
                        return;
                    
                    String[] resultado = new String[] {datos[1], datos[2], datos[3]};
                    this.resultados.add(resultado);
                    
                    //Enviar Resultado a Principal
                    servidor.validarResultado(resultado);
                    
                } else {
                    // Confirmar Conexión cada 5 segundos
                    if (this.cont + 5000 < System.currentTimeMillis()) {
                        this.enviarMensaje("CONFIRMAR_CONEXION");
                        
                        mensajeServidor = recibirMensaje();

                        if (mensajeServidor.equals(" CERRAR")) {
                            this.estado = false;
                            servidor.actualizarEstadosInterfaz();
                        }
                        
                        this.cont = System.currentTimeMillis();
                    }
                }   
            }
            
        } catch (IOException ex) {
            Logger.getLogger(HiloConexion.class.getName()).log(Level.SEVERE, null, ex);
            this.estado = false;
            servidor.actualizarEstadosInterfaz();
        }
        
        System.out.println("Conexión Finalizada con [" + nombre + "]");
    }
    
    public String recibirMensaje () throws IOException {
        //Se obtiene el flujo entrante desde el cliente
        BufferedReader entrada = new BufferedReader(new InputStreamReader(cs.getInputStream()));
        System.out.println("Esperando mensaje de [" + nombre + "]");
        while((mensajeServidor = entrada.readLine()) != null) //Mientras haya mensajes desde el cliente
            {
                System.out.println("["+nombre+"]:" + mensajeServidor);
               return mensajeServidor;
            }
        
        return "ERROR";
    }
    
    public void enviarMensaje (String mensaje) throws IOException {
        salidaCliente.writeUTF(mensaje+"\n");
    }
    
    public String obtenerNombre () {
        return this.nombre;
    }
    
    public String obtenerIp () {
        return this.ip;
    }
    
    public String obtenerEstado () {
        if (this.finalizado)
            return "Finalizado";
        
        if (this.estado)
            if (this.procesando_password)
                return "En funcionamiento";
            else    
                return "Activo";
        
        return "Desactivado";
    }
    
}
