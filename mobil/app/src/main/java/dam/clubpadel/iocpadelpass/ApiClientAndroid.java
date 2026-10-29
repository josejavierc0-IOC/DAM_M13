package dam.clubpadel.iocpadelpass;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClientAndroid {

    private static final String BASE_URL =
            "http://10.0.2.2/DAM_M13/servidor/index.php";

    /**
     * Iniciar sessió.
     */
    public static JSONObject iniciarSessio(
            String email,
            String password
    ) throws Exception {

        JSONObject dades = new JSONObject();
        dades.put("email", email);
        dades.put("password", password);

        return enviarPost("login", dades);
    }

    /**
     * Registrar un usuari nou.
     */
    public static JSONObject registrarUsuari(
            String nom,
            String cognoms,
            String email,
            String password,
            String telefon
    ) throws Exception {

        JSONObject dades = new JSONObject();
        dades.put("nom", nom);
        dades.put("cognoms", cognoms);
        dades.put("email", email);
        dades.put("password", password);

        if (telefon != null && !telefon.trim().isEmpty()) {
            dades.put("telefon", telefon.trim());
        }

        return enviarPost("usuaris", dades);
    }

    /**
     * Sol·licitar la recuperació de contrasenya.
     */
    public static JSONObject recuperarContrasenya(
            String email
    ) throws Exception {

        JSONObject dades = new JSONObject();
        dades.put("email", email);

        return enviarPost("recuperar-contrasenya", dades);
    }

    /**
     * Restablir la contrasenya mitjançant el token de recuperació.
     */
    public static JSONObject restablirContrasenya(
            String token,
            String passwordNova
    ) throws Exception {

        JSONObject dades = new JSONObject();
        dades.put("token", token);
        dades.put("password_nova", passwordNova);

        return enviarPost("restablir-contrasenya", dades);
    }

    /**
     * Consultar les pistes del club.
     *
     * La petició necessita el token de sessió de l'usuari.
     */
    public static JSONObject llistarPistes(
            String token
    ) throws Exception {

        return enviarGet("pistes", token);
    }

    /**
     * Enviar una petició GET autenticada al servidor.
     */
    private static JSONObject enviarGet(
            String recurs,
            String token
    ) throws Exception {

        HttpURLConnection connexio = null;

        try {
            URL url = new URL(BASE_URL + "?resource=" + recurs);

            connexio = (HttpURLConnection) url.openConnection();
            connexio.setRequestMethod("GET");
            connexio.setConnectTimeout(10000);
            connexio.setReadTimeout(10000);

            connexio.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            if (token != null && !token.trim().isEmpty()) {
                connexio.setRequestProperty(
                        "Authorization",
                        "Bearer " + token.trim()
                );
            }

            return llegirResposta(connexio);

        } finally {
            if (connexio != null) {
                connexio.disconnect();
            }
        }
    }

    /**
     * Mètode compartit per enviar peticions POST al servidor.
     */
    private static JSONObject enviarPost(
            String recurs,
            JSONObject dades
    ) throws Exception {

        HttpURLConnection connexio = null;

        try {
            URL url = new URL(BASE_URL + "?resource=" + recurs);

            connexio = (HttpURLConnection) url.openConnection();
            connexio.setRequestMethod("POST");
            connexio.setConnectTimeout(10000);
            connexio.setReadTimeout(10000);
            connexio.setDoOutput(true);

            connexio.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connexio.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            byte[] cos = dades.toString().getBytes(
                    StandardCharsets.UTF_8
            );

            try (OutputStream sortida = connexio.getOutputStream()) {
                sortida.write(cos);
                sortida.flush();
            }

            return llegirResposta(connexio);

        } finally {
            if (connexio != null) {
                connexio.disconnect();
            }
        }
    }

    /**
     * Llegir i convertir la resposta HTTP a un JSONObject.
     *
     * També llegeix les respostes d'error del servidor perquè
     * l'aplicació pugui mostrar-ne el missatge.
     */
    private static JSONObject llegirResposta(
            HttpURLConnection connexio
    ) throws Exception {

        int codiResposta = connexio.getResponseCode();

        InputStream flux;

        if (codiResposta >= 200 && codiResposta < 400) {
            flux = connexio.getInputStream();
        } else {
            flux = connexio.getErrorStream();
        }

        if (flux == null) {
            throw new Exception(
                    "El servidor no ha retornat cap resposta. "
                            + "Codi HTTP: " + codiResposta
            );
        }

        StringBuilder resposta = new StringBuilder();

        try (
                BufferedReader lector = new BufferedReader(
                        new InputStreamReader(
                                flux,
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            String linia;

            while ((linia = lector.readLine()) != null) {
                resposta.append(linia);
            }
        }

        JSONObject resultat;

        try {
            resultat = new JSONObject(resposta.toString());
        } catch (Exception e) {
            throw new Exception(
                    "La resposta del servidor no és un JSON vàlid. "
                            + "Codi HTTP: " + codiResposta
                            + ". Resposta: " + resposta
            );
        }

        /*
         * Afegim el codi HTTP a la resposta per poder identificar
         * errors d'autenticació o altres errors HTTP.
         */
        resultat.put("_http_code", codiResposta);

        return resultat;
    }
}