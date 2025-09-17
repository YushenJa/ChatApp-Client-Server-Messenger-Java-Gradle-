package de.hsrm.mi.gui;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.hsrm.mi.protocol.TCPProtocol;
import de.hsrm.mi.protocol.UDPChat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatClientUI extends Application {

    private static final String REGISTER     = "REGISTER";
    private static final String LOGIN        = "LOGIN";
    private static final String LIST         = "LIST";
    private static final String INVITE       = "INVITE";
    private static final String INVITE_RESP  = "INVITE_RESP";
    private static final String UDPPORT      = "UDPPORT";
    private static final String CHAT_START   = "CHAT_START";
    private static final String OK           = "OK";
    private static final String ERR          = "ERR";

    
    private Stage primaryStage;
    private TCPProtocol tcp;
    private UDPChat udp;
    private String username;

    
    private final Map<String, ChatWindow> openChats = new ConcurrentHashMap<>();
    private final ListView<String> userList = new ListView<>();

    
    @Override public void start(Stage stage) { this.primaryStage = stage; showLoginWindow(); }

    @Override public void stop() throws Exception {
        if (udp != null && !udp.isClosed()) udp.close();
        if (tcp != null) tcp.close();
    }

    
    private void showLoginWindow() {
        TextField userField   = new TextField();
        PasswordField passFld = new PasswordField();
        Label feedback        = new Label();

        Button loginBtn    = new Button("Login");
        Button registerBtn = new Button("Registrieren");

        loginBtn.   setOnAction(e -> handleLogin(userField.getText(), passFld.getText(), feedback));
        registerBtn.setOnAction(e -> handleRegister(userField.getText(), passFld.getText(), feedback));

        VBox pane = new VBox(10,
                new Label("Benutzername:"), userField,
                new Label("Passwort:"),     passFld,
                new HBox(10, loginBtn, registerBtn),
                feedback);
        pane.setPadding(new Insets(20));

        primaryStage.setTitle("Chat – Login");
        primaryStage.setScene(new Scene(pane, 300, 220));
        primaryStage.show();
    }

    private void handleLogin(String user, String pw, Label fb) {
        try {
            tcp = new TCPProtocol("localhost", 12345);
            tcp.sendCommand(LOGIN, user, pw);

            if (OK.equals(tcp.receive())) {
                this.username = user;
                openUdpSocket();
                startTcpListener();
                showMainWindow();
            } else fb.setText("Login fehlgeschlagen");
        } catch (Exception ex) {
            fb.setText("Fehler: " + ex.getMessage());
        }
    }

    private void handleRegister(String user, String pw, Label fb) {
        try (TCPProtocol tmp = new TCPProtocol("localhost", 12345)) {
            tmp.sendCommand(REGISTER, user, pw);
            fb.setText(OK.equals(tmp.receive())
                    ? "Registrierung erfolgreich" : "Registrierung fehlgeschlagen");
        } catch (Exception ex) { fb.setText("Fehler: " + ex.getMessage()); }
    }

    
    private void showMainWindow() {
        Button refreshBtn = new Button("Aktualisieren");
        Button inviteBtn  = new Button("Einladen");

        userList.setOnMouseClicked(this::handleListDoubleClick);
        
        refreshBtn.setOnAction(e -> tcp.sendCommand(LIST));
        inviteBtn .setOnAction(e -> {
            String target = userList.getSelectionModel().getSelectedItem();
            if (target != null && !target.equals(username))
                tcp.sendCommand(INVITE, target);
        });

        VBox pane = new VBox(10,
                new Label("Angemeldet als: " + username),
                new HBox(10, refreshBtn, inviteBtn),
                userList);
        pane.setPadding(new Insets(20));

        primaryStage.setTitle("Chat – Hauptfenster");
        primaryStage.setScene(new Scene(pane, 280, 380));
    }

    private void openUdpSocket() throws Exception {
        udp = new UDPChat();
        udp.open();
        tcp.sendCommand(UDPPORT, Integer.toString(udp.getPort()));

        udp.startListening((ip, msg) -> {
            Platform.runLater(() -> {
                String partner = msg.substring(0, msg.indexOf(':')).trim();
                ChatWindow cw = openChats.computeIfAbsent(
                    partner, k -> new ChatWindow(partner, ip, -1)); 
                cw.appendIncoming(msg);
            });
        });
    }

    private void sendUdp(String txt, InetAddress addr, int port) {
        try {
            udp.sendMessage(txt, addr, port);
        } catch (Exception ex) {
            System.err.println("UDP-Send-Fehler: " + ex.getMessage());
        }
    }

    private void startTcpListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = tcp.receive()) != null) {
                    String[] p = line.split("\\|", 4);
                    switch (p[0]) {
                        case LIST -> Platform.runLater(() -> updateUserList(p));
                        case INVITE -> handleInvite(p[1]);
                        case CHAT_START -> handleChatStart(p);
                        case ERR -> System.err.println("Server‑Fehler: " + line);
                    }
                }
            } catch (IOException ignored) { }
        }, "tcp-listener").start();
    }

    private void updateUserList(String[] parts) {
        String[] usersRaw = (parts.length > 1 && !parts[1].isEmpty())
                ? parts[1].split(",") : new String[0];

        List<String> users = Arrays.stream(usersRaw)
                                   .filter(u -> !u.equals(username))
                                   .toList();
        userList.getItems().setAll(users);
    }

    private void handleInvite(String fromUser) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    fromUser + " möchte chatten",
                    ButtonType.YES, ButtonType.NO);
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(btn -> {
                String resp = btn == ButtonType.YES ? "ACCEPT" : "DECLINE";
                tcp.sendCommand(INVITE_RESP, fromUser, resp);
            });
        });
    }

    private void handleChatStart(String[] p) {
        Platform.runLater(() -> {
            try {
                String peer     = p[1];
                InetAddress ip  = InetAddress.getByName(p[2]);
                int port        = Integer.parseInt(p[3]);

                openChats.computeIfAbsent(peer, k -> new ChatWindow(peer, ip, port));
            } catch (Exception e) {
                System.err.println("CHAT_START-Parsing-Fehler: " + e);
            }
        });
    }

    private void inviteSelectedUser() {
        String target = userList.getSelectionModel().getSelectedItem();
        if (target != null && !target.equals(username)) tcp.sendCommand(INVITE, target);
    }

    private void handleListDoubleClick(MouseEvent ev) {
        if (ev.getClickCount() == 2) {
            String partner = userList.getSelectionModel().getSelectedItem();
            if (partner == null || partner.equals(username)) return;

            ChatWindow cw = openChats.get(partner);
            if (cw != null) cw.show();           
            else inviteSelectedUser();    
        }
    }

    private class ChatWindow {
        private final Stage stage;          
        private final TextArea  area  = new TextArea();
        private final TextField input = new TextField();
        private final InetAddress addr;
        private final int port;

        ChatWindow(String partner, InetAddress addr, int port) {
            this.addr = addr;
            this.port = port;
            this.stage = new Stage();     

            area.setEditable(false);
            Button sendBtn = new Button("Senden");
            sendBtn.setOnAction(e -> {
                String msg = username + ": " + input.getText();
                sendUdp(msg, this.addr, this.port);
                appendOutgoing(msg);
            });

            stage.setTitle("Chat – mit " + partner);
            stage.setScene(new Scene(
                    new VBox(10, area, new HBox(10, input, sendBtn)), 420, 300));

            
            stage.setOnCloseRequest(e -> {
                e.consume();    
                stage.hide();    
            });

            stage.show();
        }

        void appendIncoming(String msg) {
            area.appendText(msg + "\n");
            show();              
        }
        void appendOutgoing(String msg) {
            area.appendText(msg + "\n");
            input.clear();
        }
        void show() {
            if (!stage.isShowing()) stage.show();
            stage.toFront();
        }
    }

}
