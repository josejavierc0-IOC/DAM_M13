package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private final ApiClient api = new ApiClient();
    private Stage finestra;

    @Override
    public void start(Stage stage) {
        finestra = stage;
        mostrarLogin();
    }

    private void mostrarLogin() {
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

        Button botoRecuperar = new Button("He oblidat la contrasenya");
        botoRecuperar.setOnAction(event ->
                mostrarRecuperarContrasenya());

        Button botoRegistrar = new Button("Registrar un soci");
        botoRegistrar.setOnAction(event ->
                mostrarRegistre());

        botoEntrar.setOnAction(event -> {
            String email = campUsuari.getText().trim();
            String password = campContrasenya.getText();

            if (email.isEmpty() || password.isEmpty()) {
                missatge.setText(
                        "Cal introduir el correu electrònic i la contrasenya."
                );
                return;
            }

            botoEntrar.setDisable(true);
            missatge.setText("Connectant amb el servidor...");

            try {
                JsonNode resposta = api.iniciarSessio(email, password);

                if (!resposta.path("success").asBoolean(false)) {
                    missatge.setText(
                            resposta.path("message")
                                    .asText("No s'ha pogut iniciar la sessió.")
                    );
                    return;
                }

                JsonNode usuari = resposta.path("usuari");
                String rol = usuari.path("rol").asText("");

                if (!"ADMIN".equalsIgnoreCase(rol)) {
                    missatge.setText(
                            "Accés denegat. Només hi poden entrar administradors."
                    );
                    return;
                }

                String token = resposta.path("token").asText("");
                if (token.isBlank()) {
                    missatge.setText(
                            "El servidor no ha retornat cap token de sessió."
                    );
                    return;
                }

                String nom = usuari.path("nom").asText("");
                String cognoms = usuari.path("cognoms").asText("");

                String nomComplet = (nom + " " + cognoms).trim();

                Sessio.iniciar(token, nomComplet, email, rol);
                mostrarPrincipal();

            } catch (IOException ex) {
                missatge.setText(
                        "No s'ha pogut contactar amb l'API. "
                        + "Comprova que Apache i MySQL estiguin en marxa "
                        + "i que l'URL del servidor sigui correcte."
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                missatge.setText("La petició s'ha interromput.");
            } finally {
                botoEntrar.setDisable(false);
            }
        });

        Label peu = new Label(
                "Aplicació interna del personal del club"
        );
        peu.getStyleClass().add("peu");

        VBox capcalera = new VBox(5, titol, subtitol);
        capcalera.setAlignment(Pos.CENTER_LEFT);

        VBox formulari = new VBox(
            10,
            capcalera,
            etiquetaUsuari,
            campUsuari,
            etiquetaContrasenya,
            campContrasenya,
            botoEntrar,
            botoRecuperar,
            botoRegistrar,
            missatge
    );

        formulari.setAlignment(Pos.CENTER_LEFT);
        formulari.setPadding(new Insets(28));
        formulari.setMaxWidth(420);
        formulari.getStyleClass().add("formulari");

        BorderPane arrel = new BorderPane();
        arrel.setCenter(formulari);
        arrel.setBottom(peu);
        BorderPane.setAlignment(peu, Pos.CENTER);
        BorderPane.setMargin(peu, new Insets(0, 0, 18, 0));

        Scene escena = new Scene(arrel, 800, 600);
        escena.getStylesheets().add(
                getClass().getResource("estils.css").toExternalForm()
        );

        finestra.setTitle("IOC Padel Pass - Administració");
        finestra.setMinWidth(650);
        finestra.setMinHeight(500);
        finestra.setScene(escena);
        finestra.centerOnScreen();
        finestra.show();
    }

    private void mostrarPrincipal() {
        Label titol = new Label("IOC Padel Pass");
        titol.getStyleClass().add("titol");

        String nom = Sessio.getNomUsuari();
        Label benvinguda = new Label(
                "Benvingut/da"
                + (nom == null || nom.isBlank() ? "" : ", " + nom)
        );

        Button botoPistes = new Button("Gestió de pistes");
        Button botoReserves = new Button("Reserves");
        Button botoIncidencies = new Button("Incidències");
        Button botoTarifes = new Button("Tarifes");
        Button botoNotificacions = new Button("Notificacions");

        Label missatge = new Label(
                "Sessió iniciada correctament. Selecciona una opció."
        );
        missatge.setWrapText(true);

        botoPistes.setOnAction(event -> mostrarPistes());

        botoReserves.setOnAction(event -> mostrarReserves());

        botoIncidencies.setOnAction(event ->
                missatge.setText(
                        "La gestió d'incidències encara està pendent d'integrar."
                )
        );

        botoTarifes.setOnAction(event -> mostrarTarifes());

        botoNotificacions.setOnAction(event ->
                missatge.setText(
                        "Les notificacions encara estan pendents d'integrar."
                )
        );

        Button botoSortir = new Button("Tancar sessió");
        botoSortir.setOnAction(event -> {
            Sessio.tancar();
            mostrarLogin();
        });

        VBox menu = new VBox(
                12,
                titol,
                benvinguda,
                new Separator(),
                botoPistes,
                botoReserves,
                botoIncidencies,
                botoTarifes,
                botoNotificacions,
                new Separator(),
                missatge,
                botoSortir
        );

        menu.setPadding(new Insets(30));
        menu.setAlignment(Pos.TOP_LEFT);

        for (var node : menu.getChildren()) {
            if (node instanceof Button boto) {
                boto.setMaxWidth(Double.MAX_VALUE);
            }
        }

        BorderPane arrel = new BorderPane();
        arrel.setCenter(menu);

        Scene escena = new Scene(arrel, 850, 650);
        escena.getStylesheets().add(
                getClass().getResource("estils.css").toExternalForm()
        );

        finestra.setTitle("IOC Padel Pass - Panell d'administració");
        finestra.setScene(escena);
        finestra.centerOnScreen();
        finestra.show();
    }

    private void mostrarPistes() {
        PistesView vista = new PistesView();

        Scene escena = new Scene(
                vista.crearVista(this::mostrarPrincipal),
                1000,
                750
        );

        escena.getStylesheets().add(
                getClass().getResource("estils.css").toExternalForm()
        );

        finestra.setTitle("IOC Padel Pass - Gestió de pistes");
        finestra.setScene(escena);
        finestra.centerOnScreen();
        finestra.show();
    }

    private void mostrarTarifes() {
        TarifesView vista = new TarifesView();

        Scene escena = new Scene(
                vista.crearVista(this::mostrarPrincipal),
                1000,
                750
        );

        escena.getStylesheets().add(
                getClass().getResource("estils.css").toExternalForm()
        );

        finestra.setTitle("IOC Padel Pass - Gestió de tarifes");
        finestra.setScene(escena);
        finestra.centerOnScreen();
        finestra.show();
    }

    private void mostrarReserves() {
    ReservesView vista = new ReservesView();

    Scene escena = new Scene(
            vista.crearVista(this::mostrarPrincipal),
            1200,
            850
    );

    escena.getStylesheets().add(
            getClass().getResource("estils.css").toExternalForm()
    );

    finestra.setTitle("IOC Padel Pass - Gestió de reserves");
    finestra.setScene(escena);
    finestra.centerOnScreen();
    finestra.show();
}

    private void mostrarRecuperarContrasenya() {
        RecuperarContrasenyaView vista =
                new RecuperarContrasenyaView();

        Scene escena = new Scene(
                vista.crearVista(this::mostrarLogin),
                800,
                650
        );

        escena.getStylesheets().add(
                getClass().getResource("estils.css").toExternalForm()
        );

        finestra.setTitle("IOC Padel Pass - Recuperació de contrasenya");
        finestra.setScene(escena);
        finestra.centerOnScreen();
        finestra.show();
    }

    private void mostrarRegistre() {
        RegistreView vista = new RegistreView();

        Scene escena = new Scene(
                vista.crearVista(this::mostrarLogin),
                800,
                700
        );

        escena.getStylesheets().add(
                getClass().getResource("estils.css").toExternalForm()
        );

        finestra.setTitle("IOC Padel Pass - Registre de soci");
        finestra.setScene(escena);
        finestra.centerOnScreen();
        finestra.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}