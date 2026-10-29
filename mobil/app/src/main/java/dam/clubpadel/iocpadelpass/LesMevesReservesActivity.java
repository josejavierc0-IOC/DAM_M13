package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LesMevesReservesActivity extends Activity {
    private static final String BASE_URL =
            "http://10.0.2.2/DAM_M13/servidor/index.php";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout llista;
    private ProgressBar progress;
    private String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        token = getSharedPreferences("IOCPadelPass", MODE_PRIVATE)
                .getString("token", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "La sessió ha caducat. Torna a iniciar sessió.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        construirInterficie();
        carregarReserves();
    }

    private void construirInterficie() {
        LinearLayout arrel = new LinearLayout(this);
        arrel.setOrientation(LinearLayout.VERTICAL);
        arrel.setPadding(24, 24, 24, 24);
        arrel.setBackgroundColor(Color.rgb(23, 26, 25));

        TextView titol = new TextView(this);
        titol.setText("Les meves reserves");
        titol.setTextSize(27);
        titol.setTypeface(null, Typeface.BOLD);
        titol.setTextColor(Color.rgb(102, 187, 106));
        titol.setGravity(Gravity.CENTER);
        arrel.addView(titol, params(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitol = new TextView(this);
        subtitol.setText("Selecciona una reserva per consultar-ne els detalls.");
        subtitol.setTextColor(Color.LTGRAY);
        subtitol.setTextSize(15);
        subtitol.setPadding(0, 12, 0, 12);
        arrel.addView(subtitol);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        arrel.addView(progress, params(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        llista = new LinearLayout(this);
        llista.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(llista);
        arrel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button actualitzar = new Button(this);
        actualitzar.setText("Actualitzar llista");
        actualitzar.setOnClickListener(v -> carregarReserves());

        arrel.addView(actualitzar, params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

// Botó per tornar al menú principal.
        Button tornarMenu = new Button(this);
        tornarMenu.setText("Tornar al menú");

        tornarMenu.setOnClickListener(v -> {
            finish();
        });

        arrel.addView(tornarMenu, params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(arrel);
    }

    private LinearLayout.LayoutParams params(int ample, int alt) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ample, alt);
        p.setMargins(0, 8, 0, 8);
        return p;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (llista != null) carregarReserves();
    }

    private void carregarReserves() {
        progress.setVisibility(View.VISIBLE);
        llista.removeAllViews();

        executor.execute(() -> {
            try {
                JSONObject resposta = peticio("GET", BASE_URL + "?resource=reserves", null);
                JSONArray reserves = resposta.optJSONArray("data");
                if (reserves == null) reserves = resposta.optJSONArray("reserves");
                JSONArray resultat = reserves;

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    llista.removeAllViews();

                    if (!resposta.optBoolean("success", false)) {
                        mostrarMissatge(resposta.optString("message",
                                "No s'han pogut carregar les reserves."));
                        return;
                    }
                    if (resultat == null || resultat.length() == 0) {
                        mostrarMissatge("Encara no tens cap reserva.");
                        return;
                    }

                    for (int i = 0; i < resultat.length(); i++) {
                        JSONObject reserva = resultat.optJSONObject(i);
                        if (reserva != null) afegirReserva(reserva);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    mostrarMissatge("Error de connexió: " + e.getMessage());
                });
            }
        });
    }

    private void afegirReserva(JSONObject r) {
        LinearLayout targeta = new LinearLayout(this);
        targeta.setOrientation(LinearLayout.VERTICAL);
        targeta.setPadding(18, 14, 18, 14);
        targeta.setBackgroundColor(Color.rgb(42, 47, 44));

        String pista = r.optString("pista_nom", r.optString("pista", "Pista"));
        String data = r.optString("data_reserva", "Data no disponible");
        String inici = hora(r.optString("hora_inici", ""));
        String fi = hora(r.optString("hora_fi", ""));
        String estat = r.optString("estat", "Desconegut");
        String preu = r.optString("preu_total", r.optString("preu", ""));
        String id = r.optString("id", "?");

        TextView info = new TextView(this);
        String text = "Reserva #" + id + "\n" + pista + "\n" + data
                + "\n" + inici + " - " + fi + "\nEstat: " + estat;
        if (!preu.isEmpty() && !"null".equals(preu)) text += "\nImport: " + preu + " €";
        info.setText(text);
        info.setTextSize(16);
        info.setTextColor(Color.WHITE);
        targeta.addView(info);

        Button detall = new Button(this);
        detall.setText("Veure detalls");
        detall.setOnClickListener(v -> mostrarDetall(r));
        targeta.addView(detall, params(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if ("CONFIRMADA".equalsIgnoreCase(estat)) {
            Button cancelLar = new Button(this);
            cancelLar.setText("Cancel·lar reserva");
            cancelLar.setOnClickListener(v -> confirmarCancellacio(r));
            targeta.addView(cancelLar, params(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        llista.addView(targeta, params(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private String hora(String h) {
        return h != null && h.length() >= 5 ? h.substring(0, 5) : h;
    }

    private void mostrarMissatge(String missatge) {
        TextView t = new TextView(this);
        t.setText(missatge);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 30, 12, 30);
        llista.addView(t);
    }

    private void mostrarDetall(JSONObject r) {

        String id = r.optString("id", "—");
        String pista = r.optString(
                "pista_nom",
                r.optString("pista", "—")
        );
        String data = r.optString("data_reserva", "—");
        String inici = hora(r.optString("hora_inici", "—"));
        String fi = hora(r.optString("hora_fi", "—"));
        String estat = r.optString("estat", "—");
        String pagament = r.optString("pagament_estat", "No disponible");

        String detall =
                "Número de reserva: " + id +
                        "\n\nPista: " + pista +
                        "\n\nData: " + data +
                        "\n\nHora d'inici: " + inici +
                        "\nHora de finalització: " + fi +
                        "\n\nEstat de la reserva: " + estat +
                        "\n\nEstat del pagament: " + pagament;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Detalls de la reserva")
                .setMessage(detall)
                .setPositiveButton("Tornar a les reserves", (d, which) -> {
                    // Tanca els detalls i torna a la llista.
                    d.dismiss();
                })
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.rgb(46, 125, 50));
        });

        dialog.show();
    }


    private void confirmarCancellacio(JSONObject r) {
        String id = r.optString("id", "");
        new AlertDialog.Builder(this)
                .setTitle("Cancel·lar reserva")
                .setMessage("Segur que vols cancelLar la reserva #" + id + "?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí, cancel·lar", (dialog, which) -> cancelLar(id))
                .show();
    }

    private void cancelLar(String id) {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String url = BASE_URL + "?resource=reserva&id=" +
                        URLEncoder.encode(id, "UTF-8") + "&accio=cancel%C2%B7lar";
                JSONObject resposta = peticio("PATCH", url, new JSONObject());
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, resposta.optString("message",
                                    resposta.optBoolean("success") ? "Reserva cancel·lada." :
                                            "No s'ha pogut cancelLar."),
                            Toast.LENGTH_LONG).show();
                    if (resposta.optBoolean("success", false)) carregarReserves();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error de connexió: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private JSONObject peticio(String metode, String urlText, JSONObject cos) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
        conn.setRequestMethod(metode);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        if (cos != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(cos.toString().getBytes(StandardCharsets.UTF_8));
            }
        }

        int codi = conn.getResponseCode();
        InputStream is = codi >= 200 && codi < 400 ?
                conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String linia;
                while ((linia = br.readLine()) != null) sb.append(linia);
            }
        }
        conn.disconnect();

        JSONObject resposta = sb.length() > 0 ? new JSONObject(sb.toString()) : new JSONObject();
        resposta.put("_http_code", codi);
        return resposta;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
