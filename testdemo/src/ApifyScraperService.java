import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApifyScraperService {

    private static final String YOUR_TOKEN = "";
    private static final String API_URL_TEMPLATE =
        "https://api.apify.com/v2/acts/curious_coder~google-play-scraper/run-sync-get-dataset-items?token=%s";
    private static final String API_URL = String.format(API_URL_TEMPLATE, YOUR_TOKEN);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(180))
        .build();

    private static JsonNode executeApiCall(String jsonInput) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
            .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("API Error: Status code " + response.statusCode() + " received. Body: " + response.body());
        }
        return OBJECT_MAPPER.readTree(response.body());
    }
    
    private static String jsonEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    private static String findAppId(JsonNode item) {
        String[] potentialFields = {"appId", "id", "app_id", "packageId", "package_name"};
        String foundAppId = null;
        
        for (String field : potentialFields) {
            if (item.has(field) && item.get(field).isTextual()) {
                String value = item.get(field).asText();
                if (!value.isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
                    foundAppId = value;
                    break;
                }
            }
        }
        
        if (foundAppId == null && item.has("url") && item.get("url").isTextual()) {
            String url = item.get("url").asText();
            int idIndex = url.lastIndexOf("id=");
            if (idIndex != -1) {
                String appId = url.substring(idIndex + 3);
                
                int paramIndex = appId.indexOf('&');
                if (paramIndex != -1) {
                    appId = appId.substring(0, paramIndex);
                }
                
                if (!appId.isEmpty()) {
                    foundAppId = appId;
                }
            }
        }

        return (foundAppId != null) ? foundAppId.trim() : null;
    }

    public static String callApifyApi(final String appName) throws IOException, InterruptedException {

        String safeAppName = jsonEscape(appName);
        
        String searchInput = String.format(
            "{\"action\": \"scrapeAppSearch\", \"count\": 4, \"maxItems\": 4, \"scrapeAppSearch.keywords\": [\"%s\"]}",
            safeAppName
        );
        
        JsonNode searchResult;
        try {
            searchResult = executeApiCall(searchInput);
        } catch (IOException e) {
            return "❌ Error in search step: " + e.getMessage();
        }
        
        if (searchResult.isArray() && searchResult.size() > 0) {
            JsonNode searchItem = searchResult.get(0);
            
            String appId = findAppId(searchItem);
            
            if (appId == null) {
                return "⚠️ Search found an app, but could not determine its App ID. The package identifier was not found in any expected field (including the URL).";
            }
            
            String safeAppId = jsonEscape(appId);

            Thread.sleep(2000); 

            String appUrl = "https://play.google.com/store/apps/details?id=" + safeAppId;

            String detailInput = String.format(
                "{\"action\": \"scrapeAppUrls\", \"scrapeAppUrls.urls\": [\"%s\"], \"scrapeAppUrls.language\": \"en\", \"maxItems\": 1}",
                appUrl
            );

            JsonNode detailResult;
            try {
                detailResult = executeApiCall(detailInput);
            } catch (IOException e) {
                return "❌ Error in detail scrape step: " + e.getMessage();
            }

            if (detailResult.isArray() && detailResult.size() > 0) {
                JsonNode item = detailResult.get(0);
                
                String title = item.has("title") ? item.get("title").asText() : "N/A";
                String developer = item.has("developer") ? item.get("developer").asText() : "N/A";
                String genre = item.has("genre") ? item.get("genre").asText() : "N/A";
                String installs = item.has("installs") ? item.get("installs").asText() : "N/A";
                double score = item.has("score") ? item.get("score").asDouble() : 0.0;
                String summary = item.has("summary") ? item.get("summary").asText() : "No summary available.";

                return String.format(
                    "✅ App Data Fetched Successfully for: %s\n\n" +
                    "--------------------------------------------------\n" +
                    " Package ID: %s\n" +
                    " Developer: %s\n" +
                    " Genre: %s\n" +
                    " Installs: %s\n" +
                    " Rating: %.1f / 5.0\n" +
                    "--------------------------------------------------\n" +
                    " Summary:\n%s...",
                    title,
                    appId,
                    developer,
                    genre,
                    installs,
                    score,
                    summary.substring(0, Math.min(summary.length(), 250)) 
                );
            } else {
                return "⚠️ Found App ID, but failed to retrieve full details for " + appId;
            }
        } else {
            return "⚠️ Search returned no results for '" + appName + "'.";
        }
    }
}