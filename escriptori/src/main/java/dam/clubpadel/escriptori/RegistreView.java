
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class RegistreView {

    private final ApiClient api = new ApiClient();

    public Parent crearVista(Runnable tornarLogin) {
        Label titol = new Label("IOC Padel Pass");
        titol.getStyleClass().add("titol");

        Label subtitol = new Label("Registre de soci");
        subtitol.getStyleClass().add("subtitol");

        TextField nom = new TextField();
        nom.setPromptText("Nom");

        TextField cognoms = new TextField();
        cognoms.setPromptText("Cognoms");

        TextField email = new TextField();
        email.setPromptText("Correu electrònic");

        TextField telefon = new TextField();
        telefon.setPromptText("Telèfon (opcional)");

        PasswordField password = new PasswordField();
        password.setPromptText("Contrasenya (mínim 8 caràcters)");

        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Repeteix la contrasenya");

        Label missatge = new Label();
        missatge.getStyleClass().add("missatge");
        missatge.setWrapText(true);

        Button registrar = new Button("Registrar-se");
        registrar.getStyleClass().add("boto-principal");
        registrar.setMaxWidth(Double.MAX_VALUE);

        Button tornar = new Button("Tornar a l'inici de sessió");
        tornar.setMaxWidth(Double.MAX_VALUE);
        tornar.setOnAction(e -> tornarLogin.run());

        registrar.setOnAction(e -> {
            String nomText = nom.getText().trim();
            String cognomsText = cognoms.getText().trim();
            String emailText = email.getText().trim();
            String telefonText = telefon.getText().trim();
            String passwordText = password.getText();
            String confirmarText = confirmar.getText();

            if (nomText.isEmpty() || cognomsText.isEmpty()
                    || emailText.isEmpty() || passwordText.isEmpty()) {
                missatge.setText(
                        "Omple el nom, els cognoms, el correu i la contrasenya."
                );
                return;
            }

            if (passwordText.length() < 8) {
                missatge.setText(
                        "La contrasenya ha de tenir com a mínim 8 caràcters."
                );
                return;
            }

            if (!passwordText.equals(confirmarText)) {
                missatge.setText("Les contrasenyes no coincideixen.");
                return;
            }

            registrar.setDisable(true);
            missatge.setText("Registrant el soci...");

            try {
                JsonNode resposta = api.registrarUsuari(
                        nomText,
                        cognomsText,
                        emailText,
                        passwordText,
                        telefonText
                );

                if (resposta.path("success").asBoolean(false)) {
                    missatge.setText(
                            "Registre completat. Ja pots tornar a l'inici "
                            + "de sessió."
                    );
                    nom.clear();
                    cognoms.clear();
                    email.clear();
                    telefon.clear();
                    password.clear();
                    confirmar.clear();
                } else {
                    missatge.setText(
                            resposta.path("message")
                                    .asText("No s'ha pogut completar el registre.")
                    );
                }

            } catch (IOException ex) {
                missatge.setText(
                        "Error de comunicació amb el servidor: "
                        + ex.getMessage()
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                missatge.setText("La petició s'ha interromput.");
            } finally {
                registrar.setDisable(false);
            }
        });

        VBox formulari = new VBox(
                10,
                titol,
                subtitol,
                new Label("Nom"),
                nom,
                new Label("Cognoms"),
                cognoms,
                new Label("Correu electrònic"),
                email,
                new Label("Telèfon"),
                telefon,
                new Label("Contrasenya"),
                password,
                new Label("Confirma la contrasenya"),
                confirmar,
                registrar,
                tornar,
                missatge
        );

        formulari.setPadding(new Insets(25));
        formulari.setAlignment(Pos.CENTER_LEFT);
        formulari.setMaxWidth(430);
        formulari.getStyleClass().add("formulari");

        BorderPane arrel = new BorderPane();
        arrel.setCenter(formulari);

        return arrel;
    }
}
