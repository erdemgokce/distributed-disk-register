package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.DiskServiceGrpc;
import com.mycompany.diskstoresystem.proto.JoinRequest;
import com.mycompany.diskstoresystem.proto.JoinResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class NodeMain {

    private static int MY_PORT;
    private static final int START_PORT = 5555;
    private static final int TCP_GATEWAY_PORT = 6666;
    private static boolean isLeader = false;

    public static void main(String[] args) {
        System.out.println("=== Distributed Disk Register System ===");

        // 1. ADIM: 5555'ten başlayarak boş portu bulur ve ve gRPC Server'ı başlatır.
        startServerOnAvailablePort();

        // 2. ADIM: Rolünü belirler ve ona göre davranır
        if (isLeader) {
            System.out.println("[ROL] LIDER: TCP Port 6666 dinleniyor...");
            // Lider ise dış dünyadan TCP mesajlarını bekleyen thread'i başlatır
            startTcpGateway();
        } else {
            System.out.println("[ROL] UYE: Lider'e (5555) katilma istegi gonderiliyor...");
            // Üye ise Lider'e gidip "Beni listene ekle" diyecek
            joinFamily();
        }

        // 3. ADIM: Her 10 saniyede bir aile listesini ekrana basar.
        startFamilyReportingThread();
    }

    private static void startServerOnAvailablePort() {
        int port = START_PORT;
        boolean started = false;

        while (!started && port < 5600) {
            try {
                // FamilyServiceImpl'i başlatırken hangi portta olduğumuzu ona söyleyelim
                Server server = ServerBuilder.forPort(port)
                        .addService(new FamilyServiceImpl(port))
                        .build()
                        .start();

                MY_PORT = port;
                isLeader = (MY_PORT == START_PORT);
                started = true;
                System.out.println("Node basariyla baslatildi. Port: " + MY_PORT);

                // Sunucunun açık kalması için awaitTermination'ı bir thread içinde bekletiyoruz
                Thread serverThread = new Thread(() -> {
                    try {
                        server.awaitTermination();
                    } catch (InterruptedException e) {
                        System.err.println("Sunucu beklenmedik sekilde durdu.");
                    }
                });
                serverThread.start();

            } catch (IOException e) {
                // Port doluysa (başka bir üye kapmışsa) bir sonrakini dene
                port++;
            }
        }
    }

    private static void startTcpGateway() {
        // Liderin TCP 6666 üzerinden komutları (SET/GET) dinlediği kısım
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_GATEWAY_PORT)) {
                while (true) {
                    try (Socket socket = serverSocket.accept();
                         Scanner in = new Scanner(socket.getInputStream())) {

                        if (in.hasNextLine()) {
                            String line = in.nextLine();
                            System.out.println("TCP Gateway'den gelen komut: " + line);
                            // TODO: Buradaki komut parse edilip FamilyServiceImpl üzerinden dağıtılacak
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("TCP Gateway baslatilamadi: " + e.getMessage());
            }
        }).start();
    }

    private static void joinFamily() {
        try {
            // Lider'in adresine (5555) bağlan
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", START_PORT)
                    .usePlaintext()
                    .build();

            // Lider'e Join isteği gönder
            DiskServiceGrpc.DiskServiceBlockingStub stub = DiskServiceGrpc.newBlockingStub(channel);

            JoinRequest req = JoinRequest.newBuilder()
                    .setPort(MY_PORT) // Kendi portumuzu bildiriyoruz
                    .build();

            JoinResponse res = stub.join(req);

            if (res.getSuccess()) {
                System.out.println("[JOIN] Lider onay verdi: " + res.getMessage());
            }

            // Kanalı açık tutmamıza gerek yok, Lider bizi listesine ekledi bile
            channel.shutdown();

        } catch (Exception e) {
            System.err.println("[JOIN] Lidere baglanilamadi! Belki de lider henüz ayakta degil.");
        }
    } {

        // Bu mantığı FamilyServiceImpl içinde implement edeceğiz.
        System.out.println("Lider'e Join istegi gonderiliyor (Port: 5555)...");
    }

    private static void startFamilyReportingThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // 10 saniyede bir rapor ver
                    System.out.println("\n--- Aile Durumu (Port: " + MY_PORT + ") ---");
                    // NodeRegistry.printMembers(); // Üye listesini basacak
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}