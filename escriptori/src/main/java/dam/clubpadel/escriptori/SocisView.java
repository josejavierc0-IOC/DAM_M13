
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SocisView {

    private static final String API_URL =
            "http://localhost/DAM_M13/servidor/index.php";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final ObservableList<JsonNode> socis =
            FXCollections.observableArrayList();

    private final TableView<JsonNode> taula = new TableView<>();
    private final TextField cerca = new TextField();

    private final TextField campNom = new TextField();
    private final TextField campCognoms = new TextField();
    private final TextField campEmail = new TextField();
    private final TextField campTelefon = new TextField();
    private final ComboBox<String> campEstat = new ComboBox<>();

    private final Label missatge = new Label();
    private final Label etiquetaSeleccionat = new Label();

    private Button botoDesar;
    private Button botoCarregar;

    private Runnable tornarMenu;
    private JsonNode sociSeleccionat;

    public Parent crearVista(Runnable tornarMenu) {
        this.tornarMenu = tornarMenu;

        BorderPane arrel = new BorderPane();
        arrel.setPadding(new Insets(20));

        Label titol = new Label("Gestió de socis");
        titol.getStyleClass().add("titol");

        Label subtitol = new Label(
                "Consulta, cerca i modifica les dades dels socis del club."
        );

        VBox capcalera = new VBox(6, titol, subtitol);
        capcalera.setPadding(new Insets(0, 0, 15, 0));
        arrel.setTop(capcalera);

        configurarTaula();

        cerca.setPromptText("Cerca per nom, cognoms o correu electrònic");
        cerca.textProperty().addListener(
                (observable, anterior, nou) -> aplicarFiltre()
        );

        botoCarregar = new Button("Actualitzar llista");
        botoCarregar.setOnAction(event -> carregarSocis());

        VBox zonaLlista = new VBox(
                10,
                new Label("Socis registrats"),
                new HBox(10, cerca, botoCarregar),
                taula
        );
        zonaLlista.setPadding(new Insets(0, 15, 0, 0));
        VBox.setVgrow(taula, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(cerca, javafx.scene.layout.Priority.ALWAYS);

        configurarFormulari();

        VBox contingut = new VBox(12, zonaLlista, crearFormulari());
        arrel.setCenter(contingut);

        Button botoTornar = new Button("Tornar al menú principal");
        botoTornar.setOnAction(event -> this.tornarMenu.run());

        HBox peu = new HBox(10, botoTornar, missatge);
        peu.setAlignment(Pos.CENTER_LEFT);
        peu.setPadding(new Insets(15, 0, 0, 0));
        arrel.setBottom(peu);

        carregarSocis();

        return arrel;
    }

    private void configurarTaula() {
        taula.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
        taula.setPlaceholder(new Label("No s'han trobat socis."));

        TableColumn<JsonNode, String> colNumero =
                columna("Número soci", "numero_soci");
        TableColumn<JsonNode, String> colNom =
                columna("Nom", "nom");
        TableColumn<JsonNode, String> colCognoms =
                columna("Cognoms", "cognoms");
        TableColumn<JsonNode, String> colEmail =
                columna("Correu electrònic", "email");
        TableColumn<JsonNode, String> colTelefon =
                columna("Telèfon", "telefon");

        taula.getColumns().setAll(
                colNumero, colNom, colCognoms, colEmail, colTelefon
        );

        taula.setItems(socis);

        taula.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, seleccionat) -> seleccionarSoci(seleccionat)
        );
    }

    private TableColumn<JsonNode, String> columna(
            String titol,
            String camp
    ) {
        TableColumn<JsonNode, String> columna =
                new TableColumn<>(titol);

        columna.setCellValueFactory(
                dada -> new javafx.beans.property.SimpleStringProperty(
                        dada.getValue().path(camp).asText("")
                )
        );

        return columna;
    }

    private void configurarFormulari() {
        campEstat.getItems().setAll("Actiu", "Inactiu");
        campEstat.setMaxWidth(Double.MAX_VALUE);
    }

    private VBox crearFormulari() {
        Label titolFormulari = new Label("Dades del soci seleccionat");

        etiquetaSeleccionat.setText(
                "Selecciona un soci de la taula per editar-ne les dades."
        );

        GridPane camps = new GridPane();
        camps.setHgap(12);
        camps.setVgap(8);

        camps.addRow(0, new Label("Nom"), campNom);
        camps.addRow(1, new Label("Cognoms"), campCognoms);
        camps.addRow(2, new Label("Correu electrònic"), campEmail);
        camps.addRow(3, new Label("Telèfon"), campTelefon);
        camps.addRow(4, new Label("Estat"), campEstat);

        campNom.setMaxWidth(Double.MAX_VALUE);
        campCognoms.setMaxWidth(Double.MAX_VALUE);
        campEmail.setMaxWidth(Double.MAX_VALUE);
        campTelefon.setMaxWidth(Double.MAX_VALUE);

        GridPane.setHgrow(campNom, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(campCognoms, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(campEmail, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(campTelefon, javafx.scene.layout.Priority.ALWAYS);

        botoDesar = new Button("Desar canvis");
        botoDesar.setDisable(true);
        botoDesar.setOnAction(event -> desarCanvis());

        Button botoNetejar = new Button("Netejar selecció");
        botoNetejar.setOnAction(event -> {
            taula.getSelectionModel().clearSelection();
            seleccionarSoci(null);
        });

        HBox botons = new HBox(10, botoDesar, botoNetejar);

        VBox formulari = new VBox(
                10,
                new Separator(),
                titolFormulari,
                etiquetaSeleccionat,
                camps,
                botons
        );

        formulari.setPadding(new Insets(15, 0, 0, 0));
        return formulari;
    }

    private void carregarSocis() {
        if (!Sessio.esAdmin() || Sessio.getToken() == null) {
            mostrarMissatge("Cal iniciar sessió com a administrador.");
            return;
        }

        botoCarregar.setDisable(true);
        mostrarMissatge("Carregant socis...");

        try {
            JsonNode resposta = peticio("GET", "socis", null);

            if (!resposta.path("success").asBoolean(false)) {
                mostrarMissatge(
                        resposta.path("message").asText(
                                "No s'han pogut carregar els socis."
                        )
                );
                return;
            }

            socis.clear();

            JsonNode dades = resposta.path("data");
            if (dades.isArray()) {
                for (JsonNode soci : dades) {
                    socis.add(soci);
                }
            }

            aplicarFiltre();
            mostrarMissatge("Socis carregats: " + socis.size());

        } catch (IOException ex) {
            mostrarMissatge("Error de connexió amb el servidor: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            mostrarMissatge("La consulta s'ha interromput.");
        } finally {
            botoCarregar.setDisable(false);
        }
    }

    private void aplicarFiltre() {
        String text = cerca.getText() == null
                ? ""
                : cerca.getText().trim().toLowerCase();

        ObservableList<JsonNode> filtrats =
                FXCollections.observableArrayList();

        for (JsonNode soci : socis) {
            String nom = soci.path("nom").asText("").toLowerCase();
            String cognoms = soci.path("cognoms").asText("").toLowerCase();
            String email = soci.path("email").asText("").toLowerCase();
            String numero = soci.path("numero_soci").asText("").toLowerCase();

            if (nom.contains(text)
                    || cognoms.contains(text)
                    || email.contains(text)
                    || numero.contains(text)) {
                filtrats.add(soci);
            }
        }

        taula.setItems(filtrats);
    }

    private void seleccionarSoci(JsonNode soci) {
        sociSeleccionat = soci;

        boolean seleccionat = soci != null;
        campNom.setDisable(!seleccionat);
        campCognoms.setDisable(!seleccionat);
        campEmail.setDisable(!seleccionat);
        campTelefon.setDisable(!seleccionat);
        campEstat.setDisable(!seleccionat);
        botoDesar.setDisable(!seleccionat);

        if (!seleccionat) {
            campNom.clear();
            campCognoms.clear();
            campEmail.clear();
            campTelefon.clear();
            campEstat.getSelectionModel().clearSelection();
            etiquetaSeleccionat.setText("Cap soci seleccionat.");
            return;
        }

        campNom.setText(soci.path("nom").asText(""));
        campCognoms.setText(soci.path("cognoms").asText(""));
        campEmail.setText(soci.path("email").asText(""));
        campTelefon.setText(soci.path("telefon").asText(""));

        campEstat.setValue(
                soci.path("actiu").asInt(0) == 1
                        ? "Actiu"
                        : "Inactiu"
        );

        etiquetaSeleccionat.setText(
                "Número de soci: "
                        + soci.path("numero_soci").asText("")
                        + " | Data d'alta: "
                        + soci.path("data_alta").asText("")
        );

        mostrarMissatge("");
    }

    private void desarCanvis() {
        if (sociSeleccionat == null) {
            mostrarMissatge("Selecciona primer un soci.");
            return;
        }

        String nom = campNom.getText().trim();
        String cognoms = campCognoms.getText().trim();
        String email = campEmail.getText().trim();
        String telefon = campTelefon.getText().trim();
        String estat = campEstat.getValue();

        if (nom.isEmpty() || cognoms.isEmpty() || email.isEmpty()
                || estat == null) {
            mostrarMissatge("Omple el nom, els cognoms, el correu i l'estat.");
            return;
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            mostrarMissatge("Introdueix un correu electrònic vàlid.");
            return;
        }

        ObjectNode dades = mapper.createObjectNode();
        dades.put("nom", nom);
        dades.put("cognoms", cognoms);
        dades.put("email", email);
        dades.put("telefon", telefon);
        dades.put("actiu", "Actiu".equals(estat) ? 1 : 0);

        int id = sociSeleccionat.path("id").asInt();

        botoDesar.setDisable(true);
        mostrarMissatge("Desant canvis...");

        try {
            JsonNode resposta = peticio(
                    "PUT",
                    "socis&id=" + id,
                    dades
            );

            if (!resposta.path("success").asBoolean(false)) {
                mostrarMissatge(
                        resposta.path("message").asText(
                                "No s'han pogut desar els canvis."
                        )
                );
                return;
            }

            mostrarMissatge(
                    resposta.path("message").asText("Canvis desats correctament.")
            );

            carregarSocis();

        } catch (IOException ex) {
            mostrarMissatge("Error en desar les dades: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            mostrarMissatge("El desament s'ha interromput.");
        } finally {
            botoDesar.setDisable(sociSeleccionat == null);
        }
    }

    private JsonNode peticio(
            String metode,
            String recurs,
            JsonNode dades
    ) throws IOException, InterruptedException {

        String token = Sessio.getToken();

        if (token == null || token.isBlank()) {
            throw new IOException("No hi ha cap token de sessió.");
        }

        String separador = recurs.contains("?") ? "&" : "?";
        String url = API_URL + "?resource=" + recurs;

        HttpRequest.Builder constructor = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json");

        if ("GET".equals(metode)) {
            constructor.GET();
        } else if ("PUT".equals(metode)) {
            constructor.PUT(
                    HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(dades)
                    )
            );
        } else {
            throw new IOException("Mètode HTTP no admès: " + metode);
        }

        HttpResponse<String> resposta = client.send(
                constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        JsonNode json;
        try {
            json = mapper.readTree(resposta.body());
        } catch (Exception ex) {
            throw new IOException(
                    "El servidor ha retornat una resposta no vàlida (HTTP "
                            + resposta.statusCode() + ")."
            );
        }

        if (resposta.statusCode() >= 400
                && !json.has("success")) {
            throw new IOException("Error HTTP " + resposta.statusCode());
        }

        return json;
    }

    private void mostrarMissatge(String text) {
        missatge.setText(text == null ? "" : text);
    }
}
