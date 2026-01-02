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
        System.out.println("Dağıtık Sistem Test Aşaması");

        try {
            // Yük dağıtma kısmı.
             testLoadBalancing();

            // Crash kısmı.
            testCrashScenario();

        } catch (Exception e) {
            System.err.println("Test sırasında genel hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testLoadBalancing() {
        System.out.println("\n1000 Adet Mesaj Gönderiliyor...");
        int successCount = 0;
        for (int i = 1; i <= MESSAGE_COUNT; i++) {
            String response = sendCommand("SET " + i + " Test_Mesajı:" + i);
            if (response != null && !response.isEmpty()) successCount++;
            if (i % 200 == 0) System.out.println(i + ". mesaj gönderildi...");
        }
        System.out.println("Yük testi tamamlandı. Başarılı: " + successCount);
    }

    private static void testCrashScenario() {
        System.out.println("\nCrash Testi Başlıyor...");
        Scanner inputScanner = new Scanner(System.in);
        String targetID = "500"; //ID = 500 olan mesajla işlem yapıyoruz.

        String targetValue = "Test_Mesajı:500";  //ID = 500 olan mesajda "Test_Mesajı:500" yazması lazım.
        //ID = 500 olan mesajı zaten yük testi sırasında veri tabanına kaydetmiştik.

        //Kullanıcı İşlemleri
        System.out.println("Lider hariç bir node'u kapattığından emin ol!");
        System.out.print("Crash testi yapmak için enter'a bas.");
        inputScanner.nextLine(); //Enter tuşuna basana kadar kodu durdurur.

        //Veriyi okuma.
        System.out.println("\nID = 500 olan mesaj okunmaya çalışılıyor.");
        String getResponse = sendCommand("GET " + targetID);

        System.out.println("   Sunucu Cevabı: " + getResponse);

        //Doğrulama kısmı.
        if (getResponse != null && getResponse.contains(targetValue)) {
            System.out.println("\nBAŞARILI: Tebrikler! Sistem çöken sunucuya rağmen veriyi getirdi.");
        } else {
            System.out.println("\nBAŞARISIZ: Veri getirilemedi.");
            System.out.println("Beklenen: " + targetValue);
            System.out.println("Gelen: " + getResponse);
        }
    }
    private static String sendCommand(String command) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String welcomeBanner = reader.readLine(); //sunucunun hoşgeldin mesajını çöpe attık ki gelen cevap bu sanılmasın.

            writer.println(command); //İsteğimizi söylüyoruz.

            String response = reader.readLine(); //Asıl cevabı okuyoruz.
            return response;

        } catch (Exception e) {
            return "BAĞLANTI HATASI: " + e.getMessage();
        }
    }
}