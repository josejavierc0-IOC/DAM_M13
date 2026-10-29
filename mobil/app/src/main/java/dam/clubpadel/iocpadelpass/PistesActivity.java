package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PistesActivity extends Activity {

    private LinearLayout llistaPistes;
    private TextView missatge;
    private ProgressBar progressBar;
    private Button btnActualitzar;

    private SharedPreferences preferences;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                "IOCPadelPass",
                MODE_PRIVATE
        );

        String token = preferences.getString("token", "");

        if (token.isEmpty()) {
            tornarAlLogin();
            return;
        }

        crearInterficie();
        carregarPistes();
    }

    /**
     * Crear la interfície de consulta de pistes.
     */
    private void crearInterficie() {

        LinearLayout principal = new LinearLayout(this);
        principal.setOrientation(LinearLayout.VERTICAL);
        principal.setPadding(24, 24, 24, 24);
        principal.setBackgroundColor(Color.rgb(23, 26, 25));

        TextView titol = new TextView(this);
        titol.setText("Pistes del club");
        titol.setTextSize(28);
        titol.setTypeface(null, Typeface.BOLD);
        titol.setTextColor(Color.rgb(102, 187, 106));
        titol.setGravity(Gravity.CENTER);
        titol.setPadding(0, 8, 0, 24);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        LinearLayout contenidorProgress =
                new LinearLayout(this);

        contenidorProgress.setGravity(Gravity.CENTER);
        contenidorProgress.addView(progressBar);

        missatge = new TextView(this);
        missatge.setTextColor(Color.WHITE);
        missatge.setTextSize(16);
        missatge.setGravity(Gravity.CENTER);
        missatge.setPadding(0, 16, 0, 16);

        btnActualitzar = new Button(this);
        btnActualitzar.setText("Actualitzar pistes");

        Button btnTornar = new Button(this);
        btnTornar.setText("Tornar a l'inici");

        llistaPistes = new LinearLayout(this);
        llistaPistes.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.addView(llistaPistes);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);

        principal.addView(titol, params);
        principal.addView(contenidorProgress, params);
        principal.addView(missatge, params);
        principal.addView(btnActualitzar, params);

        principal.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        principal.addView(btnTornar, params);

        btnActualitzar.setOnClickListener(v -> carregarPistes());

        btnTornar.setOnClickListener(v -> finish());

        setContentView(principal);
    }

    /**
     * Consultar les pistes al servidor en segon pla.
     */
    private void carregarPistes() {

        String token = preferences.getString("token", "");

        if (token.isEmpty()) {
            tornarAlLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnActualitzar.setEnabled(false);
        missatge.setText("Carregant pistes...");
        llistaPistes.removeAllViews();

        executor.execute(() -> {

            JSONObject resposta = null;
            Exception error = null;

            try {
                resposta = ApiClientAndroid.llistarPistes(token);
            } catch (Exception e) {
                error = e;
            }

            final JSONObject respostaFinal = resposta;
            final Exception errorFinal = error;

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                progressBar.setVisibility(View.GONE);
                btnActualitzar.setEnabled(true);

                if (errorFinal != null) {
                    missatge.setText(
                            "No s'han pogut carregar les pistes.\n"
                                    + obtenirMissatgeError(errorFinal)
                    );
                    return;
                }

                if (respostaFinal == null) {
                    missatge.setText(
                            "El servidor no ha retornat cap resposta."
                    );
                    return;
                }

                int codiHttp = respostaFinal.optInt(
                        "_http_code",
                        200
                );

                if (codiHttp == 401 || codiHttp == 403) {
                    missatge.setText(
                            "La sessió ha caducat o no tens permís "
                                    + "per consultar les pistes. "
                                    + "Torna a iniciar sessió."
                    );
                    return;
                }

                if (!respostaFinal.optBoolean("success", false)) {
                    String missatgeServidor =
                            respostaFinal.optString(
                                    "message",
                                    "No s'han pogut obtenir les pistes."
                            );

                    missatge.setText(missatgeServidor);
                    return;
                }

                JSONArray pistes =
                        respostaFinal.optJSONArray("pistes");

                if (pistes == null || pistes.length() == 0) {
                    missatge.setText(
                            "No hi ha pistes per mostrar."
                    );
                    return;
                }

                missatge.setText(
                        "Pistes trobades: " + pistes.length()
                );

                mostrarPistes(pistes);
            });
        });
    }

    /**
     * Mostrar les pistes retornades per l'API.
     */
    private void mostrarPistes(JSONArray pistes) {

        llistaPistes.removeAllViews();

        for (int i = 0; i < pistes.length(); i++) {

            JSONObject pista = pistes.optJSONObject(i);

            if (pista == null) {
                continue;
            }

            int id = pista.optInt("id", 0);

            String nom = pista.optString(
                    "nom",
                    "Pista sense nom"
            );

            String tipus = pista.optString(
                    "tipus",
                    "No especificat"
            );

            String descripcio = pista.optString(
                    "descripcio",
                    ""
            );

            String activa = obtenirEstatActiva(pista);

            LinearLayout targeta = new LinearLayout(this);
            targeta.setOrientation(LinearLayout.VERTICAL);
            targeta.setPadding(20, 16, 20, 16);
            targeta.setBackgroundColor(Color.rgb(39, 45, 41));

            LinearLayout.LayoutParams paramsTargeta =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            paramsTargeta.setMargins(0, 8, 0, 8);

            TextView nomPista = new TextView(this);
            nomPista.setText(nom);
            nomPista.setTextSize(21);
            nomPista.setTypeface(null, Typeface.BOLD);
            nomPista.setTextColor(Color.rgb(102, 187, 106));

            TextView idPista = new TextView(this);
            idPista.setText("Identificador: " + id);
            idPista.setTextSize(14);
            idPista.setTextColor(Color.LTGRAY);

            TextView tipusPista = new TextView(this);
            tipusPista.setText("Tipus: " + tipus);
            tipusPista.setTextSize(16);
            tipusPista.setTextColor(Color.WHITE);

            TextView estatPista = new TextView(this);
            estatPista.setText("Estat: " + activa);
            estatPista.setTextSize(16);
            estatPista.setTextColor(Color.WHITE);

            targeta.addView(nomPista);
            targeta.addView(idPista);
            targeta.addView(tipusPista);
            targeta.addView(estatPista);

            if (!descripcio.trim().isEmpty()
                    && !descripcio.equals("null")) {

                TextView descripcioPista = new TextView(this);
                descripcioPista.setText(
                        "Descripció: " + descripcio
                );
                descripcioPista.setTextSize(15);
                descripcioPista.setTextColor(Color.LTGRAY);
                descripcioPista.setPadding(0, 8, 0, 0);

                targeta.addView(descripcioPista);
            }

            llistaPistes.addView(targeta, paramsTargeta);
        }

        if (llistaPistes.getChildCount() == 0) {
            missatge.setText(
                    "La resposta no conté pistes que es puguin mostrar."
            );
        }
    }

    /**
     * Interpretar el camp "activa", que pot arribar com a booleà,
     * número o text, segons com el retorni el servidor.
     */
    private String obtenirEstatActiva(JSONObject pista) {

        Object valor = pista.opt("activa");

        if (valor == null || valor == JSONObject.NULL) {
            return "No especificat";
        }

        String text = String.valueOf(valor).trim();

        if ("1".equals(text)
                || "true".equalsIgnoreCase(text)
                || "si".equalsIgnoreCase(text)
                || "sí".equalsIgnoreCase(text)) {
            return "Activa";
        }

        if ("0".equals(text)
                || "false".equalsIgnoreCase(text)
                || "no".equalsIgnoreCase(text)) {
            return "Inactiva";
        }

        return text;
    }

    /**
     * Preparar un missatge d'error llegible.
     */
    private String obtenirMissatgeError(Exception error) {

        String missatgeError = error.getMessage();

        if (missatgeError == null || missatgeError.trim().isEmpty()) {
            return "Comprova la connexió amb el servidor.";
        }

        return missatgeError;
    }

    /**
     * Tornar a la pantalla d'inici de sessió si no hi ha token.
     */
    private void tornarAlLogin() {

        android.content.Intent intent = new android.content.Intent(
                this,
                MainActivityJava.class
        );

        intent.setFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executor.shutdownNow();
    }
}