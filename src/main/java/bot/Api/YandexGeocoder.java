package bot.Api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class YandexGeocoder {
    private static final String API_KEY = "013229c4-77e7-402a-8098-3d63c306c394";
    public static String getAddress(float latitude, float longitude) {
        try {
            String geocode = longitude + "," + latitude;
            String urlString = "https://geocode-maps.yandex.ru/1.x/?apikey=" + API_KEY + "&geocode=" + geocode + "&format=json";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject geoObject = jsonResponse.getJSONObject("response")
                    .getJSONObject("GeoObjectCollection")
                    .getJSONArray("featureMember")
                    .getJSONObject(0)
                    .getJSONObject("GeoObject");
            String address = geoObject.getJSONObject("metaDataProperty")
                    .getJSONObject("GeocoderMetaData")
                    .getString("text");
            return address;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}


