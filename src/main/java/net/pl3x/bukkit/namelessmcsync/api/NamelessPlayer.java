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
import java.net.URLEncoder;

public class NamelessPlayer {
    public String id;
    public String uuid;

    public int groupID;
    public boolean exists;
    public boolean validated;
    public boolean banned;

    public boolean error;
    public String errorMessage;

    public NamelessPlayer(String id, NamelessMCSync plugin) {
        this.id = id;
        try {
            String toPostStringUUID = "uuid=" + URLEncoder.encode(id, "UTF-8");
            URL apiConnection = new URL(String.valueOf(plugin.apiURL) + "/get");
            HttpURLConnection connection = (HttpURLConnection) apiConnection.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Length", Integer.toString(toPostStringUUID.length()));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(toPostStringUUID);
            InputStream inputStream = connection.getInputStream();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseBuilder = new StringBuilder();
            String responseString;
            while ((responseString = streamReader.readLine()) != null) {
                responseBuilder.append(responseString);
            }
            JsonParser parser = new JsonParser();
            JsonObject response = new JsonObject();
            JsonObject message = new JsonObject();
            response = parser.parse(responseBuilder.toString()).getAsJsonObject();
            System.out.println("RESPONSE: " + responseString);
            if (!response.has("error")) {
                message = parser.parse(response.get("message").getAsString()).getAsJsonObject();
            }
            if (response.has("error")) {
                exists = false;
                error = true;
                errorMessage = response.get("message").getAsString();
            } else {
                exists = true;
                error = false;
                uuid = message.get("uuid").getAsString();
                groupID = message.get("group_id").getAsInt();
                validated = message.get("validated").getAsString().equals("1");
                banned = message.get("banned").getAsString().equals("1");
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            connection.disconnect();
        } catch (Exception e) {
            plugin.getLogger().severe("There was an unknown error whilst getting player.");
            e.printStackTrace();
        }
    }
}
