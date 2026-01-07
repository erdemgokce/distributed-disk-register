package com.mycompany.diskstoresystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class HaToKuSeTest {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 6666; // Liderin TCP Portu
    private static final int MESSAGE_COUNT = 1000;

    public static void main(String[] args) {
        System.out.println("Sistem Kontrol Ediliyor...");

        // 1. ADIM: Lider Sunucu açık mı kontrolü
        if (!isLeaderActive()) {
            System.err.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("!!! HATA: LİDER SUNUCU BULUNAMADI (Port: " + SERVER_PORT + ") !!!");
            System.err.println("!!! Lütfen önce Lider Sunucuyu başlatın.            !!!");
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return;
        }

        System.out.println("Lider Sunucu Aktif! Testler Başlıyor...\n");

        try {
            // Veritabanını dolduruyoruz
            testLoadBalancing();

            // Crash senaryosu (Sadece senin istediğin mesajı getirir)
            testCrashScenario();

        } catch (Exception e) {
            System.err.println("Test sırasında hata: " + e.getMessage());
            e.printStackTrace(); // Hatayı detaylı görmek için ekledim
        }
    }

    private static boolean isLeaderActive() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void testLoadBalancing() {
        System.out.println("--- YÜK TESTİ (VERİ DOLDURMA) ---");
        System.out.println("1000 Adet Mesaj Gönderiliyor...");
        int successCount = 0;

        for (int i = 1; i <= MESSAGE_COUNT; i++) {
            String response = sendCommand("SET " + i + " Test_Mesajı:" + i);

            if (response == null) {
                System.err.println("\n!!! HATA: Yükleme sırasında Lider Sunucu koptu! !!!");
                return;
            }
            successCount++;
            if (i % 200 == 0) System.out.println(i + ". mesaj yüklendi...");
        }
        System.out.println("Yükleme tamamlandı. Toplam: " + successCount);
    }

    private static void testCrashScenario() {
        System.out.println("\n--- CRASH TESTİ (MANUEL KONTROL) ---");
        Scanner inputScanner = new Scanner(System.in);

        // 1. Sadece ID alıyoruz
        System.out.print("Görmek istediğin Mesaj ID'si (örn: 500): ");
        if (inputScanner.hasNextLine()) {
            String targetID = inputScanner.nextLine();

            // 2. Bekleme
            System.out.println("\n--- ŞİMDİ CRASH ZAMANI ---");
            System.out.println("Lider hariç bir node'u kapatabilirsin.");
            System.out.print("Node'u kapattıktan sonra mesajı çekmek için ENTER'a bas...");
            inputScanner.nextLine();

            // 3. Mesajı Çekme
            System.out.println("\nID = " + targetID + " için Lider'e istek atılıyor...");
            String getResponse = sendCommand("GET " + targetID);

            if (getResponse == null) {
                System.err.println("HATA: Lider Sunucuya ulaşılamıyor.");
                return;
            }

            // 4. SONUÇ
            System.out.println("\n**************************************************");
            System.out.println("LİDERDEN GELEN CEVAP: " + getResponse);
            System.out.println("**************************************************");
        } else {
            System.out.println("Giriş alınamadı!");
        }
    }

    private static String sendCommand(String command) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // NOT: Eğer sunucun bağlanınca "Hoşgeldin" vb. bir şey göndermiyorsa
            // aşağıdaki "welcomeBanner" satırını SİLMEN gerekir.
            // Sunucunun protokolüne göre burası kodu kilitleyebilir.
            String welcomeBanner = reader.readLine();

            writer.println(command);

            String response = reader.readLine();
            return response;

        } catch (Exception e) {
            return null;
        }
    }
}
