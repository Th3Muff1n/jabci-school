package de.muffincrafter.jabcischool.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

public class Client {
    private static String sendBroadcastTx(String tx) throws IOException {
        String url = "http://localhost:26657/broadcast_tx_commit?tx=\"" + tx + "\"";
        return sendGet(url);
    }

    private static String sendQuery(String data) throws IOException {
        String url = "http://localhost:26657/abci_query?data=\"" + data + "\"";
        return sendGet(url);
    }

    private static String sendGet(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        //int responseCode = con.getResponseCode();
        //System.out.printf("%nSending 'GET' request to URL : %s%n", url);
        //System.out.printf("Response Code : %d%n", responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //System.out.println(response.toString());

        return response.toString();
    }

    private static void createCertificate() throws IOException {
        System.out.println();
        Certificate test = Certificate.buildCertificate();
        String response = sendBroadcastTx(test.getJSONString());
        //String response = sendBroadcastTx("e");
        if (JSONTools.checkResponse(response)) {
            System.out.println("Zeugnis eingetragen");
        } else {
            System.out.println("Fehler beim eintragen");
        }
    }

    private static void findCertificate(Scanner scanner) throws IOException {
        System.out.print("\nName : ");
        String student_name = scanner.nextLine();
        String response = sendQuery(student_name);
        Certificate certificate = JSONTools.convertResponseToCertificate(response);
        if (certificate == null) {
            System.out.printf("Zeugnis von %s nicht gefunden%n", student_name);
        } else {
            certificate.printCertificate();
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Mögliche Aktionen: create, find, exit");
        while (true) {
            System.out.println();
            String input;
            switch ((input = scanner.nextLine())) {
                case "create":
                    createCertificate();
                    break;
                case "find":
                    findCertificate(scanner);
                    break;
                case "exit":
                    System.exit(0);
                    break;
                default:
                    System.out.printf("Aktion \"%s\" nicht gefunden\n", input);
            }
        }
    }
}
