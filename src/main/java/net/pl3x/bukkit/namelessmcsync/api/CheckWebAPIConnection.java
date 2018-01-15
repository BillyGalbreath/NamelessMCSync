package net.pl3x.bukkit.namelessmcsync.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.pl3x.bukkit.namelessmcsync.NamelessMCSync;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckWebAPIConnection {
    public boolean succeeded = false;
    public boolean error = true;
    public String errorMessage;

    public CheckWebAPIConnection(final NamelessMCSync plugin) {
        try {
            URL apiConnection = new URL(String.valueOf(plugin.apiURL) + "/checkConnection");
            HttpURLConnection connection = (HttpURLConnection) apiConnection.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Length", Integer.toString(0));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes("");
            InputStream inputStream = connection.getInputStream();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseBuilder = new StringBuilder();
            String responseString;
            while ((responseString = streamReader.readLine()) != null) {
                responseBuilder.append(responseString);
            }
            JsonObject response = new JsonObject();
            JsonParser parser = new JsonParser();
            response = parser.parse(responseBuilder.toString()).getAsJsonObject();
            if (response.has("success") || response.get("message").getAsString().equalsIgnoreCase("Invalid API method")) {
                error = false;
                succeeded = true;
            } else if (response.has("error")) {
                error = true;
                succeeded = false;
                errorMessage = response.get("message").getAsString();
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            connection.disconnect();
        } catch (Exception e) {
            errorMessage = "Invalid API key";
            plugin.getLogger().severe("Invalid API key");
            e.printStackTrace();
        }
    }
}
