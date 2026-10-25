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
import android.widget.Toast;

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
        if (token.isEmpty()) {
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
        benvinguda.setText(
                "Benvingut/da, " + nom + " " + cognoms
        );
        benvinguda.setTextSize(22);
        benvinguda.setTextColor(Color.WHITE);
        benvinguda.setGravity(Gravity.CENTER);
        benvinguda.setPadding(0, 32, 0, 16);

        TextView rolUsuari = new TextView(this);
        rolUsuari.setText("Rol: " + rol);
        rolUsuari.setTextSize(16);
        rolUsuari.setTextColor(Color.LTGRAY);
        rolUsuari.setGravity(Gravity.CENTER);

        Button btnReserves = new Button(this);
        btnReserves.setText("Les meves reserves");

        Button btnPistes = new Button(this);
        btnPistes.setText("Consultar pistes");

        Button btnTancarSessio = new Button(this);
        btnTancarSessio.setText("Tancar sessió");

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 16, 0, 0);

        principal.addView(titol, params);
        principal.addView(benvinguda, params);
        principal.addView(rolUsuari, params);
        principal.addView(btnReserves, params);
        principal.addView(btnPistes, params);
        principal.addView(btnTancarSessio, params);

        btnReserves.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Prepararem la pantalla de reserves.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        btnPistes.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Prepararem la consulta de pistes.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        btnTancarSessio.setOnClickListener(v -> tancarSessio());

        setContentView(principal);
    }

    private void tancarSessio() {

        // De moment tanquem la sessió local.
        // Al pas següent també revocarem el token al servidor.

        preferences.edit().clear().commit();

        tornarAlLogin();
    }

    private void tornarAlLogin() {

        Intent intent = new Intent(this, MainActivityJava.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}