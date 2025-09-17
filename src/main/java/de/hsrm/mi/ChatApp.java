package de.hsrm.mi;

import java.util.Arrays;

import de.hsrm.mi.client.ChatClient;
import de.hsrm.mi.gui.ChatClientUI;
import de.hsrm.mi.server.ChatServer;
import javafx.application.Application;

public class ChatApp {
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            Application.launch(ChatClientUI.class, args);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "server":
    			ChatServer.main(Arrays.copyOfRange(args, 1, args.length));
                break;

            case "client":
                ChatClient.main(Arrays.copyOfRange(args, 1, args.length));
                break;

            case "gui":
                Application.launch(ChatClientUI.class,
                                   Arrays.copyOfRange(args, 1, args.length));
                break;

            default:
                System.err.println("Unbekannter Modus: " + args[0] +
                                   " (erlaubt: server | client | gui)");
        }
    }
}