package com.mycompany.diskstoresystem;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

/**
 * HaToKuSe TCP Client (text protocol)
 *
 * Sends SET/GET commands over a persistent TCP connection.
 * Measures per-request round-trip time (from send to response read).
 *
 * Protocol examples:
 *   SET 34 ISTANBUL\n
 *   GET 34\n
 */
public class HaToKuSeClient {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

    private static final class Config {
        String host = "127.0.0.1";
        int port = 6666;
        int durationMinutes = 10;

        // Workload
        double setRatio = 0.80;      // 0..1
        int keySpace = 10_000;       // keys 0..keySpace-1
        int sleepMsBetweenOps = 0;   // pacing

        // Payload sizes
        int minPayloadBytes = 5;     // for SET
        int maxPayloadBytes = 2_000; // typical max
        int largePayloadBytes = 1_000_000; // 1MB
        int largeEveryN = 200;       // every N SETs send a large payload

        // Output
        String csvPath = "hatokuse_client_metrics.csv";
        boolean printEach = false;

        // Networking
        int connectTimeoutMs = 5000;
        int readTimeoutMs = 15000;
        boolean reconnectOnFailure = true;
        int reconnectBackoffMs = 500;
    }

    public static void main(String[] args) throws Exception {
        Config cfg = parseArgs(args);

        long endAt = System.nanoTime() + (long) cfg.durationMinutes * 60L * 1_000_000_000L;
        Random rnd = new Random();

        System.out.println("HaToKuSeClient starting...");
        System.out.printf("Target: %s:%d | duration=%d min | setRatio=%.2f | keySpace=%d%n",
                cfg.host, cfg.port, cfg.durationMinutes, cfg.setRatio, cfg.keySpace);
        System.out.printf("Payload: min=%dB max=%dB | large=%dB every %d SETs%n",
                cfg.minPayloadBytes, cfg.maxPayloadBytes, cfg.largePayloadBytes, cfg.largeEveryN);
        System.out.printf("CSV: %s%n", cfg.csvPath);

        // CSV header
        try (PrintWriter csv = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cfg.csvPath, false), StandardCharsets.UTF_8))) {
            csv.println("ts,op,key,payload_bytes,ok,rtt_ms,response");
        }

        long opCount = 0;
        long setCount = 0;
        long okCount = 0;
        long errCount = 0;

        // Simple stats
        long rttSum = 0;
        long rttMin = Long.MAX_VALUE;
        long rttMax = Long.MIN_VALUE;

        Connection conn = null;
        try {
            conn = Connection.connect(cfg);

            while (System.nanoTime() < endAt) {
                opCount++;

                boolean doSet = rnd.nextDouble() < cfg.setRatio;
                int key = rnd.nextInt(Math.max(1, cfg.keySpace));

                String op;
                String request;
                int payloadBytes = 0;

                if (doSet) {
                    op = "SET";
                    setCount++;

                    boolean isLarge = (cfg.largeEveryN > 0) && (setCount % cfg.largeEveryN == 0);
                    payloadBytes = isLarge
                            ? cfg.largePayloadBytes
                            : randomBetween(rnd, cfg.minPayloadBytes, cfg.maxPayloadBytes);

                    String value = randomAscii(rnd, payloadBytes);
                    request = "SET " + key + " " + value;
                } else {
                    op = "GET";
                    request = "GET " + key;
                }

                String ts = TS_FMT.format(Instant.now());

                long startNs = System.nanoTime();
                String response;
                boolean ok;

                try {
                    response = conn.sendAndReadLine(request);
                    long rttMs = (System.nanoTime() - startNs) / 1_000_000L;

                    ok = response != null && response.startsWith("OK");
                    if (ok) okCount++; else errCount++;

                    rttSum += rttMs;
                    rttMin = Math.min(rttMin, rttMs);
                    rttMax = Math.max(rttMax, rttMs);

                    appendCsv(cfg.csvPath, ts, op, key, payloadBytes, ok, rttMs, response);

                    if (cfg.printEach) {
                        System.out.printf("%s | %s %d (%dB) -> %s | rtt=%dms%n",
                                ts, op, key, payloadBytes, response, rttMs);
                    }

                } catch (IOException e) {
                    // Record as ERROR and optionally reconnect
                    long rttMs = (System.nanoTime() - startNs) / 1_000_000L;
                    errCount++;
                    String err = "ERROR " + e.getClass().getSimpleName() + ":" + safeMsg(e.getMessage());
                    appendCsv(cfg.csvPath, ts, op, key, payloadBytes, false, rttMs, err);
                    if (cfg.printEach) {
                        System.out.printf("%s | %s %d (%dB) -> %s | rtt=%dms%n",
                                ts, op, key, payloadBytes, err, rttMs);
                    }

                    if (cfg.reconnectOnFailure) {
                        closeQuietly(conn);
                        Thread.sleep(cfg.reconnectBackoffMs);
                        conn = Connection.connect(cfg);
                    } else {
                        throw e;
                    }
                }

                if (cfg.sleepMsBetweenOps > 0) {
                    Thread.sleep(cfg.sleepMsBetweenOps);
                }

                // Periodic summary every 1000 ops
                if (opCount % 1000 == 0) {
                    long avg = (opCount == 0) ? 0 : (rttSum / Math.max(1, (okCount + errCount)));
                    System.out.printf("ops=%d (SET=%d) OK=%d ERROR=%d | rtt(ms) min=%d avg=%d max=%d%n",
                            opCount, setCount, okCount, errCount,
                            (rttMin == Long.MAX_VALUE ? 0 : rttMin), avg,
                            (rttMax == Long.MIN_VALUE ? 0 : rttMax));
                }
            }

        } finally {
            closeQuietly(conn);
        }

        long total = okCount + errCount;
        long avg = total == 0 ? 0 : (rttSum / total);

        System.out.println("\nDone.");
        System.out.printf("Total ops=%d | SET=%d | OK=%d | ERROR=%d%n", opCount, setCount, okCount, errCount);
        System.out.printf("RTT(ms): min=%d avg=%d max=%d%n",
                (rttMin == Long.MAX_VALUE ? 0 : rttMin), avg, (rttMax == Long.MIN_VALUE ? 0 : rttMax));
        System.out.printf("CSV written: %s%n", cfg.csvPath);
    }

    private static void appendCsv(String csvPath, String ts, String op, int key, int payloadBytes,
                                  boolean ok, long rttMs, String response) {
        // Append per line (simple + safe). If you want max throughput, keep an open writer.
        try (PrintWriter csv = new PrintWriter(new OutputStreamWriter(new FileOutputStream(csvPath, true), StandardCharsets.UTF_8))) {
            csv.printf("%s,%s,%d,%d,%s,%d,%s%n",
                    csvEscape(ts), op, key, payloadBytes, ok ? "1" : "0", rttMs, csvEscape(response));
        } catch (IOException ignored) {
            // If CSV can't be written, we still continue the benchmark.
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        String t = s.replace("\r", " ").replace("\n", " ");
        // quote if contains comma or quote
        if (t.contains(",") || t.contains("\"") ) {
            t = t.replace("\"", "\"\"");
            return "\"" + t + "\"";
        }
        return t;
    }

    private static String safeMsg(String s) {
        if (s == null) return "";
        return s.replace("\r", " ").replace("\n", " ");
    }

    private static int randomBetween(Random rnd, int minInclusive, int maxInclusive) {
        if (maxInclusive < minInclusive) {
            int tmp = minInclusive; minInclusive = maxInclusive; maxInclusive = tmp;
        }
        if (minInclusive == maxInclusive) return minInclusive;
        return minInclusive + rnd.nextInt(maxInclusive - minInclusive + 1);
    }

    /** Generates an ASCII string of EXACT byte length in UTF-8 (ASCII subset). */
    private static String randomAscii(Random rnd, int bytes) {
        if (bytes <= 0) return "";
        char[] chars = new char[bytes];
        for (int i = 0; i < bytes; i++) {
            // printable ASCII excluding spaces? keep it simple: [A-Z0-9]
            int r = rnd.nextInt(36);
            chars[i] = (r < 10) ? (char)('0' + r) : (char)('A' + (r - 10));
        }
        return new String(chars);
    }

    private static void closeQuietly(Connection c) {
        if (c == null) return;
        try { c.close(); } catch (Exception ignored) {}
    }

    private static final class Connection implements Closeable {
        private final Socket socket;
        private final BufferedWriter out;
        private final BufferedReader in;

        private Connection(Socket socket, BufferedWriter out, BufferedReader in) {
            this.socket = socket;
            this.out = out;
            this.in = in;
        }

        static Connection connect(Config cfg) throws IOException {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(cfg.host, cfg.port), cfg.connectTimeoutMs);
            s.setSoTimeout(cfg.readTimeoutMs);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            return new Connection(s, out, in);
        }

        String sendAndReadLine(String line) throws IOException {
            out.write(line);
            out.write("\n");
            out.flush();
            String resp = in.readLine();
            if (resp == null) throw new EOFException("server closed connection");
            return resp;
        }

        @Override
        public void close() throws IOException {
            try { out.close(); } catch (IOException ignored) {}
            try { in.close(); } catch (IOException ignored) {}
            socket.close();
        }
    }

    private static Config parseArgs(String[] args) {
        Config c = new Config();
        for (String a : args) {
            if (a.startsWith("--host=")) c.host = a.substring("--host=".length());
            else if (a.startsWith("--port=")) c.port = Integer.parseInt(a.substring("--port=".length()));
            else if (a.startsWith("--durationMinutes=")) c.durationMinutes = Integer.parseInt(a.substring("--durationMinutes=".length()));
            else if (a.startsWith("--setRatio=")) c.setRatio = Double.parseDouble(a.substring("--setRatio=".length()));
            else if (a.startsWith("--keySpace=")) c.keySpace = Integer.parseInt(a.substring("--keySpace=".length()));
            else if (a.startsWith("--sleepMs=")) c.sleepMsBetweenOps = Integer.parseInt(a.substring("--sleepMs=".length()));
            else if (a.startsWith("--minPayloadBytes=")) c.minPayloadBytes = Integer.parseInt(a.substring("--minPayloadBytes=".length()));
            else if (a.startsWith("--maxPayloadBytes=")) c.maxPayloadBytes = Integer.parseInt(a.substring("--maxPayloadBytes=".length()));
            else if (a.startsWith("--largePayloadBytes=")) c.largePayloadBytes = Integer.parseInt(a.substring("--largePayloadBytes=".length()));
            else if (a.startsWith("--largeEveryN=")) c.largeEveryN = Integer.parseInt(a.substring("--largeEveryN=".length()));
            else if (a.startsWith("--csv=")) c.csvPath = a.substring("--csv=".length());
            else if (a.equals("--printEach")) c.printEach = true;
            else if (a.startsWith("--connectTimeoutMs=")) c.connectTimeoutMs = Integer.parseInt(a.substring("--connectTimeoutMs=".length()));
            else if (a.startsWith("--readTimeoutMs=")) c.readTimeoutMs = Integer.parseInt(a.substring("--readTimeoutMs=".length()));
            else if (a.equals("--noReconnect")) c.reconnectOnFailure = false;
            else if (a.startsWith("--reconnectBackoffMs=")) c.reconnectBackoffMs = Integer.parseInt(a.substring("--reconnectBackoffMs=".length()));
        }
        // sanity
        if (c.setRatio < 0) c.setRatio = 0;
        if (c.setRatio > 1) c.setRatio = 1;
        if (c.durationMinutes < 1) c.durationMinutes = 1;
        if (c.keySpace < 1) c.keySpace = 1;
        return c;
    }
}
