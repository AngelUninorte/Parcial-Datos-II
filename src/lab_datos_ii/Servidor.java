package lab_datos_ii;

import Interfaz.HostPanel;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public class Servidor extends Conexion 
{
    
    private ArrayList<HiloConexion> conexiones;
    private volatile boolean esperando = false;
    private HostPanel panel;
    private BuscadorPassword buscador;
    
    public Servidor(HostPanel panel) throws IOException{
        super("servidor");
        this.conexiones = new ArrayList<>();
        this.panel = panel;
        this.buscador = new BuscadorPassword(true);
    } //Se usa el constructor para servidor de Conexion
    
    @Override
    public void run () {
        this.startServer();
    }
    
    public void startServer()//Método para iniciar el servidor
    {
        this.esperando = true; //Habilita espera de conexiones
        
        try
        {
            System.out.println("Esperando..."); //Esperando conexión
            
            while (esperando) {
                cs = ss.accept(); //Accept comienza el socket y espera una conexión desde un cliente
                
                System.out.println("Cliente Conectado");
                
                //Se obtiene el flujo de salida del cliente para enviarle mensajes
                salidaCliente = new DataOutputStream(cs.getOutputStream());

                //Validación del establecimiento de una conexión
                String mensaje = recibirMensaje();
                
                if (!mensaje.equals(" 	CONEXION")) {
                    System.out.println("Conexión Interrumpida");
                    continue;
                }
                
                //Se le envía un mensaje al cliente usando su flujo de salida
                salidaCliente.writeUTF("CONEXION\n");
                
                //Obtener información del Computador Cliente
                mensaje = recibirMensaje();
                System.out.println(mensaje);
                String[] datos = mensaje.split(";");
                
                HiloConexion hilo = new HiloConexion (cs, datos[0], datos[1], true, this);
                hilo.start();
                
                this.conexiones.add(hilo);
                
                panel.refrescarPantalla();
            }

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    public boolean usuarioValido (String user) {
        return this.buscador.usuarioValido(user);
    }
    
    public void iniciarBusqueda (String user) {
        
        if (this.conexiones.size() == 0)
            return;
        
        //Dividir cargas de trabajo
        int n = this.buscador.getPasswords().size()/this.conexiones.size();
        System.out.println("Cantidad de paswword por PC: "+n);
        ArrayList<ArrayList<String>> conjuntos = new ArrayList<>();
        
        //Asignar cargas de trabajo
        for (int i = 0; i < this.conexiones.size(); i++) {
            ArrayList<String> conjunto = new ArrayList<>();
            
            for (int j = i*n; j < (i+1)*n; j++) {
                String pwd = this.buscador.getPasswords().get(j);
                if (pwd != null)
                    conjunto.add(pwd);
            }
            
            conjuntos.add(conjunto);
        }
        
        //Iniciar Operación
        for (int i = 0; i < this.conexiones.size(); i++) {
            System.out.println(conjuntos.get(i).size());
            conexiones.get(i).asignarPassword(user, conjuntos.get(i));
            conexiones.get(i).activarActividad();
            
        }
        
        this.esperando = false;
        this.panel.refrescarPantalla();
    }
    
    public void detenerBusqueda () {
        for (HiloConexion conexion : this.conexiones) {
            conexion.desactivarActividad();
        }
    }
    
    
    public String recibirMensaje () throws IOException {
        //Se obtiene el flujo entrante desde el cliente
        BufferedReader entrada = new BufferedReader(new InputStreamReader(cs.getInputStream()));
        while((mensajeServidor = entrada.readLine()) != null) //Mientras haya mensajes desde el cliente
            {
               return mensajeServidor;
            }
        
        return "ERROR";
    }
    
    public ArrayList<HiloConexion> obtenerConexiones () {
        return this.conexiones;
    }
    
    public void actualizarEstadosInterfaz () {
        this.panel.refrescarPantalla();
    }
    
    public void validarResultado (String[] resultado) {
        System.out.println(resultado[0]);
        if (this.buscador.validarResultado(resultado)) {
            JOptionPane.showMessageDialog(null, "Búsqueda Detenida!!!\nContraseña Encontrada por : " + resultado[0] + "\nContraseña encriptada:" + resultado[2]);
            this.detenerBusqueda();
        }
    }
}
