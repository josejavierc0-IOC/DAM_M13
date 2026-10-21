
package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class TarifesView {

    private final ApiClient api = new ApiClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final TableView<Tarifa> taula = new TableView<>();

    private final TextField campNom = new TextField();
    private final TextField campPreu = new TextField();
    private final TextField campDurada = new TextField();
    private final TextField campDataInici = new TextField();
    private final TextField campDataFi = new TextField();

    private final Label missatge = new Label();

    public Parent crearVista(Runnable tornar) {

        Label titol = new Label("Gestió de tarifes");
        titol.getStyleClass().add("titol");

        TableColumn<Tarifa, Integer> colId =
                new TableColumn<>("ID");
        colId.setCellValueFactory(
                new PropertyValueFactory<>("id"));
        colId.setPrefWidth(55);

        TableColumn<Tarifa, String> colNom =
                new TableColumn<>("Nom");
        colNom.setCellValueFactory(
                new PropertyValueFactory<>("nom"));

        TableColumn<Tarifa, String> colPreu =
                new TableColumn<>("Preu (€)");
        colPreu.setCellValueFactory(
                new PropertyValueFactory<>("preu"));

        TableColumn<Tarifa, Integer> colDurada =
                new TableColumn<>("Durada (min)");
        colDurada.setCellValueFactory(
                new PropertyValueFactory<>("duradaMinuts"));

        TableColumn<Tarifa, String> colDataInici =
                new TableColumn<>("Data inici");
        colDataInici.setCellValueFactory(
                new PropertyValueFactory<>("dataInici"));

        TableColumn<Tarifa, String> colDataFi =
                new TableColumn<>("Data fi");
        colDataFi.setCellValueFactory(
                new PropertyValueFactory<>("dataFi"));

        TableColumn<Tarifa, String> colEstat =
                new TableColumn<>("Estat");
        colEstat.setCellValueFactory(
                new PropertyValueFactory<>("estatText"));

        taula.getColumns().addAll(
                colId, colNom, colPreu, colDurada,
                colDataInici, colDataFi, colEstat
        );

        taula.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        campNom.setPromptText("Nom de la tarifa");
        campPreu.setPromptText("Ex.: 20.00");
        campDurada.setPromptText("Ex.: 60");
        campDataInici.setPromptText("AAAA-MM-DD");
        campDataFi.setPromptText("AAAA-MM-DD");

        campDataInici.setText(
                LocalDate.now().toString());
        campDataFi.setText(
                LocalDate.now().plusMonths(1).toString());

        taula.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, tarifa) -> {
                    if (tarifa != null) {
                        campNom.setText(tarifa.getNom());
                        campPreu.setText(tarifa.getPreu());
                        campDurada.setText(
                                String.valueOf(tarifa.getDuradaMinuts()));
                        campDataInici.setText(tarifa.getDataInici());
                        campDataFi.setText(tarifa.getDataFi());
                    }
                });

        Button botoActualitzar = new Button("Actualitzar");
        botoActualitzar.setOnAction(e -> carregarTarifes());

        Button botoCrear = new Button("Crear tarifa");
        botoCrear.setOnAction(e -> crearTarifa());

        Button botoModificar = new Button("Desar modificacions");
        botoModificar.setOnAction(e -> modificarTarifa());

        Button botoDesactivar = new Button("Desactivar tarifa");
        botoDesactivar.setOnAction(e -> desactivarTarifa());

        Button botoNetejar = new Button("Netejar formulari");
        botoNetejar.setOnAction(e -> netejar());

        Button botoTornar = new Button("Tornar al menú");
        botoTornar.setOnAction(e -> tornar.run());

        VBox formulari = new VBox(
                8,
                new Label("Nom"),
                campNom,
                new Label("Preu (€)"),
                campPreu,
                new Label("Durada en minuts"),
                campDurada,
                new Label("Data d'inici (AAAA-MM-DD)"),
                campDataInici,
                new Label("Data de fi (AAAA-MM-DD)"),
                campDataFi,
                new HBox(8, botoCrear, botoModificar),
                new HBox(8, botoDesactivar, botoNetejar)
        );

        formulari.setPadding(new Insets(12));
        formulari.setMaxWidth(480);

        missatge.setWrapText(true);

        VBox contingut = new VBox(
                12,
                new HBox(12, titol, botoActualitzar),
                new Label("Tarifes registrades"),
                taula,
                new Label("Dades de la tarifa seleccionada"),
                formulari,
                missatge,
                botoTornar
        );

        contingut.setPadding(new Insets(20));
        VBox.setVgrow(taula, Priority.ALWAYS);

        carregarTarifes();

        return contingut;
    }

    private void carregarTarifes() {
        try {
            JsonNode resposta = api.get(
                    "tarifes", Sessio.getToken());

            if (resposta == null || resposta.isNull()
                    || (resposta.isObject()
                    && resposta.has("success")
                    && !resposta.path("success").asBoolean(false))) {
                mostrarError(resposta);
                return;
            }

            JsonNode nodes;

            if (resposta.isArray()) {
                nodes = resposta;
            } else if (resposta.path("data").isArray()) {
                nodes = resposta.path("data");
            } else if (resposta.path("tarifes").isArray()) {
                nodes = resposta.path("tarifes");
            } else {
                missatge.setText(
                        "Format de resposta inesperat: " + resposta);
                return;
            }

            var tarifes =
                    FXCollections.<Tarifa>observableArrayList();

            for (JsonNode node : nodes) {
                int id = node.path("id").asInt(0);

                if (id <= 0) {
                    continue;
                }

                tarifes.add(new Tarifa(
                        id,
                        node.path("nom").asText(""),
                        node.path("preu").asText(""),
                        node.path("durada_minuts").asInt(0),
                        node.path("data_inici").asText(""),
                        node.path("data_fi").asText(""),
                        estaActiva(node.path("activa"))
                ));
            }

            taula.setItems(tarifes);
            missatge.setText(
                    "Tarifes carregades: " + tarifes.size());

        } catch (Exception ex) {
            missatge.setText(
                    "Error en consultar les tarifes: "
                            + ex.getMessage());
        }
    }

    private void crearTarifa() {
        if (!validarFormulari()) {
            return;
        }

        try {
            JsonNode resposta = api.post(
                    "tarifes",
                    Sessio.getToken(),
                    dadesFormulari()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message")
                        .asText("Tarifa creada correctament"));
                netejar();
                carregarTarifes();
            } else {
                mostrarError(resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en crear la tarifa: " + ex.getMessage());
        }
    }

    private void modificarTarifa() {
        Tarifa seleccionada = taula.getSelectionModel()
                .getSelectedItem();

        if (seleccionada == null) {
            missatge.setText("Selecciona una tarifa de la taula.");
            return;
        }

        if (!validarFormulari()) {
            return;
        }

        try {
            JsonNode resposta = api.put(
                    "tarifes&id=" + seleccionada.getId(),
                    Sessio.getToken(),
                    dadesFormulari()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message")
                        .asText("Tarifa modificada correctament"));
                carregarTarifes();
            } else {
                mostrarError(resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en modificar la tarifa: "
                            + ex.getMessage());
        }
    }

    private void desactivarTarifa() {
        Tarifa seleccionada = taula.getSelectionModel()
                .getSelectedItem();

        if (seleccionada == null) {
            missatge.setText("Selecciona una tarifa de la taula.");
            return;
        }

        if (!seleccionada.isActiva()) {
            missatge.setText("Aquesta tarifa ja està desactivada.");
            return;
        }

        Alert confirmacio = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Vols desactivar la tarifa «"
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
                    "tarifes&id=" + seleccionada.getId(),
                    Sessio.getToken()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message")
                        .asText("Tarifa desactivada correctament"));
                netejar();
                carregarTarifes();
            } else {
                mostrarError(resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en desactivar la tarifa: "
                            + ex.getMessage());
        }
    }


