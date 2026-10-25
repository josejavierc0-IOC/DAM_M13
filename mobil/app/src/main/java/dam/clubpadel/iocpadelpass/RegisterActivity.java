package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends Activity {

    private EditText etNom;
    private EditText etCognoms;
    private EditText etEmail;
    private EditText etTelefon;
    private EditText etPassword;
    private EditText etConfirmarPassword;
    private Button btnRegistrar;
    private TextView tvTornarLogin;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNom = findViewById(R.id.etNom);
        etCognoms = findViewById(R.id.etCognoms);
        etEmail = findViewById(R.id.etEmailRegistre);
        etTelefon = findViewById(R.id.etTelefon);
        etPassword = findViewById(R.id.etPasswordRegistre);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        tvTornarLogin = findViewById(R.id.tvTornarLogin);

        btnRegistrar.setOnClickListener(v -> registrar());

        tvTornarLogin.setOnClickListener(v -> finish());
    }

    private void registrar() {
        String nom = etNom.getText().toString().trim();
        String cognoms = etCognoms.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefon = etTelefon.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmarPassword =
                etConfirmarPassword.getText().toString();

        if (nom.isEmpty() || cognoms.isEmpty()
                || email.isEmpty() || password.isEmpty()
                || confirmarPassword.isEmpty()) {

            mostrarMissatge("Cal omplir tots els camps obligatoris.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            etEmail.setError("Introdueix un correu electrònic vàlid");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError(
                    "La contrasenya ha de tenir almenys 8 caràcters"
            );
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmarPassword)) {
            etConfirmarPassword.setError(
                    "Les contrasenyes no coincideixen"
            );
            etConfirmarPassword.requestFocus();
            return;
        }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Registrant...");

        executor.execute(() -> {
            try {
                JSONObject resposta = ApiClientAndroid.registrarUsuari(
                        nom,
                        cognoms,
                        email,
                        password,
                        telefon
                );

                runOnUiThread(() -> {
                    btnRegistrar.setEnabled(true);
                    btnRegistrar.setText("Crear compte");

                    boolean correcte =
                            resposta.optBoolean("success", false);

                    String missatge = resposta.optString(
                            "message",
                            correcte
                                    ? "Registre completat correctament."
                                    : "No s'ha pogut completar el registre."
                    );

                    if (correcte) {
                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("Registre completat")
                                .setMessage(
                                        missatge
                                                + "\n\nAra pots iniciar sessió "
                                                + "amb el teu correu i la "
                                                + "contrasenya."
                                )
                                .setCancelable(false)
                                .setPositiveButton("Anar al login",
                                        (dialog, which) -> {
                                            setResult(
                                                    RESULT_OK,
                                                    getIntent()
                                            );
                                            finish();
                                        })
                                .show();
                    } else {
                        mostrarMissatge(missatge);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnRegistrar.setEnabled(true);
                    btnRegistrar.setText("Crear compte");

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