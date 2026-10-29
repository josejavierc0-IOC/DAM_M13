package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReservaActivity extends Activity {
    private static final String BASE_URL =
            "http://10.0.2.2/DAM_M13/servidor/index.php";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<JSONObject> franges = new ArrayList<>();
    private final ArrayList<String> textosFranges = new ArrayList<>();

    private TextView dataText, estatText;
    private Button dataButton, consultarButton, reservarButton;
    private ListView frangesList;
    private ArrayAdapter<String> adapter;
    private String dataSeleccionada;
    private int franjaSeleccionada = -1;
    private boolean carregant = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Calendar avui = Calendar.getInstance();
        dataSeleccionada = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                .format(avui.getTime());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Reservar una pista");
        title.setTextSize(25);
        root.addView(title);

        dataText = new TextView(this);
        dataText.setTextSize(17);
        dataText.setPadding(0, dp(12), 0, dp(8));
        root.addView(dataText);

        dataButton = new Button(this);
        dataButton.setText("Seleccionar data");
        root.addView(dataButton);

        consultarButton = new Button(this);
        consultarButton.setText("Consultar disponibilitat");
        root.addView(consultarButton);

        estatText = new TextView(this);
        estatText.setText("Selecciona una data i consulta les franges.");
        estatText.setPadding(0, dp(8), 0, dp(8));
        root.addView(estatText);

        frangesList = new ListView(this);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, textosFranges);
        frangesList.setAdapter(adapter);
        frangesList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        root.addView(frangesList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        reservarButton = new Button(this);
        reservarButton.setText("Confirmar reserva");
        reservarButton.setEnabled(false);
        root.addView(reservarButton);

        Button tornarButton = new Button(this);
        tornarButton.setText("Tornar");
        root.addView(tornarButton);

        setContentView(root);
        actualitzarDataText();

        dataButton.setOnClickListener(v -> obrirSelectorData());
        consultarButton.setOnClickListener(v -> consultarDisponibilitat());
        reservarButton.setOnClickListener(v -> confirmarReserva());
        tornarButton.setOnClickListener(v -> finish());

        frangesList.setOnItemClickListener((parent, view, position, id) -> {
            franjaSeleccionada = position;
            reservarButton.setEnabled(position >= 0 && position < franges.size());
        });
    }

    private void obrirSelectorData() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (picker, year, month, day) -> {
                    Calendar seleccionada = Calendar.getInstance();
                    seleccionada.set(year, month, day);
                    dataSeleccionada = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                            .format(seleccionada.getTime());
                    actualitzarDataText();
                    buidarFranges();
                    estatText.setText("Ara consulta la disponibilitat d'aquesta data.");
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void actualitzarDataText() {
        dataText.setText("Data seleccionada: " + dataSeleccionada);
    }

    private void consultarDisponibilitat() {
        if (carregant) return;
        buidarFranges();
        estatText.setText("Consultant disponibilitat...");
        mostrarCarrega(true, "Consultant franges...");
        final String data = dataSeleccionada;

        executor.execute(() -> {
            try {
                String query = "?resource=disponibilitat&data=" +
                        URLEncoder.encode(data, "UTF-8");
                JSONObject resposta = peticioGet(query);
                runOnUiThread(() -> {
                    mostrarCarrega(false, null);
                    if (!resposta.optBoolean("success", false)) {
                        estatText.setText(resposta.optString("message",
                                "No s'ha pogut consultar la disponibilitat."));
                        return;
                    }
                    JSONArray llista = resposta.optJSONArray("franges_disponibles");
                    if (llista == null) llista = new JSONArray();

                    for (int i = 0; i < llista.length(); i++) {
                        JSONObject franja = llista.optJSONObject(i);
                        if (franja == null) continue;
                        franges.add(franja);
                        String inici = franja.optString("hora_inici", "");
                        String fi = franja.optString("hora_fi", "");
                        String pista = franja.optString("pista", "Pista");
                        String tarifa = franja.optString("tarifa", "Tarifa");
                        String durada = franja.optString("durada_minuts", "?");
                        String preu = franja.optString("preu_total", "—");
                        textosFranges.add(pista + " · " + inici.substring(0, Math.min(5, inici.length()))
                                + "–" + fi.substring(0, Math.min(5, fi.length()))
                                + "\n" + tarifa + " · " + durada + " min · " + preu + " €");
                    }
                    adapter.notifyDataSetChanged();
                    if (franges.isEmpty()) {
                        estatText.setText("No hi ha franges disponibles per aquesta data.");
                    } else {
                        estatText.setText("Selecciona una franja i confirma la reserva.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    mostrarCarrega(false, null);
                    estatText.setText("Error de connexió. Comprova que el servidor XAMPP estigui actiu.");
                });
            }
        });
    }

    private void confirmarReserva() {
        if (franjaSeleccionada < 0 || franjaSeleccionada >= franges.size()) {
            Toast.makeText(this, "Selecciona una franja horària.", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject franja = franges.get(franjaSeleccionada);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Confirmar reserva")
                .setMessage("Pista: " + franja.optString("pista") +
                        "\nData: " + franja.optString("data") +
                        "\nHora: " + franja.optString("hora_inici", "").substring(
                        0, Math.min(5, franja.optString("hora_inici", "").length())) +
                        "–" + franja.optString("hora_fi", "").substring(
                        0, Math.min(5, franja.optString("hora_fi", "").length())) +
                        "\nDurada: " + franja.optString("durada_minuts") + " minuts" +
                        "\nPreu: " + franja.optString("preu_total") + " €" +
                        "\n\nEl pagament queda pendent i es farà presencialment.")
                .setNegativeButton("Cancel·lar", null)
                .setPositiveButton("Confirmar", (dialog, which) -> enviarReserva(franja))
                .show();
    }

    private void enviarReserva(JSONObject franja) {
        if (carregant) return;
        mostrarCarrega(true, "Creant reserva...");
        executor.execute(() -> {
            try {
                JSONObject dades = new JSONObject();
                dades.put("pista_id", franja.getInt("pista_id"));
                dades.put("tarifa_id", franja.getInt("tarifa_id"));
                dades.put("data_reserva", franja.getString("data"));
                dades.put("hora_inici", franja.getString("hora_inici"));
                dades.put("hora_fi", franja.getString("hora_fi"));

                JSONObject resposta = peticioPost("?resource=reserves", dades);
                runOnUiThread(() -> {
                    mostrarCarrega(false, null);
                    if (resposta.optBoolean("success", false)) {
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Reserva creada")
                                .setMessage(resposta.optString("message",
                                        "La reserva s'ha creat correctament.") +
                                        "\n\nEl pagament queda pendent.")
                                .setPositiveButton("D'acord", (d, w) -> {
                                    buidarFranges();
                                    consultarDisponibilitat();
                                })
                                .show();
                    } else {
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("No s'ha pogut reservar")
                                .setMessage(resposta.optString("message",
                                        "El servidor no ha pogut crear la reserva. Torna a consultar la disponibilitat."))
                                .setPositiveButton("D'acord", (d, w) -> consultarDisponibilitat())
                                .show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    mostrarCarrega(false, null);
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Error de connexió")
                            .setMessage("No s'ha pogut completar la reserva. Comprova la connexió i torna-ho a provar.")
                            .setPositiveButton("D'acord", null)
                            .show();
                });
            }
        });
    }

    private JSONObject peticioGet(String query) throws Exception {
        HttpURLConnection conn = obrirConnexio(query, "GET");
        return llegirResposta(conn);
    }

    private JSONObject peticioPost(String query, JSONObject dades) throws Exception {
        HttpURLConnection conn = obrirConnexio(query, "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] cos = dades.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(cos);
        }
        return llegirResposta(conn);
    }

    private HttpURLConnection obrirConnexio(String query, String metode) throws Exception {
        String token = getSharedPreferences("IOCPadelPass", MODE_PRIVATE)
                .getString("token", "");
        URL url = new URL(BASE_URL + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(metode);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Accept", "application/json");
        if (!token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
        return conn;
    }

    private JSONObject llegirResposta(HttpURLConnection conn) throws Exception {
        try {
            int codi = conn.getResponseCode();
            InputStream stream = (codi >= 200 && codi < 400)
                    ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) throw new Exception("Resposta HTTP " + codi);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String linia;
                while ((linia = br.readLine()) != null) sb.append(linia);
            }
            JSONObject json = new JSONObject(sb.toString());
            if (codi < 200 || codi >= 300) {
                if (!json.has("message")) json.put("message", "Error HTTP " + codi);
            }
            return json;
        } finally {
            conn.disconnect();
        }
    }

    private void buidarFranges() {
        franges.clear();
        textosFranges.clear();
        franjaSeleccionada = -1;
        if (adapter != null) adapter.notifyDataSetChanged();
        if (reservarButton != null) reservarButton.setEnabled(false);
    }

    private void mostrarCarrega(boolean activa, String missatge) {
        carregant = activa;
        dataButton.setEnabled(!activa);
        consultarButton.setEnabled(!activa);
        reservarButton.setEnabled(!activa && franjaSeleccionada >= 0);
        if (activa && missatge != null) estatText.setText(missatge);
    }

    private int dp(int valor) {
        return (int) (valor * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
