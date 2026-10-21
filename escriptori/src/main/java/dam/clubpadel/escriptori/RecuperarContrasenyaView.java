
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class RecuperarContrasenyaView {

    private final ApiClient api = new ApiClient();

    public Parent crearVista(Runnable tornar) {
        Label titol = new Label("Recuperar contrasenya");
        titol.getStyleClass().add("titol");

        TextField email = new TextField();
        email.setPromptText("Correu electrònic");

        Button sollicitar = new Button("Sol·licitar recuperació");

        TextField token = new TextField();
        token.setPromptText("Token de recuperació");

        PasswordField nova = new PasswordField();
        nova.setPromptText("Nova contrasenya");

        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Confirma la contrasenya");

        Button restablir = new Button("Restablir contrasenya");
        Button enrere = new Button("Tornar a l'inici de sessió");

        Label missatge = new Label();
        missatge.setWrapText(true);

        sollicitar.setOnAction(e -> {
            if (email.getText().isBlank()) {
                missatge.setText("Introdueix el correu electrònic.");
                return;
            }

            try {
                JsonNode resposta =
                        api.sollicitarRecuperacio(email.getText().trim());

                if (resposta.path("success").asBoolean(false)) {
                    String recuperacio =
                            resposta.path("token").asText("");
                    if (!recuperacio.isBlank()) {
                        token.setText(recuperacio);
                        missatge.setText(
                                "Token de desenvolupament rebut. "
                                + "Enviament per correu no disponible.");
                    } else {
                        missatge.setText(
                                "Si el compte existeix, rebràs instruccions "
                                + "de recuperació.");
                    }
                } else {
                    missatge.setText(resposta.path("message")
                            .asText("No s'ha pogut sol·licitar la recuperació."));
                }
            } catch (IOException ex) {
                missatge.setText("Error de comunicació: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                missatge.setText("La petició s'ha interromput.");
            }
        });

        restablir.setOnAction(e -> {
            String t = token.getText().trim();
            String n = nova.getText();
            String c = confirmar.getText();

            if (t.isBlank() || n.isBlank() || c.isBlank()) {
                missatge.setText("Omple tots els camps.");
                return;
            }

            if (n.length() < 8) {
                missatge.setText(
                        "La contrasenya ha de tenir almenys 8 caràcters.");
                return;
            }

            if (!n.equals(c)) {
                missatge.setText("Les contrasenyes no coincideixen.");
                return;
            }

            try {
                JsonNode resposta = api.restablirContrasenya(t, n);

                if (resposta.path("success").asBoolean(false)) {
                    missatge.setText(
                            "Contrasenya restablerta. Ja pots iniciar sessió.");
                    token.clear();
                    nova.clear();
                    confirmar.clear();
                } else {
                    missatge.setText(resposta.path("message")
                            .asText("No s'ha pogut restablir la contrasenya."));
                }
            } catch (IOException ex) {
                missatge.setText("Error de comunicació: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                missatge.setText("La petició s'ha interromput.");
            }
        });

        enrere.setOnAction(e -> tornar.run());

        VBox arrel = new VBox(12, titol, email, sollicitar,
                new Separator(), token, nova, confirmar, restablir,
                missatge, enrere);
        arrel.setPadding(new Insets(30));
        arrel.setMaxWidth(560);
        return arrel;
    }
}
