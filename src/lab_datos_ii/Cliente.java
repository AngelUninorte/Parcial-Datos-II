package lab_datos_ii;

import Interfaz.ClientPanel;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cliente extends Conexion
{
    
    private String ip;
    private String name;
    
    private volatile boolean conectado;
    private volatile boolean procesando;
    
    private BuscadorPassword buscador;
    
    private ClientPanel panel;
    
    public Cliente() throws IOException {
        super("cliente");
        this.buscador = new BuscadorPassword(false);
        name = InetAddress.getLocalHost().getHostName();
        ip = InetAddress.getLocalHost().getHostAddress();         
    } 

    public Cliente(ClientPanel panel, String ip) throws IOException {
        super("cliente", ip);
        this.buscador = new BuscadorPassword(false);
        this.name = InetAddress.getLocalHost().getHostName();
        this.ip = InetAddress.getLocalHost().getHostAddress();
        this.panel = panel;
    }
    
    @Override
    public void run () {
        this.startClient();
    }
    
    public void startClient() //Método para iniciar el cliente
    {
        try
        {   
            //Flujo de datos hacia el servidor
            salidaServidor = new DataOutputStream(cs.getOutputStream());
            
            //Establecer Conexión
            salidaServidor.writeUTF("CONEXION\n");
            
            String mensaje = recibirMensaje();          
            System.out.println(mensaje);
            
            if (mensaje.equals(" 	CONEXION")) {
            
                //Enviar Información
                salidaServidor.writeUTF(name+";"+ip+";activo\n");
                this.conectado = true;
            }
            
            while (this.conectado) {
                mensaje = recibirMensaje();
                System.out.println("Confirmación de Conexión");
                System.out.println("|"+mensaje+"|");
                
                if (mensaje.equals(" CONFIRMAR_CONEXION")) {

                    //Enviar Información
                    salidaServidor.writeUTF("CONEXION\n");
                    this.panel.setText("Conexión Establecida con " + this.ip);
                }
                
                mensaje = mensaje.substring(2);
                System.out.println(mensaje.substring(1).equals("ACTIVAR"));
                if (mensaje.equals("ACTIVAR")) {
                    this.activarProceso();
                    salidaServidor.writeUTF("ACTIVADO\n");
                    this.panel.setText("En Proceso de Búsqueda de Contraseña");
                }
            }
            
            while (this.procesando) {
                //Recibe Entradas
                mensaje = this.recibirMensaje();
                String[] datos = mensaje.split(";");
                
                System.out.println(mensaje);
                System.out.println(datos.length);
                
                if (datos.length < 2) {
                    salidaServidor.writeUTF("ERROR\n");
                    continue;
                }
                
                //Procesa Entradas
                String[] resultado = this.buscador.recuperar(datos[1], datos[2]);
                
                //Envía las Salidas
                String salida = resultado[0]+";"+resultado[1]+";"+resultado[2];
                salidaServidor.writeUTF(";"+salida+";\n");
            }

        }
        catch (Exception e)
        {
            Logger.getLogger(HiloConexion.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public String recibirMensaje () throws IOException, ClassNotFoundException {
        System.out.println("Esperando Mensaje...");
        //Se obtiene el flujo entrante desde el cliente
        BufferedReader entrada = new BufferedReader(new InputStreamReader(cs.getInputStream()));
        
        while((mensajeServidor = entrada.readLine()) != null) //Mientras haya mensajes desde el cliente
            {
               return mensajeServidor;
            }
        
        return "ERROR";
    }
    
    public void cerrar () throws IOException {
        
        this.panel.setText("Conexión Finalizada");
        
        salidaServidor.writeUTF("CERRAR\n");
        
        cs.close();//Fin de la conexión
    }
    
    public void activarProceso () {
        this.conectado = false;
        this.procesando = true;
        System.out.println("ACTIVADO");
    }
    
    public boolean procesando () {
        return this.procesando;
    }
    
}