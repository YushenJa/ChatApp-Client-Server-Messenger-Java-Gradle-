package de.hsrm.mi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;

    private static final String REGISTER     = "REGISTER";
    private static final String LOGIN        = "LOGIN";
    private static final String LIST         = "LIST";
    private static final String INVITE       = "INVITE";
    private static final String INVITE_RESP  = "INVITE_RESP";
    private static final String UDPPORT      = "UDPPORT";
    private static final String CHAT_START   = "CHAT_START";

    private final Map<String,String> cred    = new ConcurrentHashMap<>();       // Logins und Passwörter
    private final Map<String,Client> online  = new ConcurrentHashMap<>();       //Liste der aktiven Clients

    public static void main(String[] args) throws IOException { new ChatServer().run(); }

    private void run() throws IOException {
        try (ServerSocket ss = new ServerSocket(PORT)) {                            //TCP-Port 12345 für eingehende Verbindungen
            System.out.println("\r\nServer laeuft auf Port " + PORT);
            while (true) new Thread(new ClientHandler(ss.accept())).start();        //Für jeden Client wird ein neuer ClientHandler gestartet
        }
    }

    private record Client(Socket sock, PrintWriter out, int udpPort) {}             //Dieser Datensatz beschreibt einen Kunden(TCP-Verbindung zum Client, der Datenfluss, über den der Server Nachrichten an diesen Client sendet, Die UDP-Portnummer)

    private class ClientHandler implements Runnable {
        private final Socket sock;
        private BufferedReader in;
        private PrintWriter out;
        private String user = null;
        private int udpPort = -1;

        ClientHandler(Socket s) { sock = s; }

        @Override
        public void run() {
            try (sock) {                                                                    // TCP-Client-Verbindung
                in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));     // Eingabe-Streams
                out = new PrintWriter(sock.getOutputStream(), true);              // Ausgabe-Streams

                String line;
                while ((line = in.readLine()) != null) {
                    String[] p = line.split("\\|", 4);                          //Server empfängt den Befehlsstring und ruft die gewünschte Methode auf
                    switch (p[0]) {
                        case REGISTER    -> reg(p);
                        case LOGIN       -> login(p);
                        case UDPPORT     -> udp(p);
                        case LIST        -> list();
                        case INVITE      -> invite(p);
                        case INVITE_RESP -> inviteResp(p);
                        default          -> out.println("ERR|UNKNOWN");
                    }
                }
            } catch (IOException ignored) { }
            finally { logout(); }
        }

        private void reg(String[] p){                                                       //Fügt einen Benutzer zu <<cred>> hinzu, wenn dieser noch nicht existiert
            if (p.length<3) { out.println("ERR|ARG"); return; }
            if (cred.putIfAbsent(p[1], hash(p[2]))==null) out.println("OK");
            else out.println("ERR|EXISTS");
        }

        private void login(String[] p){                                                     //Überprüft die Anmeldung und das Passwort, merkt sich den Benutzer und fügt ihn der Online-Liste hinzu
            if (p.length<3) { out.println("ERR|ARG"); return; }
            if (!hash(p[2]).equals(cred.get(p[1]))) { out.println("ERR|LOGIN"); return; }
            if (online.containsKey(p[1]))           { out.println("ERR|ONLINE"); return; }
            user=p[1];
            online.put(user,new Client(sock,out,-1));
            out.println("OK");
        }

        private void udp(String[] p){
            if (!logged()) return;
            udpPort = Integer.parseInt(p[1]);
            online.computeIfPresent(user,(k,c)->new Client(c.sock,c.out,udpPort));
        }

        private void list(){                                                                // Sendet eine Liste aller Online-Benutzer
            if (!logged()) return;
            Set<String> u = online.keySet();
            out.println(LIST+"|"+String.join(",",u));
        }

        private void invite(String[] p){                                                    // Sendet eine Einladung an den angegebenen Benutzer (falls online)
            if (!logged()||p.length<2) return;
            Client tgt = online.get(p[1]);
            if (tgt==null) { out.println("ERR|OFFLINE"); return; }
            tgt.out.println(INVITE+"|"+user);
        }

        private void inviteResp(String[] p){                                                // Wenn die Einladung angenommen wird, erhalten beide Clients einen CHAT_START-Befehl mit der Adresse und dem Port des Gesprächspartners
            if (!logged()||p.length<3) return;
            String src = p[1], decision = p[2];
            Client requester = online.get(src);
            if (requester==null) return;

            if ("ACCEPT".equals(decision) && udpPort!=-1) {
                requester.out.println(CHAT_START + "|" + user + "|" +
                        sock.getInetAddress().getHostAddress() + "|" + udpPort);

                this.out.println(CHAT_START + "|" + src + "|" +
                        requester.sock.getInetAddress().getHostAddress() + "|" + requester.udpPort());
            } else requester.out.println("ERR|REJECTED");
        }

        private void logout(){ if(user!=null) online.remove(user); }
        private boolean logged(){ if (user==null){ out.println("ERR|LOGIN_FIRST"); return false; } return true; }
    }

    private static String hash(String s){                                                   // Kennwörter werden nicht im Klartext gespeichert, sondern in eine Hash-Zeichenkette umgewandelt
        try{
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b:d) sb.append("%02x".formatted(b));
            return sb.toString();
        }catch(Exception e){ throw new RuntimeException(e); }
    }
}
