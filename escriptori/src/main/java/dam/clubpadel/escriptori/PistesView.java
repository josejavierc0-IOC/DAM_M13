
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.io.IOException;

public class PistesView {

    private final ApiClient api = new ApiClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final TableView<Pista> taula = new TableView<>();
    private final TextField campNom = new TextField();
    private final TextField campTipus = new TextField();
    private final TextArea campDescripcio = new TextArea();
    private final Label missatge = new Label();

    public Parent crearVista(Runnable tornar) {
        Label titol = new Label("Gestió de pistes");
        titol.getStyleClass().add("titol");

        TableColumn<Pista, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(55);

        TableColumn<Pista, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setPrefWidth(160);

        TableColumn<Pista, String> colTipus = new TableColumn<>("Tipus");
        colTipus.setCellValueFactory(new PropertyValueFactory<>("tipus"));
        colTipus.setPrefWidth(120);

        TableColumn<Pista, String> colDescripcio =
                new TableColumn<>("Descripció");
        colDescripcio.setCellValueFactory(
                new PropertyValueFactory<>("descripcio"));
        colDescripcio.setPrefWidth(220);

        TableColumn<Pista, String> colEstat =
                new TableColumn<>("Estat");
        colEstat.setCellValueFactory(
                new PropertyValueFactory<>("estatText"));
        colEstat.setPrefWidth(90);

        taula.getColumns().addAll(
                colId, colNom, colTipus, colDescripcio, colEstat
        );
        taula.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        campNom.setPromptText("Nom de la pista");
        campTipus.setPromptText("Ex.: interior o exterior");
        campDescripcio.setPromptText("Descripció");
        campDescripcio.setPrefRowCount(3);
        campDescripcio.setWrapText(true);

        taula.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, pista) -> {
                    if (pista != null) {
                        campNom.setText(pista.getNom());
                        campTipus.setText(pista.getTipus());
                        campDescripcio.setText(pista.getDescripcio());
                    }
                });

        Button botoCarregar = new Button("Actualitzar");
        botoCarregar.setOnAction(e -> carregarPistes());

        Button botoCrear = new Button("Crear pista");
        botoCrear.setOnAction(e -> crearPista());

        Button botoModificar = new Button("Desar modificacions");
        botoModificar.setOnAction(e -> modificarPista());

        Button botoDesactivar = new Button("Desactivar pista");
        botoDesactivar.setOnAction(e -> desactivarPista());

        Button botoNetejar = new Button("Netejar formulari");
        botoNetejar.setOnAction(e -> {
            taula.getSelectionModel().clearSelection();
            campNom.clear();
            campTipus.clear();
            campDescripcio.clear();
            missatge.setText("");
        });

        Button botoTornar = new Button("Tornar al menú");
        botoTornar.setOnAction(e -> tornar.run());

        VBox formulari = new VBox(
                8,
                new Label("Nom"),
                campNom,
                new Label("Tipus"),
                campTipus,
                new Label("Descripció"),
                campDescripcio,
                new HBox(8, botoCrear, botoModificar),
                new HBox(8, botoDesactivar, botoNetejar)
        );
        formulari.setPadding(new Insets(12));
        formulari.setPrefWidth(300);

        missatge.setWrapText(true);

        VBox contingut = new VBox(
                12,
                new HBox(12, titol, botoCarregar),
                new Label("Pistes registrades"),
                taula,
                new Label("Dades de la pista seleccionada"),
                formulari,
                missatge,
                botoTornar
        );
        contingut.setPadding(new Insets(20));

        VBox.setVgrow(taula, Priority.ALWAYS);

        carregarPistes();
        return contingut;
    }

    private void carregarPistes() {
        try {
            JsonNode resposta = api.get("pistes", Sessio.getToken());

            if (!resposta.path("success").asBoolean(false)) {
                mostrarError(resposta);
                return;
            }

            var pistes = FXCollections.<Pista>observableArrayList();
            JsonNode nodes = resposta.path("pistes");

            for (JsonNode node : nodes) {
                pistes.add(new Pista(
                        node.path("id").asInt(),
                        node.path("nom").asText(""),
                        node.path("tipus").asText(""),
                        node.path("descripcio").asText(""),
                        node.path("activa").asInt(0) == 1
                ));
            }

            taula.setItems(pistes);
            missatge.setText("Pistes carregades: " + pistes.size());

        } catch (Exception ex) {
            missatge.setText(
                    "Error en consultar les pistes: " + ex.getMessage()
            );
        }
    }

    private void crearPista() {
        if (!validarFormulari()) {
            return;
        }

        try {
            ObjectNode dades = dadesFormulari();
            JsonNode resposta = api.post(
                    "pistes", Sessio.getToken(), dades
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message").asText(
                        "Pista creada correctament"));
                netejar();
                carregarPistes();
            } else {
                mostrarError(resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en crear la pista: " + ex.getMessage()
            );
        }
    }

    private void modificarPista() {
        Pista seleccionada = taula.getSelectionModel()
                .getSelectedItem();

        if (seleccionada == null) {
            missatge.setText("Selecciona una pista de la taula.");
            return;
        }

        if (!validarFormulari()) {
            return;
        }

        try {
            ObjectNode dades = dadesFormulari();

            JsonNode resposta = api.put(
                    "pistes&id=" + seleccionada.getId(),
                    Sessio.getToken(),
                    dades
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message").asText(
                        "Pista modificada correctament"));
                carregarPistes();
            } else {
                mostrarError(resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en modificar la pista: " + ex.getMessage()
            );
        }
    }

    private void desactivarPista() {
        Pista seleccionada = taula.getSelectionModel()
                .getSelectedItem();

        if (seleccionada == null) {
            missatge.setText("Selecciona una pista de la taula.");
            return;
        }

        if (!seleccionada.isActiva()) {
            missatge.setText("Aquesta pista ja està desactivada.");
            return;
        }

        Alert confirmacio = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Vols desactivar la pista «"
                        + seleccionada.getNom() + "»?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirmacio.setHeaderText("Confirmació de baixa");

        if (confirmacio.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            JsonNode resposta = api.delete(
                    "pistes&id=" + seleccionada.getId(),
                    Sessio.getToken()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message").asText(
                        "Pista desactivada correctament"));
                carregarPistes();
            } else {
                mostrarError(resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en desactivar la pista: " + ex.getMessage()
            );
        }
    }

    private boolean validarFormulari() {
        if (campNom.getText().trim().isEmpty()) {
            missatge.setText("El nom de la pista és obligatori.");
            return false;
        }
        return true;
    }

    private ObjectNode dadesFormulari() {
        ObjectNode dades = mapper.createObjectNode();
        dades.put("nom", campNom.getText().trim());
        dades.put("tipus", campTipus.getText().trim());
        dades.put("descripcio", campDescripcio.getText().trim());
        return dades;
    }

    private void netejar() {
        taula.getSelectionModel().clearSelection();
        campNom.clear();
        campTipus.clear();
        campDescripcio.clear();
    }

    private void mostrarError(JsonNode resposta) {
        missatge.setText(resposta.path("message")
                .asText("L'operació no s'ha pogut completar."));
    }

    public static class Pista {
        private final int id;
        private final String nom;
        private final String tipus;
        private final String descripcio;
        private final boolean activa;

        public Pista(int id, String nom, String tipus,
                     String descripcio, boolean activa) {
            this.id = id;
            this.nom = nom;
            this.tipus = tipus;
            this.descripcio = descripcio;
            this.activa = activa;
        }

        public int getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public String getTipus() {
            return tipus;
        }

        public String getDescripcio() {
            return descripcio;
        }

        public boolean isActiva() {
            return activa;
        }

        public String getEstatText() {
            return activa ? "Activa" : "Inactiva";
        }
    }
}
