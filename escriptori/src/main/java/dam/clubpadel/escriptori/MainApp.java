package dam.clubpadel.escriptori;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Label titol = new Label("IOC Padel Pass");
        titol.getStyleClass().add("titol");

        Label subtitol = new Label("Administració del club");
        subtitol.getStyleClass().add("subtitol");

        Label etiquetaUsuari = new Label("Correu electrònic");
        TextField campUsuari = new TextField();
        campUsuari.setPromptText("admin@clubpadel.cat");
        campUsuari.setMaxWidth(Double.MAX_VALUE);

        Label etiquetaContrasenya = new Label("Contrasenya");
        PasswordField campContrasenya = new PasswordField();
        campContrasenya.setPromptText("Introdueix la contrasenya");
        campContrasenya.setMaxWidth(Double.MAX_VALUE);

        Label missatge = new Label();
        missatge.getStyleClass().add("missatge");
        missatge.setWrapText(true);

        Button botoEntrar = new Button("Iniciar sessió");
        botoEntrar.getStyleClass().add("boto-principal");
        botoEntrar.setMaxWidth(Double.MAX_VALUE);
        botoEntrar.setDefaultButton(true);

        botoEntrar.setOnAction(event -> {
            String usuari = campUsuari.getText().trim();
            String contrasenya = campContrasenya.getText();

            if (usuari.isEmpty() || contrasenya.isEmpty()) {
                missatge.setText(
                    "Cal introduir el correu electrònic i la contrasenya."
                );
                return;
            }

            missatge.setText(
                "Formulari validat. La connexió amb el servidor "
                + "encara està pendent de configurar."
            );
        });

        Label peu = new Label("Aplicació interna del personal del club");
        peu.getStyleClass().add("peu");

        VBox formulari = new VBox(
            10,
            titol,
            subtitol,
            etiquetaUsuari,
            campUsuari,
            etiquetaContrasenya,
            campContrasenya,
            botoEntrar,
            missatge
        );

        formulari.setAlignment(Pos.CENTER_LEFT);
        formulari.setPadding(new Insets(28));
        formulari.setMaxWidth(420);
        formulari.getStyleClass().add("formulari");

        VBox capcalera = new VBox(5, titol, subtitol);
        capcalera.setAlignment(Pos.CENTER_LEFT);

        // El títol es mostra a la part superior del formulari.
        formulari.getChildren().removeAll(titol, subtitol);
        formulari.getChildren().add(0, capcalera);

        BorderPane arrel = new BorderPane();
        arrel.setCenter(formulari);
        arrel.setBottom(peu);
        BorderPane.setAlignment(peu, Pos.CENTER);
        BorderPane.setMargin(peu, new Insets(0, 0, 18, 0));

        Scene escena = new Scene(arrel, 800, 600);
        escena.getStylesheets().add(
            getClass().getResource("estils.css").toExternalForm()
        );

        stage.setTitle("IOC Padel Pass - Administració");
        stage.setMinWidth(650);
        stage.setMinHeight(500);
        stage.setScene(escena);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
