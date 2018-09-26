package de.muffincrafter.jabcischool.client;

import org.json.JSONObject;

import java.util.Base64;

public class JSONTools {
    public static Certificate convertResponseToCertificate(String response) {
        JSONObject obj = new JSONObject(response);
        JSONObject response_obj = obj.getJSONObject("result").getJSONObject("response");

        String log = response_obj.getString("log");
        if (!log.equals("exists")) {
            return null;
        }

        String valueEnc = response_obj.getString("value");
        String value = new String(Base64.getDecoder().decode(valueEnc));

        JSONObject certificate_obj = new JSONObject(value);
        return Certificate.certificateFromJSON(certificate_obj);
    }

    public static boolean checkResponse(String response) {
        JSONObject obj = new JSONObject(response);
        if (obj.has("error")) {
            return false;
        }
        return true;
    }
}
