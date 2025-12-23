package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.DiskServiceGrpc;
import com.mycompany.diskstoresystem.proto.StoreRequest;
import com.mycompany.diskstoresystem.proto.StoreResponse;
import com.mycompany.diskstoresystem.proto.RetrieveRequest;
import com.mycompany.diskstoresystem.proto.RetrieveResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Scanner;

public class GrpcDiskClient {

    public static void main(String[] args) {
        // Kanal oluştur (Sunucu bağlantısı)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        // Stub (Vekil) oluştur
        DiskServiceGrpc.DiskServiceBlockingStub stub = DiskServiceGrpc.newBlockingStub(channel);
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- DISK STORE ISTEMCISI ---");
        System.out.println("Kullanim Formatlari:");
        System.out.println("  SET <anahtar> <mesaj>  -> Ornek: SET 55 Merhaba Dunya");
        System.out.println("  GET <anahtar>          -> Ornek: GET 55");
        System.out.println("  EXIT                   -> Cikis");
        System.out.println("----------------------------");

        while (true) {
            System.out.print("> "); // Komut satiri imleci
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("EXIT")) {
                System.out.println("Cikis yapiliyor...");
                break;
            }

            // Girdiyi boşluklara göre ayırıyoruz
            // split("\\s+", 3) demek: En fazla 3 parçaya böl.
            // 1. Parça: Komut (SET/GET)
            // 2. Parça: Anahtar (Key)
            // 3. Parça: Geri kalan her şey (Data) -> Bu sayede mesajda boşluk olsa bile bozulmaz.
            String[] parts = line.split("\\s+", 3);
            String command = parts[0].toUpperCase();

            try {
                if (command.equals("SET")) {
                    // Kontrol: SET komutu için en az 3 parça olmalı (SET, Key, Data)
                    if (parts.length < 3) {
                        System.out.println("HATA: Eksik parametre! Kullanim: SET <id> <mesaj>");
                        continue;
                    }
                    String key = parts[1];
                    String data = parts[2];

                    StoreRequest req = StoreRequest.newBuilder().setKey(key).setData(data).build();
                    StoreResponse res = stub.store(req);
                    System.out.println("SUNUCU: " + res.getMessage());

                } else if (command.equals("GET")) {
                    // Kontrol: GET komutu için en az 2 parça olmalı (GET, Key)
                    if (parts.length < 2) {
                        System.out.println("HATA: Eksik parametre! Kullanim: GET <id>");
                        continue;
                    }
                    String key = parts[1];

                    RetrieveRequest req = RetrieveRequest.newBuilder().setKey(key).build();
                    RetrieveResponse res = stub.retrieve(req);

                    if (res.getFound()) {
                        System.out.println("SONUC: " + res.getData());
                    } else {
                        System.out.println("SONUC: Veri bulunamadi (NOT_FOUND).");
                    }

                } else {
                    System.out.println("Gecersiz komut! Lutfen SET veya GET kullanin.");
                }
            } catch (Exception e) {
                System.out.println("Sunucu ile iletisim hatasi: " + e.getMessage());
            }
        }
        
        channel.shutdown();
    }
}