
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ApiClient {

    private static final String BASE_URL =
            "http://localhost/DAM_M13/servidor/index.php";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public ApiClient() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        mapper = new ObjectMapper();
    }

    public JsonNode iniciarSessio(String email, String password)
            throws IOException, InterruptedException {
        ObjectNode dades = mapper.createObjectNode();
        dades.put("email", email);
        dades.put("password", password);
        return enviar("POST", "login", null, dades);
    }

    public JsonNode registrarUsuari(
            String nom, String cognoms, String email,
            String password, String telefon)
            throws IOException, InterruptedException {

        ObjectNode dades = mapper.createObjectNode();
        dades.put("nom", nom);
        dades.put("cognoms", cognoms);
        dades.put("email", email);
        dades.put("password", password);
        dades.put("telefon", telefon);

        return enviar("POST", "usuaris", null, dades);
    }

    public JsonNode canviarContrasenya(
            String token, String actual, String nova)
            throws IOException, InterruptedException {

        ObjectNode dades = mapper.createObjectNode();
        dades.put("password_actual", actual);
        dades.put("password_nova", nova);

        return enviar("POST", "canviar-contrasenya", token, dades);
    }

    public JsonNode sollicitarRecuperacio(String email)
            throws IOException, InterruptedException {
        ObjectNode dades = mapper.createObjectNode();
        dades.put("email", email);
        return enviar("POST", "recuperar-contrasenya", null, dades);
    }

    public JsonNode restablirContrasenya(String tokenRecuperacio,
                                         String passwordNova)
            throws IOException, InterruptedException {
        ObjectNode dades = mapper.createObjectNode();
        dades.put("token", tokenRecuperacio);
        dades.put("password_nova", passwordNova);

        return enviar("POST", "restablir-contrasenya", null, dades);
    }

    public JsonNode get(String recurs, String token)
            throws IOException, InterruptedException {
        return enviar("GET", recurs, token, null);
    }

    public JsonNode post(String recurs, String token, JsonNode dades)
            throws IOException, InterruptedException {
        return enviar("POST", recurs, token, dades);
    }

    public JsonNode put(String recurs, String token, JsonNode dades)
            throws IOException, InterruptedException {
        return enviar("PUT", recurs, token, dades);
    }

    public JsonNode patch(String recurs, String token)
            throws IOException, InterruptedException {
        return enviar("PATCH", recurs, token, mapper.createObjectNode());
    }

    public JsonNode patch(String recurs, String token, JsonNode dades)
            throws IOException, InterruptedException {
        return enviar("PATCH", recurs, token, dades);
    }

    public JsonNode delete(String recurs, String token)
            throws IOException, InterruptedException {
        return enviar("DELETE", recurs, token, null);
    }

    private JsonNode enviar(String metode, String recurs, String token,
                            JsonNode dades)
            throws IOException, InterruptedException {

        String url = BASE_URL + "?resource=" + recurs;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        if (dades != null) {
            builder.header("Content-Type", "application/json");
            builder.method(metode, HttpRequest.BodyPublishers.ofString(
                    mapper.writeValueAsString(dades)));
        } else if ("GET".equals(metode)) {
            builder.GET();
        } else if ("DELETE".equals(metode)) {
            builder.DELETE();
        } else {
            builder.method(metode,
                    HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> resposta = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());

        String cos = resposta.body();

        if (cos == null || cos.isBlank()) {
            throw new IOException(
                    "El servidor ha retornat una resposta buida (HTTP "
                            + resposta.statusCode() + ").");
        }

        JsonNode resultat;
        try {
            resultat = mapper.readTree(cos);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IOException(
                    "La resposta del servidor no és JSON vàlid (HTTP "
                            + resposta.statusCode() + ").", ex);
        }

        if (resposta.statusCode() < 200
                || resposta.statusCode() >= 300) {
            if (resultat != null
                    && resultat.hasNonNull("message")) {
                return resultat;
            }
            throw new IOException(
                    "Error HTTP " + resposta.statusCode());
        }

        return resultat;
    }
}
