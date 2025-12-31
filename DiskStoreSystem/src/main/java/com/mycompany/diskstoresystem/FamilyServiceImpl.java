package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FamilyServiceImpl extends DiskServiceGrpc.DiskServiceImplBase {
    private final int myPort;
    private static int nextMemberIndex = 0;
    private static final Map<Integer, DiskServiceGrpc.DiskServiceBlockingStub> memberStubs = new ConcurrentHashMap<>();

    public FamilyServiceImpl(int port) {
        this.myPort = port;
    }

    // --- 1. JOIN (Yeni Üye Kaydı) ---
    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        int remotePort = request.getPort();
        if (remotePort != this.myPort && !memberStubs.containsKey(remotePort)) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", remotePort)
                    .usePlaintext()
                    .build();
            memberStubs.put(remotePort, DiskServiceGrpc.newBlockingStub(channel));
            System.out.println("[FAMILY] Yeni üye listeye eklendi: " + remotePort);
        }
        JoinResponse response = JoinResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Lider (" + myPort + ") seni kabul etti.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 2. STORE (Üye Veriyi Alınca Kaydeder) ---
    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        // DÜZELTME: Üye (Follower) mesajı aldığında diske yazmalı
        saveToDisk(request.getKey(), request.getData());

        StoreResponse response = StoreResponse.newBuilder()
                .setSuccess(true)
                .setMessage(myPort + " portuna kaydedildi.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 3. RETRIEVE (Veri Okuma) ---
    @Override
    public void retrieve(RetrieveRequest request, StreamObserver<RetrieveResponse> responseObserver) {
        RetrieveResponse response = RetrieveResponse.newBuilder()
                .setData("Ornek Veri")
                .setFound(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 4. BROADCAST (Chat Mesajı Gelince) ---
    @Override
    public void broadcast(ChatMessage request, StreamObserver<Empty> responseObserver) {
        System.out.println("\n--------------------------------------");
        System.out.println(" 💬 YENİ MESAJ GELDİ!");
        System.out.println("  Kimden (Port): " + request.getFromPort());
        System.out.println("  Mesaj: " + request.getText());
        System.out.println("--------------------------------------");
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    // --- LİDER METOTLARI (Statik) ---

    public static void broadcastToAll(String text, int fromPort) {
        ChatMessage chatMsg = ChatMessage.newBuilder()
                .setText(text)
                .setFromPort(fromPort)
                .setTimestamp(System.currentTimeMillis())
                .build();

        for (Integer targetPort : memberStubs.keySet()) {
            try {
                memberStubs.get(targetPort).broadcast(chatMsg);
            } catch (Exception e) {
                System.err.println("-> Mesaj iletilemedi: " + targetPort);
            }
        }
    }

    public static void sendStoreToAll(String key, String value) {
        // Lider önce kendine yazar
        saveToDisk(key, value);

        int tolerance = getToleranceValue();
        if (memberStubs.isEmpty()) {
            System.out.println("[LIDER] Yedeklenecek üye yok.");
            return;
        }

        List<Integer> ports = new ArrayList<>(memberStubs.keySet());
        int count = 0;
        int totalMembers = ports.size();

        while (count < tolerance && count < totalMembers) {
            int targetPort = ports.get(nextMemberIndex % totalMembers);
            nextMemberIndex++;
            try {
                StoreRequest req = StoreRequest.newBuilder().setKey(key).setData(value).build();
                memberStubs.get(targetPort).store(req);
                System.out.println("-> Yedeklendi: Port " + targetPort);
                count++;
            } catch (Exception e) {
                System.err.println("-> Port " + targetPort + " hata verdi.");
            }
        }
    }

    public static void sendRetrieveRequest(String key) {
        System.out.println("[LIDER] Okuma isteği: " + key);
    }

    // --- YARDIMCI METOTLAR ---

    // DÜZELTME: İki tane saveToDiskStatic vardı, teke indirdik ve ismini saveToDisk yaptık
    private static void saveToDisk(String key, String data) {
        try {
            File folder = new File("messages");
            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, key + ".msg");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(data);
            }
            System.out.println("[DISK] Yazma basarili: " + file.getName());
        } catch (IOException e) {
            System.err.println("[DISK] Hata: " + e.getMessage());
        }
    }

    private static int getToleranceValue() {
        try {
            File file = new File("tolerance.conf");
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                if (scanner.hasNextInt()) return scanner.nextInt();
            }
        } catch (Exception e) {
            System.err.println("tolerance.conf okunamadı.");
        }
        return 1;
    }
}