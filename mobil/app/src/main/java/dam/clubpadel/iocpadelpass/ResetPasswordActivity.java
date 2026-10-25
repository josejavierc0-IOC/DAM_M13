package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResetPasswordActivity extends Activity {

    private EditText etToken;
    private EditText etPasswordNova;
    private EditText etConfirmarPassword;
    private Button btnRestablir;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etToken = findViewById(R.id.etToken);
        etPasswordNova = findViewById(R.id.etPasswordNova);
        etConfirmarPassword =
                findViewById(R.id.etConfirmarPasswordNova);
        btnRestablir = findViewById(R.id.btnRestablir);

        String tokenRebut = getIntent().getStringExtra("token");

        if (tokenRebut != null && !tokenRebut.isEmpty()) {
            etToken.setText(tokenRebut);
        }

        btnRestablir.setOnClickListener(v -> restablir());
    }

    private void restablir() {
        String token = etToken.getText().toString().trim();
        String passwordNova = etPasswordNova.getText().toString();
        String confirmarPassword =
                etConfirmarPassword.getText().toString();

        if (token.isEmpty()) {
            etToken.setError("Introdueix el token");
            etToken.requestFocus();
            return;
        }

        if (passwordNova.isEmpty()
                || confirmarPassword.isEmpty()) {
            mostrarMissatge(
                    "Cal introduir i confirmar la contrasenya nova."
            );
            return;
        }

        if (passwordNova.length() < 8) {
            etPasswordNova.setError(
                    "La contrasenya ha de tenir almenys 8 caràcters"
            );
            etPasswordNova.requestFocus();
            return;
        }

        if (!passwordNova.equals(confirmarPassword)) {
            etConfirmarPassword.setError(
                    "Les contrasenyes no coincideixen"
            );
            etConfirmarPassword.requestFocus();
            return;
        }

        btnRestablir.setEnabled(false);
        btnRestablir.setText("Actualitzant...");

        executor.execute(() -> {
            try {
                JSONObject resposta =
                        ApiClientAndroid.restablirContrasenya(
                                token,
                                passwordNova
                        );

                runOnUiThread(() -> {
                    btnRestablir.setEnabled(true);
                    btnRestablir.setText("Canviar contrasenya");

                    boolean correcte =
                            resposta.optBoolean("success", false);

                    String missatge = resposta.optString(
                            "message",
                            "El servidor no ha retornat cap missatge."
                    );

                    if (correcte) {
                        new AlertDialog.Builder(this)
                                .setTitle("Contrasenya actualitzada")
                                .setMessage(
                                        missatge
                                                + "\n\nAra pots tornar al "
                                                + "login i entrar amb la "
                                                + "contrasenya nova."
                                )
                                .setCancelable(false)
                                .setPositiveButton("Anar al login",
                                        (dialog, which) -> {
                                            setResult(RESULT_OK);
                                            finish();
                                        })
                                .show();
                    } else {
                        mostrarMissatge(missatge);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnRestablir.setEnabled(true);
                    btnRestablir.setText("Canviar contrasenya");

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