package de.hsrm.mi.protocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.function.BiConsumer;

import de.hsrm.mi.utils.EncryptionUtil;

public class UDPChat {

    private DatagramSocket socket;

    public void open() throws Exception {
        socket = new DatagramSocket(0);
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public void sendMessage(String message, InetAddress address, int port) throws Exception {
        String encrypted = EncryptionUtil.encrypt(message);
        byte[] buf = encrypted.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
    }

    public void startListening(BiConsumer<InetAddress, String> onMessage) {
        Thread listener = new Thread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                while (true) {
                    socket.receive(packet);
                    String encrypted = new String(packet.getData(), 0, packet.getLength());
                    String plain = EncryptionUtil.decrypt(encrypted);
                    onMessage.accept(packet.getAddress(), plain);
                }
            } catch (Exception e) {
                System.err.println("UDP-Listener beendet: " + e.getMessage());
            }
        });
        listener.setDaemon(true);
        listener.start();
    }
        public boolean isClosed() {
            return socket == null || socket.isClosed();
        }

        public void close() {
            if (socket != null && !socket.isClosed()) socket.close();
        }
}