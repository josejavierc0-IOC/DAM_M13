package dam.clubpadel.iocpadelpass;

import android.app.Activity;
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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TarifesActivity extends Activity {

    private static final String BASE_URL =
            "http://10.0.2.2/DAM_M13/servidor/index.php";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private LinearLayout llista;
    private ProgressBar progress;
    private String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        token = getSharedPreferences(
                "IOCPadelPass",
                MODE_PRIVATE
        ).getString("token", "");

        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "La sessió ha caducat. Torna a iniciar sessió.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        construirInterficie();
        carregarTarifes();
    }

    private void construirInterficie() {

        LinearLayout arrel = new LinearLayout(this);
        arrel.setOrientation(LinearLayout.VERTICAL);
        arrel.setPadding(24, 24, 24, 24);
        arrel.setBackgroundColor(Color.rgb(23, 26, 25));

        TextView titol = new TextView(this);
        titol.setText("Tarifes i preus");
        titol.setTextSize(27);
        titol.setTypeface(null, Typeface.BOLD);
        titol.setTextColor(Color.rgb(102, 187, 106));
        titol.setGravity(Gravity.CENTER);

        arrel.addView(titol, crearParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitol = new TextView(this);
        subtitol.setText(
                "Consulta les tarifes disponibles i la durada de cada opció."
        );
        subtitol.setTextSize(15);
        subtitol.setTextColor(Color.LTGRAY);
        subtitol.setPadding(0, 12, 0, 12);

        arrel.addView(subtitol);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);

        arrel.addView(progress, crearParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ScrollView scroll = new ScrollView(this);

        llista = new LinearLayout(this);
        llista.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(llista);

        arrel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        Button actualitzar = new Button(this);
        actualitzar.setText("Actualitzar tarifes");
        actualitzar.setOnClickListener(v -> carregarTarifes());

        arrel.addView(actualitzar, crearParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button tornar = new Button(this);
        tornar.setText("Tornar al menú");
        tornar.setOnClickListener(v -> finish());

        arrel.addView(tornar, crearParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(arrel);
    }

    private LinearLayout.LayoutParams crearParams(
            int ample,
            int alt
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ample, alt);

        params.setMargins(0, 8, 0, 8);

        return params;
    }

    private void carregarTarifes() {

        progress.setVisibility(View.VISIBLE);
        llista.removeAllViews();

        executor.execute(() -> {

            HttpURLConnection conn = null;

            try {
                URL url = new URL(BASE_URL + "?resource=tarifes");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty(
                        "Accept",
                        "application/json"
                );
                conn.setRequestProperty(
                        "Authorization",
                        "Bearer " + token
                );

                int codi = conn.getResponseCode();

                InputStream is = (codi >= 200 && codi < 400)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                StringBuilder respostaText = new StringBuilder();

                if (is != null) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                                    is,
                                    StandardCharsets.UTF_8
                            ))) {

                        String linia;

                        while ((linia = br.readLine()) != null) {
                            respostaText.append(linia);
                        }
                    }
                }

                JSONObject resposta = respostaText.length() > 0
                        ? new JSONObject(respostaText.toString())
                        : new JSONObject();

                runOnUiThread(() -> mostrarResultat(resposta, codi));

            } catch (Exception e) {

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);

                    mostrarMissatge(
                            "No s'han pogut carregar les tarifes. "
                                    + "Comprova la connexió amb el servidor."
                    );
                });

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void mostrarResultat(JSONObject resposta, int codi) {

        progress.setVisibility(View.GONE);
        llista.removeAllViews();

        if (!resposta.optBoolean("success", false)) {

            mostrarMissatge(
                    resposta.optString(
                            "message",
                            "No s'han pogut obtenir les tarifes."
                    )
            );

            return;
        }

        JSONArray tarifes = resposta.optJSONArray("data");

        if (tarifes == null) {
            tarifes = resposta.optJSONArray("tarifes");
        }

        if (tarifes == null) {
            mostrarMissatge(
                    "El servidor no ha retornat la llista de tarifes "
                            + "en el format esperat. Resposta HTTP: " + codi
            );
            return;
        }

        if (tarifes.length() == 0) {
            mostrarMissatge("Actualment no hi ha tarifes disponibles.");
            return;
        }

        for (int i = 0; i < tarifes.length(); i++) {

            JSONObject tarifa = tarifes.optJSONObject(i);

            if (tarifa != null) {
                afegirTarifa(tarifa);
            }
        }
    }

    private void afegirTarifa(JSONObject tarifa) {

        LinearLayout targeta = new LinearLayout(this);
        targeta.setOrientation(LinearLayout.VERTICAL);
        targeta.setPadding(20, 16, 20, 16);
        targeta.setBackgroundColor(Color.rgb(42, 47, 44));

        String nom = tarifa.optString("nom", "Tarifa");

        String preuText = tarifa.optString("preu", "");
        double preu = tarifa.optDouble("preu", -1);

        int durada = tarifa.optInt("durada_minuts", 0);

        TextView nomTarifa = new TextView(this);
        nomTarifa.setText(nom);
        nomTarifa.setTextSize(20);
        nomTarifa.setTypeface(null, Typeface.BOLD);
        nomTarifa.setTextColor(Color.rgb(102, 187, 106));

        targeta.addView(nomTarifa);

        TextView informacio = new TextView(this);
        informacio.setTextColor(Color.WHITE);
        informacio.setTextSize(16);

        StringBuilder text = new StringBuilder();

        if (preu >= 0) {
            text.append(String.format(
                    java.util.Locale.getDefault(),
                    "Preu: %.2f €",
                    preu
            ));
        } else if (!preuText.isEmpty() && !"null".equals(preuText)) {
            text.append("Preu: ").append(preuText).append(" €");
        } else {
            text.append("Preu: no disponible");
        }

        if (durada > 0) {
            int hores = durada / 60;
            int minuts = durada % 60;

            text.append("\nDurada: ");

            if (hores > 0) {
                text.append(hores).append(
                        hores == 1 ? " hora" : " hores"
                );
            }

            if (minuts > 0) {
                if (hores > 0) {
                    text.append(" i ");
                }

                text.append(minuts).append(" minuts");
            }
        }

        String dataInici = tarifa.optString("data_inici", "");
        String dataFi = tarifa.optString("data_fi", "");

        if (!dataInici.isEmpty() && !"null".equals(dataInici)) {
            text.append("\nVàlida des de: ").append(dataInici);
        }

        if (!dataFi.isEmpty() && !"null".equals(dataFi)) {
            text.append("\nVàlida fins a: ").append(dataFi);
        }

        informacio.setText(text.toString());
        informacio.setPadding(0, 8, 0, 0);

        targeta.addView(informacio);

        llista.addView(targeta, crearParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    private void mostrarMissatge(String missatge) {

        TextView text = new TextView(this);
        text.setText(missatge);
        text.setTextColor(Color.WHITE);
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER);
        text.setPadding(16, 32, 16, 32);

        llista.addView(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}