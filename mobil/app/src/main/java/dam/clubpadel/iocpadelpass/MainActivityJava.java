package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivityJava extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private TextView tvError;
    private Button btnLogin;
    private Button btnRegister;
    private TextView btnForgotPassword;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);

        tvError.setVisibility(View.GONE);

        btnLogin.setOnClickListener(v -> iniciarSessio());

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivityJava.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });

        btnForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivityJava.this,
                    RecoverPasswordActivity.class
            );
            startActivity(intent);
        });
    }

    private void iniciarSessio() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        tvError.setVisibility(View.GONE);

        if (email.isEmpty() || password.isEmpty()) {
            mostrarError("Introdueix el correu i la contrasenya.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            mostrarError("Introdueix un correu electrònic vàlid.");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Connectant...");

        executor.execute(() -> {
            try {
                JSONObject resposta =
                        ApiClientAndroid.iniciarSessio(email, password);

                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sessió");

                    boolean correcte =
                            resposta.optBoolean("success", false);

                    if (!correcte) {
                        mostrarError(
                                resposta.optString(
                                        "message",
                                        "No s'ha pogut iniciar la sessió."
                                )
                        );
                        return;
                    }

                    String token = resposta.optString("token", "");
                    JSONObject usuari = resposta.optJSONObject("usuari");

                    if (token.isEmpty() || usuari == null) {
                        mostrarError(
                                "La resposta del servidor no conté "
                                        + "les dades de sessió esperades."
                        );
                        return;
                    }

                    SharedPreferences preferencies =
                            getSharedPreferences(
                                    "IOCPadelPass",
                                    MODE_PRIVATE
                            );

                    SharedPreferences.Editor editor =
                            preferencies.edit();

                    editor.putString("token", token);
                    editor.putString("email", email);
                    editor.putString(
                            "nom",
                            usuari.optString("nom", "")
                    );
                    editor.putString(
                            "cognoms",
                            usuari.optString("cognoms", "")
                    );
                    editor.putString(
                            "rol",
                            usuari.optString("rol", "")
                    );
                    editor.putInt("id", usuari.optInt("id", -1));
                    editor.putString(
                            "expira_at",
                            resposta.optString("expira_at", "")
                    );
                    editor.apply();

                    Toast.makeText(
                            MainActivityJava.this,
                            "Sessió iniciada correctament",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            MainActivityJava.this,
                            HomeActivity.class
                    );

                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sessió");

                    mostrarError(
                            "No s'ha pogut connectar amb el servidor. "
                                    + "Comprova que el servidor estigui "
                                    + "en funcionament."
                    );
                });
            }
        });
    }

    private void mostrarError(String missatge) {
        tvError.setText(missatge);
        tvError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}