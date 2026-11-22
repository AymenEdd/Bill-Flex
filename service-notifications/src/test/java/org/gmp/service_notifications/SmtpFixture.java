package org.gmp.service_notifications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class SmtpFixture implements AutoCloseable {

    private final ServerSocket server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<String> rejectedRecipients = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, AtomicInteger> deliveries = new ConcurrentHashMap<>();

    SmtpFixture() {
        try {
            server = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        executor.submit(this::acceptConnections);
    }

    int port() {
        return server.getLocalPort();
    }

    void reject(String recipient) {
        rejectedRecipients.add(recipient);
    }

    int mailCount(String recipient) {
        AtomicInteger count = deliveries.get(recipient);
        return count == null ? 0 : count.get();
    }

    private void acceptConnections() {
        while (!server.isClosed()) {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(4000);
                handle(socket);
            } catch (IOException ex) {
                if (!server.isClosed()) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
        respond(writer, "220 Bill-Flex test SMTP");
        String recipient = null;
        String line;
        while ((line = reader.readLine()) != null) {
            String command = line.toUpperCase(Locale.ROOT);
            if (command.startsWith("RCPT TO:<")) {
                recipient = line.substring(9, line.indexOf('>'));
                respond(writer, rejectedRecipients.contains(recipient) ? "550 Rejected test recipient" : "250 Recipient OK");
            } else if (command.equals("DATA")) {
                respond(writer, "354 End data with a dot on its own line");
                while ((line = reader.readLine()) != null && !line.equals(".")) {
                    // Consume the message before acknowledging it.
                }
                if (line == null) {
                    return;
                }
                deliveries.computeIfAbsent(recipient, address -> new AtomicInteger()).incrementAndGet();
                respond(writer, "250 Message received");
            } else if (command.equals("QUIT")) {
                respond(writer, "221 Closing");
                return;
            } else {
                respond(writer, "250 OK");
            }
        }
    }

    private void respond(BufferedWriter writer, String line) throws IOException {
        writer.write(line + "\r\n");
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        server.close();
        executor.shutdownNow();
    }
}
