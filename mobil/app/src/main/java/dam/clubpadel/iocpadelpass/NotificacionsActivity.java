package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.List;

public class NotificacionsActivity extends Activity {

    private static final String BASE_URL =
            "http://10.0.2.2/DAM_M13/servidor/index.php";

    private SharedPreferences preferences;
    private LinearLayout llista;
    private TextView textEstat;
    private Button btnTotesLlegides;

    private final List<Notificacio> notificacions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                "IOCPadelPass",
                MODE_PRIVATE
        );

        String token = preferences.getString("token", "");

        if (token == null || token.trim().isEmpty()) {
            tornarAlLogin();
            return;
        }

        crearInterficie();
        carregarNotificacions();
    }

    private void crearInterficie() {

        LinearLayout principal = new LinearLayout(this);
        principal.setOrientation(LinearLayout.VERTICAL);
        principal.setPadding(28, 28, 28, 28);
        principal.setBackgroundColor(Color.rgb(23, 26, 25));

        TextView titol = new TextView(this);
        titol.setText("Les meves notificacions");
        titol.setTextSize(26);
        titol.setTypeface(null, Typeface.BOLD);
        titol.setTextColor(Color.rgb(102, 187, 106));
        titol.setGravity(Gravity.CENTER);
        titol.setPadding(0, 8, 0, 20);

        textEstat = new TextView(this);
        textEstat.setText("Carregant notificacions...");
        textEstat.setTextSize(16);
        textEstat.setTextColor(Color.WHITE);
        textEstat.setGravity(Gravity.CENTER);
        textEstat.setPadding(0, 8, 0, 16);

        btnTotesLlegides = new Button(this);
        btnTotesLlegides.setText("Marcar totes com a llegides");
        btnTotesLlegides.setOnClickListener(v -> marcarTotesLlegides());

        ScrollView scrollView = new ScrollView(this);

        llista = new LinearLayout(this);
        llista.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(
                llista,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button btnTornar = new Button(this);
        btnTornar.setText("Tornar al menú");
        btnTornar.setOnClickListener(v -> finish());

        principal.addView(titol, crearParametres());
        principal.addView(textEstat, crearParametres());
        principal.addView(btnTotesLlegides, crearParametres());

        principal.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        principal.addView(btnTornar, crearParametres());

        setContentView(principal);
    }

    private LinearLayout.LayoutParams crearParametres() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);

        return params;
    }

    private void carregarNotificacions() {

        new Thread(() -> {

            HttpURLConnection connexio = null;

            try {
                String token = preferences.getString("token", "");

                URL url = new URL(
                        BASE_URL + "?resource=notificacions"
                );

                connexio = (HttpURLConnection) url.openConnection();
                connexio.setRequestMethod("GET");
                connexio.setConnectTimeout(10000);
                connexio.setReadTimeout(10000);
                connexio.setRequestProperty(
                        "Authorization",
                        "Bearer " + token
                );
                connexio.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int codi = connexio.getResponseCode();

                InputStream entrada = codi >= 200 && codi < 300
                        ? connexio.getInputStream()
                        : connexio.getErrorStream();

                String resposta = llegirResposta(entrada);

                JSONObject json = new JSONObject(resposta);

                if (codi < 200 || codi >= 300
                        || !json.optBoolean("success", false)) {

                    String missatge = json.optString(
                            "message",
                            "No s'han pogut carregar les notificacions."
                    );

                    runOnUiThread(() ->
                            mostrarError(missatge)
                    );

                    return;
                }

                JSONArray dades = json.optJSONArray("data");

                List<Notificacio> resultat = new ArrayList<>();

                if (dades != null) {
                    for (int i = 0; i < dades.length(); i++) {

                        JSONObject objecte = dades.optJSONObject(i);

                        if (objecte == null) {
                            continue;
                        }

                        Notificacio notificacio = new Notificacio();

                        notificacio.id = objecte.optInt("id", 0);
                        notificacio.titol = objecte.optString(
                                "titol",
                                "Notificació"
                        );
                        notificacio.missatge = objecte.optString(
                                "missatge",
                                ""
                        );
                        notificacio.tipus = objecte.optString(
                                "tipus",
                                "GENERAL"
                        );
                        notificacio.data = objecte.optString(
                                "creat_at",
                                ""
                        );

                        Object valorLlegida = objecte.opt("llegida");

                        notificacio.llegida =
                                "1".equals(String.valueOf(valorLlegida))
                                        || Boolean.TRUE.equals(valorLlegida);

                        resultat.add(notificacio);
                    }
                }

                runOnUiThread(() -> {
                    notificacions.clear();
                    notificacions.addAll(resultat);
                    mostrarNotificacions();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        mostrarError(
                                "Error de connexió en consultar les notificacions."
                        )
                );

            } finally {
                if (connexio != null) {
                    connexio.disconnect();
                }
            }

        }).start();
    }

    private String llegirResposta(InputStream entrada) throws Exception {

        if (entrada == null) {
            return "{}";
        }

        StringBuilder resultat = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        entrada,
                        StandardCharsets.UTF_8
                )
        )) {
            String linia;

            while ((linia = reader.readLine()) != null) {
                resultat.append(linia);
            }
        }

        return resultat.toString();
    }

    private void mostrarNotificacions() {

        llista.removeAllViews();

        if (notificacions.isEmpty()) {
            textEstat.setText("No tens notificacions.");
            btnTotesLlegides.setEnabled(false);
            return;
        }

        int pendents = 0;

        for (Notificacio notificacio : notificacions) {
            if (!notificacio.llegida) {
                pendents++;
            }

            afegirNotificacio(notificacio);
        }

        textEstat.setText(
                "Total: " + notificacions.size()
                        + " | Pendents: " + pendents
        );

        btnTotesLlegides.setEnabled(pendents > 0);
    }

    private void afegirNotificacio(Notificacio notificacio) {

        LinearLayout targeta = new LinearLayout(this);
        targeta.setOrientation(LinearLayout.VERTICAL);
        targeta.setPadding(20, 16, 20, 16);

        if (notificacio.llegida) {
            targeta.setBackgroundColor(Color.rgb(48, 52, 50));
        } else {
            targeta.setBackgroundColor(Color.rgb(43, 65, 47));
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);

        TextView titol = new TextView(this);

        titol.setText(
                (notificacio.llegida ? "✓ " : "● ")
                        + notificacio.titol
        );

        titol.setTextSize(18);
        titol.setTypeface(null, Typeface.BOLD);
        titol.setTextColor(Color.WHITE);

        TextView tipus = new TextView(this);
        tipus.setText("Tipus: " + notificacio.tipus);
        tipus.setTextSize(13);
        tipus.setTextColor(Color.rgb(170, 210, 170));

        TextView missatge = new TextView(this);
        missatge.setText(notificacio.missatge);
        missatge.setTextSize(15);
        missatge.setTextColor(Color.LTGRAY);
        missatge.setPadding(0, 8, 0, 8);

        TextView data = new TextView(this);
        data.setText(
                notificacio.data.isEmpty()
                        ? ""
                        : "Data: " + notificacio.data
        );
        data.setTextSize(12);
        data.setTextColor(Color.LTGRAY);

        TextView estat = new TextView(this);
        estat.setText(
                notificacio.llegida
                        ? "Llegida"
                        : "Pendent de llegir"
        );
        estat.setTextSize(13);
        estat.setTextColor(
                notificacio.llegida
                        ? Color.LTGRAY
                        : Color.rgb(129, 199, 132)
        );

        targeta.addView(titol);
        targeta.addView(tipus);
        targeta.addView(missatge);
        targeta.addView(data);
        targeta.addView(estat);

        targeta.setOnClickListener(v -> {

            if (notificacio.llegida) {
                new AlertDialog.Builder(this)
                        .setTitle(notificacio.titol)
                        .setMessage(notificacio.missatge)
                        .setPositiveButton("Tancar", null)
                        .show();

            } else {
                new AlertDialog.Builder(this)
                        .setTitle(notificacio.titol)
                        .setMessage(
                                notificacio.missatge
                                        + "\n\nVols marcar-la com a llegida?"
                        )
                        .setNegativeButton("Ara no", null)
                        .setPositiveButton("Marcar com a llegida",
                                (dialog, which) ->
                                        marcarLlegida(notificacio))
                        .show();
            }
        });

        llista.addView(targeta, params);
    }

    private void marcarLlegida(Notificacio notificacio) {

        executarPatch(
                "?resource=notificacions&accio=llegir&id="
                        + notificacio.id,
                "No s'ha pogut marcar la notificació com a llegida."
        );
    }

    private void marcarTotesLlegides() {

        new AlertDialog.Builder(this)
                .setTitle("Marcar totes com a llegides")
                .setMessage(
                        "Vols marcar totes les notificacions com a llegides?"
                )
                .setNegativeButton("Cancel·lar", null)
                .setPositiveButton("Sí", (dialog, which) ->
                        executarPatch(
                                "?resource=notificacions&accio=llegir_totes",
                                "No s'han pogut marcar les notificacions."
                        ))
                .show();
    }

    private void executarPatch(String ruta, String errorPerDefecte) {

        new Thread(() -> {

            HttpURLConnection connexio = null;

            try {
                String token = preferences.getString("token", "");

                URL url = new URL(BASE_URL + ruta);

                connexio = (HttpURLConnection) url.openConnection();
                connexio.setRequestMethod("PATCH");
                connexio.setConnectTimeout(10000);
                connexio.setReadTimeout(10000);
                connexio.setRequestProperty(
                        "Authorization",
                        "Bearer " + token
                );
                connexio.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int codi = connexio.getResponseCode();

                InputStream entrada = codi >= 200 && codi < 300
                        ? connexio.getInputStream()
                        : connexio.getErrorStream();

                String resposta = llegirResposta(entrada);
                JSONObject json = new JSONObject(resposta);

                if (codi < 200 || codi >= 300
                        || !json.optBoolean("success", false)) {

                    String missatge = json.optString(
                            "message",
                            errorPerDefecte
                    );

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    missatge,
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                    return;
                }

                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            json.optString(
                                    "message",
                                    "Notificacions actualitzades."
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    carregarNotificacions();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                errorPerDefecte,
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {
                if (connexio != null) {
                    connexio.disconnect();
                }
            }

        }).start();
    }

    private void mostrarError(String missatge) {

        textEstat.setText(missatge);
        llista.removeAllViews();

        btnTotesLlegides.setEnabled(false);

        Toast.makeText(
                this,
                missatge,
                Toast.LENGTH_LONG
        ).show();
    }

    private void tornarAlLogin() {

        Intent intent = new Intent(
                NotificacionsActivity.this,
                MainActivityJava.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private static class Notificacio {

        int id;
        String titol;
        String missatge;
        String tipus;
        String data;
        boolean llegida;
    }
}