private boolean validarFormulari() {
    if (campNom.getText() == null
            || campNom.getText().trim().isEmpty()) {
        missatge.setText("El nom de la tarifa és obligatori.");
        return false;
    }

    double preu;
    int durada;

    try {
        preu = Double.parseDouble(
                campPreu.getText().trim().replace(',', '.'));

        if (!Double.isFinite(preu) || preu <= 0) {
            missatge.setText("El preu ha de ser superior a zero.");
            return false;
        }
    } catch (NumberFormatException ex) {
        missatge.setText("El preu ha de tenir un format vàlid.");
        return false;
    }

    try {
        durada = Integer.parseInt(campDurada.getText().trim());

        if (durada <= 0) {
            missatge.setText(
                    "La durada ha de ser superior a zero.");
            return false;
        }
    } catch (NumberFormatException ex) {
        missatge.setText(
                "La durada ha de ser un nombre enter positiu.");
        return false;
    }

    String textInici = campDataInici.getText() == null
            ? "" : campDataInici.getText().trim();

    String textFi = campDataFi.getText() == null
            ? "" : campDataFi.getText().trim();

    LocalDate inici = null;
    LocalDate fi = null;

    try {
        if (!textInici.isEmpty()) {
            inici = LocalDate.parse(textInici);
        }

        if (!textFi.isEmpty()) {
            fi = LocalDate.parse(textFi);
        }
    } catch (java.time.format.DateTimeParseException ex) {
        missatge.setText(
                "Les dates, si s'introdueixen, han de tenir "
                + "el format AAAA-MM-DD.");
        return false;
    }

    if (inici != null && fi != null && fi.isBefore(inici)) {
        missatge.setText(
                "La data de fi no pot ser anterior a la d'inici.");
        return false;
    }

    return true;
}

