package com.mycompany.diskstoresystem;

import java.util.Scanner;

public class DiskStoreSystem {

    public static void main(String[] args) {
        System.out.println("=== Disk Store System ===");
        System.out.println("1. Server'ı Başlat (Sunucu)");
        System.out.println("2. Client'ı Başlat (İstemci)");
        System.out.print("Seçiminiz (1 veya 2): ");

        Scanner scanner = new Scanner(System.in);
        String secim = scanner.nextLine();

        try {
            if (secim.equals("1")) {
                System.out.println("Server başlatılıyor...");
                // Artık hata fırlatsa bile try-catch içinde olduğu için kızmayacak
                GrpcDiskServer.main(args); 
            } else if (secim.equals("2")) {
                System.out.println("Client başlatılıyor...");
                GrpcDiskClient.main(args);
            } else {
                System.out.println("Geçersiz seçim! Program kapanıyor.");
            }
        } catch (Exception e) {
            // Bir hata olursa burada yakalayıp ekrana basacağız
            System.err.println("Bir hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }
}