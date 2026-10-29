package dam.clubpadel.iocpadelpass;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends Activity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                "IOCPadelPass",
                MODE_PRIVATE
        );

        String token = preferences.getString("token", "");

        // Si no hi ha token, tornem al login.
        if (token == null || token.trim().isEmpty()) {
            tornarAlLogin();
            return;
        }

        mostrarPantalla();
    }

    private void mostrarPantalla() {

        String nom = preferences.getString("nom", "");
        String cognoms = preferences.getString("cognoms", "");
        String rol = preferences.getString("rol", "");

        LinearLayout principal = new LinearLayout(this);
        principal.setOrientation(LinearLayout.VERTICAL);
        principal.setGravity(Gravity.CENTER);
        principal.setPadding(32, 32, 32, 32);
        principal.setBackgroundColor(Color.rgb(23, 26, 25));

        TextView titol = new TextView(this);
        titol.setText("IOC Padel Pass");
        titol.setTextSize(30);
        titol.setTypeface(null, Typeface.BOLD);
        titol.setTextColor(Color.rgb(102, 187, 106));
        titol.setGravity(Gravity.CENTER);

        TextView benvinguda = new TextView(this);
        benvinguda.setText("Benvingut/da, " + nom + " " + cognoms);
        benvinguda.setTextSize(22);
        benvinguda.setTextColor(Color.WHITE);
        benvinguda.setGravity(Gravity.CENTER);
        benvinguda.setPadding(0, 32, 0, 16);

        TextView rolUsuari = new TextView(this);
        rolUsuari.setText("Rol: " + rol);
        rolUsuari.setTextSize(16);
        rolUsuari.setTextColor(Color.LTGRAY);
        rolUsuari.setGravity(Gravity.CENTER);

        Button btnReservar = new Button(this);
        btnReservar.setText("Reservar una pista");

        Button btnLesMevesReserves = new Button(this);
        btnLesMevesReserves.setText("Les meves reserves");

        Button btnPistes = new Button(this);
        btnPistes.setText("Consultar pistes");

        Button btnTancarSessio = new Button(this);
        btnTancarSessio.setText("Tancar sessió");

        principal.addView(titol, crearParametres());
        principal.addView(benvinguda, crearParametres());
        principal.addView(rolUsuari, crearParametres());
        principal.addView(btnReservar, crearParametres());
        principal.addView(btnLesMevesReserves, crearParametres());
        principal.addView(btnPistes, crearParametres());
        principal.addView(btnTancarSessio, crearParametres());

        // Obrir la pantalla per crear una reserva.
        btnReservar.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    ReservaActivity.class
            );
            startActivity(intent);
        });

        // Obrir la pantalla amb les reserves de l'usuari.
        btnLesMevesReserves.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    LesMevesReservesActivity.class
            );
            startActivity(intent);
        });

        // Obrir la pantalla de consulta de pistes.
        btnPistes.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    PistesActivity.class
            );
            startActivity(intent);
        });

        // Tancar la sessió.
        btnTancarSessio.setOnClickListener(v -> tancarSessio());

        setContentView(principal);
    }

    private LinearLayout.LayoutParams crearParametres() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 16, 0, 0);

        return params;
    }

    private void tancarSessio() {

        // Esborrem les dades locals de la sessió.
        preferences.edit().clear().apply();

        tornarAlLogin();
    }

    private void tornarAlLogin() {

        Intent intent = new Intent(
                HomeActivity.this,
                MainActivityJava.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