private ObjectNode dadesFormulari() {
    ObjectNode dades = mapper.createObjectNode();

    dades.put("nom", campNom.getText().trim());
    dades.put("preu", Double.parseDouble(
            campPreu.getText().trim().replace(',', '.')));
    dades.put("durada_minuts", Integer.parseInt(
            campDurada.getText().trim()));

    String inici = campDataInici.getText() == null
            ? "" : campDataInici.getText().trim();

    String fi = campDataFi.getText() == null
            ? "" : campDataFi.getText().trim();

    if (inici.isEmpty()) {
        dades.putNull("data_inici");
    } else {
        dades.put("data_inici", inici);
    }

    if (fi.isEmpty()) {
        dades.putNull("data_fi");
    } else {
        dades.put("data_fi", fi);
    }

    return dades;
}


    private boolean estaActiva(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return false;
        }

        if (node.isBoolean()) {
            return node.asBoolean();
        }

        if (node.isNumber()) {
            return node.asInt() == 1;
        }

        String valor = node.asText("").trim();

        return valor.equals("1")
                || valor.equalsIgnoreCase("true")
                || valor.equalsIgnoreCase("si")
                || valor.equalsIgnoreCase("sí");
    }

    private void netejar() {
        taula.getSelectionModel().clearSelection();
        campNom.clear();
        campPreu.clear();
        campDurada.clear();
        campDataInici.clear();
        campDataFi.clear();
    }

    private void mostrarError(JsonNode resposta) {
        if (resposta == null || resposta.isNull()) {
            missatge.setText("No s'ha rebut cap resposta de l'API.");
            return;
        }

        missatge.setText(resposta.path("message")
                .asText("L'operació no s'ha pogut completar."));
    }

    public static class Tarifa {
        private final int id;
        private final String nom;
        private final String preu;
        private final int duradaMinuts;
        private final String dataInici;
        private final String dataFi;
        private final boolean activa;

        public Tarifa(int id, String nom, String preu,
                      int duradaMinuts, String dataInici,
                      String dataFi, boolean activa) {
            this.id = id;
            this.nom = nom;
            this.preu = preu;
            this.duradaMinuts = duradaMinuts;
            this.dataInici = dataInici;
            this.dataFi = dataFi;
            this.activa = activa;
        }

        public int getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public String getPreu() {
            return preu;
        }

        public int getDuradaMinuts() {
            return duradaMinuts;
        }

        public String getDataInici() {
            return dataInici;
        }

        public String getDataFi() {
            return dataFi;
        }

        public boolean isActiva() {
            return activa;
        }

        public String getEstatText() {
            return activa ? "Activa" : "Inactiva";
        }
    }
}
