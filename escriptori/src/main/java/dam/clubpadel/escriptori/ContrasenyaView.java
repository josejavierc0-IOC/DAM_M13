
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ContrasenyaView {

    private final ApiClient api = new ApiClient();

    public Parent crearVista(Runnable tornar) {
        Label titol = new Label("Canviar contrasenya");
        titol.getStyleClass().add("titol");

        PasswordField actual = new PasswordField();
        actual.setPromptText("Contrasenya actual");

        PasswordField nova = new PasswordField();
        nova.setPromptText("Nova contrasenya");

        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Repeteix la nova contrasenya");

        Label missatge = new Label();
        missatge.setWrapText(true);

        Button desar = new Button("Canviar contrasenya");
        Button enrere = new Button("Tornar al menú");

        desar.setOnAction(e -> {
            String a = actual.getText();
            String n = nova.getText();
            String c = confirmar.getText();

            if (a.isBlank() || n.isBlank() || c.isBlank()) {
                missatge.setText("Omple tots els camps.");
                return;
            }

            if (n.length() < 8) {
                missatge.setText(
                        "La nova contrasenya ha de tenir almenys 8 caràcters.");
                return;
            }

            if (!n.equals(c)) {
                missatge.setText("Les contrasenyes noves no coincideixen.");
                return;
            }

            try {
                JsonNode resposta = api.canviarContrasenya(
                        Sessio.getToken(), a, n);

                if (resposta.path("success").asBoolean(false)) {
                    missatge.setText("Contrasenya canviada correctament.");
                    actual.clear();
                    nova.clear();
                    confirmar.clear();
                } else {
                    missatge.setText(resposta.path("message")
                            .asText("No s'ha pogut canviar la contrasenya."));
                }
            } catch (IOException ex) {
                missatge.setText("Error de comunicació: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                missatge.setText("La petició s'ha interromput.");
            }
        });

        enrere.setOnAction(e -> tornar.run());

        VBox arrel = new VBox(12, titol, actual, nova, confirmar,
                desar, missatge, enrere);
        arrel.setPadding(new Insets(30));
        arrel.setMaxWidth(520);
        return arrel;
    }
}
