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

    // --- YENİ EKLENDİ: Üye listesini tutacak olan Map ---
    // Integer: Port Numarası, Stub: O üyeye mesaj gönderme aracı
    private static final Map<Integer, DiskServiceGrpc.DiskServiceBlockingStub> memberStubs = new ConcurrentHashMap<>();

    public FamilyServiceImpl(int port) {
        this.myPort = port;
    }

    // Join (Üye Kaydı)
    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        int remotePort = request.getPort();

        // Eğer gelen port biz değilsek ve listede yoksa, onu AİLE LİSTESİNE EKLE
        if (remotePort != this.myPort && !memberStubs.containsKey(remotePort)) {
            // O üyeye doğru bir gRPC kanalı açıyoruz
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", remotePort)
                    .usePlaintext()
                    .build();

            // Kanalı kullanacak "Stub" nesnesini oluşturup listeye atıyoruz
            memberStubs.put(remotePort, DiskServiceGrpc.newBlockingStub(channel));
            System.out.println("[FAMILY] Yeni üye listeye eklendi: " + remotePort);
        }

        JoinResponse response = JoinResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Lider (" + myPort + ") seni aileye kabul etti.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        // SET komutu buraya gelecek (Şimdilik aynen kalsın)
        String key = request.getKey();
        System.out.println("Kaydediliyor: " + key);

        StoreResponse response = StoreResponse.newBuilder()
                .setSuccess(true)
                .setMessage("OK")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void retrieve(RetrieveRequest request, StreamObserver<RetrieveResponse> responseObserver) {
        // GET komutu buraya gelecek (Şimdilik aynen kalsın)
        RetrieveResponse response = RetrieveResponse.newBuilder()
                .setData("Ornek Veri")
                .setFound(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- GÜNCELLENDİ: Broadcast (Üye Mesajı Alınca) ---
    @Override
    public void broadcast(ChatMessage request, StreamObserver<Empty> responseObserver) {
        // Mesaj geldiğinde ekrana güzelce basıyoruz
        System.out.println("\n--------------------------------------");
        System.out.println(" YENİ MESAJ GELDİ!");
        System.out.println("  Kimden (Port): " + request.getFromPort());
        System.out.println("  Mesaj: " + request.getText());
        System.out.println("--------------------------------------");

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    // --- YENİ EKLENDİ: Liderin Mesajı Dağıtması İçin ---
    // Bu metodu NodeMain içinden çağıracağız
    public static void broadcastToAll(String text, int fromPort) {
        // Gönderilecek Protobuf mesajını hazırla
        ChatMessage chatMsg = ChatMessage.newBuilder()
                .setText(text)
                .setFromPort(fromPort)
                .setTimestamp(System.currentTimeMillis())
                .build();

        System.out.println("[BROADCAST] Mesaj " + memberStubs.size() + " üyeye dağıtılıyor...");

        // Listedeki herkese tek tek gönder
        for (Integer targetPort : memberStubs.keySet()) {
            try {
                memberStubs.get(targetPort).broadcast(chatMsg);

                System.out.println("-> Üyeye (" + targetPort + ") başarıyla iletildi.");
            } catch (Exception e) {
                System.err.println("-> Üyeye (" + targetPort + ") mesaj gidemedi: " + e.getMessage());

            }
        }
    }
}