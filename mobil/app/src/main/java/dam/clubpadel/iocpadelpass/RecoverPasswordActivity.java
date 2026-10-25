package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecoverPasswordActivity extends Activity {

    private EditText etEmail;
    private Button btnRecuperar;
    private TextView tvTornarLogin;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_password);

        etEmail = findViewById(R.id.etEmailRecuperacio);
        btnRecuperar = findViewById(R.id.btnRecuperar);
        tvTornarLogin = findViewById(R.id.tvTornarLoginRecuperacio);

        btnRecuperar.setOnClickListener(v -> solicitarRecuperacio());

        tvTornarLogin.setOnClickListener(v -> finish());
    }

    private void solicitarRecuperacio() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Introdueix el correu electrònic");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            etEmail.setError("Introdueix un correu electrònic vàlid");
            etEmail.requestFocus();
            return;
        }

        btnRecuperar.setEnabled(false);
        btnRecuperar.setText("Processant...");

        executor.execute(() -> {
            try {
                JSONObject resposta =
                        ApiClientAndroid.recuperarContrasenya(email);

                runOnUiThread(() -> {
                    btnRecuperar.setEnabled(true);
                    btnRecuperar.setText("Recuperar contrasenya");

                    boolean correcte =
                            resposta.optBoolean("success", false);

                    String missatge = resposta.optString(
                            "message",
                            "El servidor no ha retornat cap missatge."
                    );

                    if (!correcte) {
                        mostrarMissatge(missatge);
                        return;
                    }

                    String token = resposta.optString("token", "");
                    if (token.isEmpty()) {
                        mostrarMissatge(
                                "La sol·licitud s'ha processat, però el "
                                        + "servidor no ha retornat el token."
                        );
                        return;
                    }

                    String expiracio = resposta.optString(
                            "expira_at",
                            "No indicada"
                    );

                    new AlertDialog.Builder(
                            RecoverPasswordActivity.this
                    )
                            .setTitle("Recuperació sol·licitada")
                            .setMessage(
                                    "El servidor ha generat el token de "
                                            + "recuperació.\n\n"
                                            + "Token de prova:\n"
                                            + token
                                            + "\n\nCaduca: "
                                            + expiracio
                                            + "\n\nAquest token es mostra "
                                            + "només perquè encara no tenim "
                                            + "configurat l'enviament per "
                                            + "correu electrònic."
                            )
                            .setPositiveButton(
                                    "Continuar",
                                    (dialog, which) -> {
                                        android.content.Intent intent =
                                                new android.content.Intent(
                                                        this,
                                                        ResetPasswordActivity.class
                                                );
                                        intent.putExtra("token", token);
                                        startActivity(intent);
                                    }
                            )
                            .setNegativeButton("Tancar", null)
                            .show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnRecuperar.setEnabled(true);
                    btnRecuperar.setText("Recuperar contrasenya");

                    mostrarMissatge(
                            "No s'ha pogut connectar amb el servidor. "
                                    + "Comprova la connexió i torna-ho "
                                    + "a provar."
                    );
                });
            }
        });
    }

    private void mostrarMissatge(String missatge) {
        Toast.makeText(
                this,
                missatge,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}