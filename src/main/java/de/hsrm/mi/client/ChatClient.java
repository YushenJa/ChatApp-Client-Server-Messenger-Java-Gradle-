package de.hsrm.mi.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import de.hsrm.mi.protocol.TCPProtocol;
import de.hsrm.mi.protocol.UDPChat;

public class ChatClient {

    private static final String LOGIN       = "LOGIN";
    private static final String REGISTER    = "REGISTER";
    private static final String LIST        = "LIST";
    private static final String INVITE      = "INVITE";
    private static final String UDPPORT     = "UDPPORT";
    private static final String OK          = "OK";

    public static void main(String[] args) throws Exception {
        try (TCPProtocol tcp = new TCPProtocol("localhost", 12345);
             BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.print("\r\nUser: ");
            String user = stdin.readLine();
            System.out.print("Pass: ");
            String pw = stdin.readLine();

            System.out.print("Registrieren (j/n)? ");
            String regAntwort = stdin.readLine();
            if (regAntwort.equalsIgnoreCase("j")) {
                tcp.sendCommand(REGISTER, user, pw);
                if (!OK.equals(tcp.receive())) {
                    System.out.println("Registrierung fehlgeschlagen");
                } else {
                    System.out.println("Registrierung erfolgreich!");
                    tcp.sendCommand(LOGIN, user, pw);       
                }
            } else if (regAntwort.equalsIgnoreCase("n")) {
                System.out.print("Login (j/n)? ");
                String regAntwort2 = stdin.readLine();
                if (regAntwort2.equalsIgnoreCase("j")) {
                    tcp.sendCommand(LOGIN, user, pw);
                    if (!OK.equals(tcp.receive())) {
                        System.out.println("Login fehlgeschlagen");
                        return;
                    } else {
                        System.out.println("Login erfolgreich!");  
                    }
                }
            }



            UDPChat udp = new UDPChat();
            udp.open();
            tcp.sendCommand(UDPPORT, Integer.toString(udp.getPort()));
            udp.startListening((senderAddr, plainMsg) -> {
                System.out.println("\n[CHAT от " + senderAddr.getHostAddress() + "]: " + plainMsg); //hier  senderAddr.getHostAddress() durch name ersetzen
                System.out.print(">>> ");
            });

            System.out.println(
                "Eingeloggt! Verfügbare Befehle:\n" +
                "  LIST                      - zeigt alle aktiven Benutzer\n" +
                "  INVITE|<user>             - laedt <user> zu einem Chat ein\n" +
                "  INVITE_RESP|<user>|ACCEPT - nimmt eine Einladung von <user> an\n" +
                "  INVITE_RESP|<user>|REJECT - lehnt eine Einladung von <user> ab\n" +
                "  /msg <Nachricht>          - sendet eine Nachricht im aktiven Chat\n"
            );

            final InetAddress[] chatPartnerAddress = {null};
            final int[] chatPartnerPort = {-1};

            // TCP Listener
            Thread tcpListener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = tcp.receive()) != null) {
                        if (msg.startsWith("CHAT_START|")) {
                            String[] parts = msg.split("\\|");
                            String partnerName = parts[1];
                            chatPartnerAddress[0] = InetAddress.getByName(parts[2]);
                            chatPartnerPort[0] = Integer.parseInt(parts[3]);
                            System.out.println("\n[CHAT VERBUNDEN mit " + partnerName + " (" + parts[2] + ":" + parts[3] + ")]");
                        } else {
                            System.out.println("\n[SERVER]: " + msg);
                        }
                        System.out.print(">>> ");
                    }
                } catch (IOException e) {
                    System.err.println("TCP-Verbindung beendet: " + e.getMessage());
                }
            });
            tcpListener.setDaemon(true);
            tcpListener.start();

            // UDP Listener
             String line;
            System.out.print(">>> ");
            while ((line = stdin.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                String cmd = parts[0].toUpperCase();

                switch (cmd) {
                    case "LIST" -> tcp.sendCommand(LIST);

                    case "INVITE" -> {
                        if (parts.length < 2) {
                            System.out.println("Benutzung: INVITE <user>");
                            break;
                        }
                        tcp.sendCommand(INVITE, parts[1]);
                    }

                    case "INVITE_RESP" -> {
                        if (parts.length < 2) {
                            System.out.println("Benutzung: INVITE_RESP <user> <ACCEPT|REJECT>");
                            break;
                        }
                        String[] args2 = parts[1].split("\\s+");
                        if (args2.length < 2) {
                            System.out.println("Benutzung: INVITE_RESP <user> <ACCEPT|REJECT>");
                            break;
                        }
                        tcp.sendCommand("INVITE_RESP", args2[0], args2[1].toUpperCase());
                    }

                    case "/MSG" -> {
                        if (chatPartnerAddress[0] == null || chatPartnerPort[0] == -1) {
                            System.out.println("Нет активного чата. Сначала используйте INVITE и INVITE_RESP.");
                            break;
                        }
                        if (parts.length < 2) {
                            System.out.println("Использование: /msg <текст>");
                            break;
                        }
                        udp.sendMessage(parts[1], chatPartnerAddress[0], chatPartnerPort[0]);
                    }

                    default -> {
                        tcp.sendRaw(line);
                    }
                }
                System.out.print(">>> ");
            }
        }
    }
}