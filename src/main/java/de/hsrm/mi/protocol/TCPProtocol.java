package de.hsrm.mi.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPProtocol implements AutoCloseable {
    private static final String SEP = "|";

    private final Socket socket;                // TCP-Socket
    private final BufferedReader in;            // Thread, um die Antwort des Servers zu lesen
    private final PrintWriter out;              //Stream, um Daten an den Server zu senden
    

    public TCPProtocol(String host, int port) throws IOException {
        this.socket = new Socket(host, port);      
        this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));       
        this.out    = new PrintWriter(socket.getOutputStream(), true);      
    }    

    public void sendCommand(String... parts) {          // Sendet einen Befehl an den Server
        out.println(String.join(SEP, parts));           // → LOGIN|user|pass
    }

    public void sendRaw(String line) { out.println(line); }     // Sendet einen Befehl ohne Bearbeitung an den Server

    public String receive() throws IOException { return in.readLine(); }        // Liest eine Antwort vom Server (z.B OK oder ERROR)

    @Override 
    public void close() throws IOException {
        in.close(); out.close(); socket.close();
    }
}