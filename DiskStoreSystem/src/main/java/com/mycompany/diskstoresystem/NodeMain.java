package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.DiskServiceGrpc;
import com.mycompany.diskstoresystem.proto.JoinRequest;
import com.mycompany.diskstoresystem.proto.JoinResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.mycompany.diskstoresystem.proto.Empty;

public class NodeMain {

    private static int MY_PORT;
    private static final int START_PORT = 5555;
    private static final int TCP_GATEWAY_PORT = 6666;
    private static boolean isLeader = false;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        System.out.println("=== Distributed Disk Register System ===");

        // 1. ADIM: Portu bul ve gRPC Sunucuyu başlat
        startServerOnAvailablePort();

        // 2. ADIM: Role göre TCP Gateway veya Join başlat
        if (isLeader) {
            System.out.println("[ROL] LIDER: TCP Port 6666 dinleniyor...");
            startTcpGateway();
        } else {
            System.out.println("[ROL] UYE: Lider'e katilma istegi gonderiliyor...");
            joinFamily();
        }

        // 3. ADIM: Durum raporlama
        startFamilyReportingThread();

        // 4. ADIM: Heartbeat baslat
        startHeartbeat();
    }

    private static void startServerOnAvailablePort() {
        int port = START_PORT;
        boolean started = false;

        while (!started && port < 5600) {
            try {
                Server server = ServerBuilder.forPort(port)
                        .addService(new FamilyServiceImpl(port))
                        .build()
                        .start();

                MY_PORT = port;
                isLeader = (MY_PORT == START_PORT);
                started = true;
                System.out.println("Node basariyla baslatildi. Port: " + MY_PORT);

                Thread serverThread = new Thread(() -> {
                    try {
                        server.awaitTermination();
                    } catch (InterruptedException e) {
                        System.err.println("Sunucu durdu.");
                    }
                });
                serverThread.start();

            } catch (IOException e) {
                port++;
            }
        }
    }

    private static void startTcpGateway() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_GATEWAY_PORT)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        try (Scanner in = new Scanner(socket.getInputStream());
                             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                            out.println("--- Distributed Disk Register Gateway'e Hosgeldiniz ---");

                            while (in.hasNextLine()) {
                                String line = in.nextLine();
                                if (line.equalsIgnoreCase("EXIT"))
                                    break;

                                String[] parts = line.split(" ", 3);
                                String command = parts[0].toUpperCase();

                                if (command.equals("SET") && parts.length == 3) {
                                    FamilyServiceImpl.sendStoreToAll(parts[1], parts[2]);
                                    out.println("OK - Kayit baslatildi.");
                                } else if (command.equals("GET") && parts.length == 2) {
                                    // Metot artik bize String donuyor, bunu istemciye yazdiralim
                                    String result = FamilyServiceImpl.sendRetrieveRequest(parts[1]);
                                    out.println(result);
                                } else {
                                    out.println("HATA: Gecersiz komut.");
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("TCP Baglantisi koptu.");
                        }
                    }).start();
                }
            } catch (IOException e) {
                System.err.println("TCP Gateway hatasi: " + e.getMessage());
            }
        }).start();
    }

    private static void joinFamily() {
        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", START_PORT)
                    .usePlaintext()
                    .build();

            DiskServiceGrpc.DiskServiceBlockingStub stub = DiskServiceGrpc.newBlockingStub(channel);
            // Lideri kaydet
            NodeRegistry.addNode(START_PORT, stub, channel);

            JoinRequest req = JoinRequest.newBuilder().setPort(MY_PORT).build();

            JoinResponse res = stub.join(req);

            if (res.getSuccess()) {
                System.out.println("[JOIN] Lider onayi: " + res.getMessage());
            }
            // Not: Kanalı kapatmıyoruz çünkü ileride liderden mesaj almak için aktif
            // kalmalı.
        } catch (Exception e) {
            System.err.println("[JOIN] Lidere baglanilamadi! Liderin ayakta oldugundan emin olun.");
        }
    }

    private static void startFamilyReportingThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(20000); // 20 saniyede bir rapor ver
                    if (isLeader) {
                        System.out.println("\n========= SISTEM RAPORU (LIDER) =========");

                        // Liderin kendi mesajlarını say (messages klasöründeki dosya sayısı)
                        File folder = new File("messages");
                        int localCount = folder.exists() ? folder.list().length : 0;
                        System.out.println("Liderdeki Toplam Mesaj: " + localCount);

                        // Üyelerdeki mesajları yazdır
                        System.out.println("Üyelerin Mesaj Dağılımı:");
                        FamilyServiceImpl.printMemberStats(); // Bunu aşağıda tanımlayacağız
                        System.out.println("========================================\n");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private static void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Iterate through known members and call the Ping RPC.
                for (Integer port : NodeRegistry.getMembers().keySet()) {
                    try {
                        DiskServiceGrpc.DiskServiceBlockingStub stub = NodeRegistry.getMembers().get(port);
                        stub.ping(Empty.newBuilder().build());
                    } catch (Exception e) {
                        System.err.println("Member [port: " + port + "] is unreachable.");
                        NodeRegistry.removeNode("localhost", port);
                    }
                }

                // Election Check: Call nodeRegistry.isLowestPort(myPort).
                if (!isLeader && NodeRegistry.isLowestPort(MY_PORT)) {
                    promoteToLeader();
                }

            } catch (Exception e) {
                System.err.println("[HEARTBEAT] Error: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private static void promoteToLeader() {
        try {
            isLeader = true;
            System.out.println("[LEADER PROMOTION] Determining leadership...");
            // Wait a bit to ensure port 6666 is clear or just retry
            startTcpGateway();
            System.out.println("[LEADER PROMOTION] I am the new LEADER!");
        } catch (Exception e) {
            System.err.println("[LEADER PROMOTION] Failed to start TCP Gateway: " + e.getMessage());
        }
    }
}
