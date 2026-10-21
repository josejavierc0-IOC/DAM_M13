package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ReservesView {

    private final ApiClient api = new ApiClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final TableView<Reserva> taula = new TableView<>();
    private final Label missatge = new Label();

    private final ComboBox<Opcio> campSoci = new ComboBox<>();
    private final ComboBox<Opcio> campPista = new ComboBox<>();
    private final ComboBox<Opcio> campTarifa = new ComboBox<>();

    private final DatePicker campData = new DatePicker();
    private final TextField campHoraInici = new TextField();
    private final TextField campHoraFi = new TextField();

    private final DatePicker filtreData = new DatePicker();
    private final ComboBox<Opcio> filtrePista = new ComboBox<>();

    private final Button botoCrear = new Button("Crear reserva");
    private final Button botoModificar = new Button("Desar modificacions");
    private final Button botoCancelar = new Button("Cancel·lar reserva");
    private final Button botoPagada = new Button("Marcar com a pagada");

    public Parent crearVista(Runnable tornar) {

        Label titol = new Label("Gestió de reserves");
        titol.getStyleClass().add("titol");

        crearColumnes();

        // Filtres
        filtreData.setPromptText("Qualsevol data");
        filtrePista.setPromptText("Totes les pistes");
        filtrePista.setPrefWidth(190);

        Button botoFiltrar = new Button("Filtrar");
        botoFiltrar.setOnAction(e -> carregarReserves());

        Button botoTotes = new Button("Mostrar totes");
        botoTotes.setOnAction(e -> {
            filtreData.setValue(null);
            filtrePista.getSelectionModel().clearSelection();
            carregarReserves();
        });

        HBox filtres = new HBox(
                8,
                new Label("Data:"),
                filtreData,
                new Label("Pista:"),
                filtrePista,
                botoFiltrar,
                botoTotes
        );
        filtres.setPadding(new Insets(8, 0, 8, 0));

        // Formulari
        campSoci.setPromptText("Selecciona un soci");
        campPista.setPromptText("Selecciona una pista");
        campTarifa.setPromptText("Selecciona una tarifa");

        campSoci.setMaxWidth(Double.MAX_VALUE);
        campPista.setMaxWidth(Double.MAX_VALUE);
        campTarifa.setMaxWidth(Double.MAX_VALUE);
        campData.setMaxWidth(Double.MAX_VALUE);

        campHoraInici.setPromptText("HH:mm");
        campHoraFi.setPromptText("HH:mm");

        taula.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, reserva) -> {
                    if (reserva != null) {
                        omplirFormulari(reserva);
                    }
                    actualitzarBotons();
                }
        );

        Button botoActualitzar = new Button("Actualitzar");
        botoActualitzar.setOnAction(e -> {
            carregarOpcions();
            carregarReserves();
        });

        botoCrear.setOnAction(e -> crearReserva());
        botoModificar.setOnAction(e -> modificarReserva());
        botoCancelar.setOnAction(e -> cancelLarReserva());
        botoPagada.setOnAction(e -> marcarPagada());

        Button botoNetejar = new Button("Netejar formulari");
        botoNetejar.setOnAction(e -> {
            netejar();
            missatge.setText("");
        });

        Button botoTornar = new Button("Tornar al menú");
        botoTornar.setOnAction(e -> tornar.run());

        VBox formulari = new VBox(
                7,
                new Label("Soci"),
                campSoci,
                new Label("Pista"),
                campPista,
                new Label("Tarifa"),
                campTarifa,
                new Label("Data de la reserva"),
                campData,
                new Label("Hora d'inici"),
                campHoraInici,
                new Label("Hora de fi"),
                campHoraFi,
                new HBox(8, botoCrear, botoModificar),
                new HBox(8, botoCancelar, botoPagada),
                botoNetejar
        );

        formulari.setPadding(new Insets(12));
        formulari.setMaxWidth(380);

        missatge.setWrapText(true);

        VBox contingut = new VBox(
                12,
                new HBox(12, titol, botoActualitzar),
                filtres,
                new Label("Reserves registrades"),
                taula,
                new Label("Dades de la reserva"),
                formulari,
                missatge,
                botoTornar
        );

        contingut.setPadding(new Insets(20));
        VBox.setVgrow(taula, Priority.ALWAYS);

        carregarOpcions();
        carregarReserves();
        actualitzarBotons();

        return contingut;
    }

    private void crearColumnes() {
        TableColumn<Reserva, Integer> colId =
                new TableColumn<>("ID");
        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        TableColumn<Reserva, String> colData =
                new TableColumn<>("Data");
        colData.setCellValueFactory(
                new PropertyValueFactory<>("dataReserva")
        );

        TableColumn<Reserva, String> colInici =
                new TableColumn<>("Inici");
        colInici.setCellValueFactory(
                new PropertyValueFactory<>("horaInici")
        );

        TableColumn<Reserva, String> colFi =
                new TableColumn<>("Fi");
        colFi.setCellValueFactory(
                new PropertyValueFactory<>("horaFi")
        );

        TableColumn<Reserva, String> colPista =
                new TableColumn<>("Pista");
        colPista.setCellValueFactory(
                new PropertyValueFactory<>("pistaNom")
        );

        TableColumn<Reserva, String> colSoci =
                new TableColumn<>("Soci");
        colSoci.setCellValueFactory(
                new PropertyValueFactory<>("soci")
        );

        TableColumn<Reserva, String> colEstat =
                new TableColumn<>("Estat");
        colEstat.setCellValueFactory(
                new PropertyValueFactory<>("estat")
        );

        TableColumn<Reserva, String> colPagament =
                new TableColumn<>("Pagament");
        colPagament.setCellValueFactory(
                new PropertyValueFactory<>("pagament")
        );

        taula.getColumns().setAll(
                colId,
                colData,
                colInici,
                colFi,
                colPista,
                colSoci,
                colEstat,
                colPagament
        );

        taula.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );
    }

    /*
     * Carrega socis, pistes i tarifes des de l'API.
     */
    private void carregarOpcions() {
        carregarSocis();
        carregarPistes();
        carregarTarifes();
    }

    private void carregarSocis() {
        try {
            JsonNode resposta = api.get(
                    "socis",
                    Sessio.getToken()
            );

            if (!resposta.path("success").asBoolean(false)) {
                mostrarError("No s'han pogut carregar els socis.", resposta);
                return;
            }

            JsonNode nodes = extreureArray(
                    resposta,
                    "data",
                    "socis",
                    "usuaris"
            );

            List<Opcio> opcions = new ArrayList<>();

            for (JsonNode node : nodes) {
                int id = node.path("id").asInt(0);
                if (id <= 0) {
                    continue;
                }

                String numero = node.path("numero_soci").asText("");
                String nom = node.path("nom").asText("");
                String cognoms = node.path("cognoms").asText("");

                String etiqueta = numero.isBlank()
                        ? (nom + " " + cognoms).trim()
                        : numero + " — " + (nom + " " + cognoms).trim();

                opcions.add(new Opcio(id, etiqueta));
            }

            campSoci.setItems(
                    FXCollections.observableArrayList(opcions)
            );

            missatge.setText(
                    opcions.isEmpty()
                            ? "No s'han trobat socis disponibles."
                            : "Socis carregats: " + opcions.size()
            );

        } catch (Exception ex) {
            missatge.setText(
                    "Error en carregar els socis: " + ex.getMessage()
            );
        }
    }

    private void carregarPistes() {
    try {
        JsonNode resposta = api.get("pistes", Sessio.getToken());

        if (resposta.has("success")
                && !resposta.path("success").asBoolean(false)) {
            mostrarError("No s'han pogut carregar les pistes.", resposta);
            return;
        }

        JsonNode nodes = extreureArray(resposta, "data", "pistes");
        List<Opcio> opcions = new ArrayList<>();

        for (JsonNode node : nodes) {
            int id = node.path("id").asInt(0);

            if (id <= 0 || !estaActiva(node)) {
                continue;
            }

            String nom = node.path("nom").asText("Pista " + id);
            opcions.add(new Opcio(id, nom));
        }

        campPista.setItems(
                FXCollections.observableArrayList(opcions)
        );

        filtrePista.setItems(
                FXCollections.observableArrayList(opcions)
        );

        if (opcions.isEmpty()) {
            missatge.setText(
                    "L'API ha respost, però no s'han trobat pistes actives. "
                    + "Resposta: " + resposta
            );
        } else {
            missatge.setText(
                    "Pistes carregades: " + opcions.size()
            );
        }

    } catch (Exception ex) {
        missatge.setText(
                "Error en carregar les pistes: " + ex.getMessage()
        );
    }
}

    private void carregarTarifes() {
    try {
        JsonNode resposta = api.get("tarifes", Sessio.getToken());

        if (resposta.has("success")
                && !resposta.path("success").asBoolean(false)) {
            mostrarError("No s'han pogut carregar les tarifes.", resposta);
            return;
        }

        JsonNode nodes = extreureArray(resposta, "data", "tarifes");
        List<Opcio> opcions = new ArrayList<>();

        for (JsonNode node : nodes) {
            int id = node.path("id").asInt(0);

            if (id <= 0 || !estaActiva(node)) {
                continue;
            }

            String nom = node.path("nom").asText("Tarifa " + id);
            String preu = node.path("preu").asText("");
            String durada = node.path("durada_minuts").asText("");

            String etiqueta = nom;

            if (!preu.isBlank()) {
                etiqueta += " — " + preu + " €";
            }

            if (!durada.isBlank()) {
                etiqueta += " (" + durada + " min)";
            }

            opcions.add(new Opcio(id, etiqueta));
        }

        campTarifa.setItems(
                FXCollections.observableArrayList(opcions)
        );

        if (opcions.isEmpty()) {
            missatge.setText(
                    "L'API ha respost, però no s'han trobat tarifes actives. "
                    + "Resposta: " + resposta
            );
        } else {
            missatge.setText(
                    "Tarifes carregades: " + opcions.size()
            );
        }

    } catch (Exception ex) {
        missatge.setText(
                "Error en carregar les tarifes: " + ex.getMessage()
        );
    }

}

private boolean estaActiva(JsonNode node) {
    JsonNode activa = node.path("activa");

    // Si l'API no retorna el camp, no descartem l'element.
    if (activa.isMissingNode() || activa.isNull()) {
        return true;
    }

    if (activa.isBoolean()) {
        return activa.asBoolean();
    }

    if (activa.isNumber()) {
        return activa.asInt() == 1;
    }

    String valor = activa.asText("").trim();

    return valor.equals("1")
            || valor.equalsIgnoreCase("true")
            || valor.equalsIgnoreCase("si")
            || valor.equalsIgnoreCase("sí");
}

    /*
     * Admet respostes JSON amb arrays sota diferents claus.
     */
    private JsonNode extreureArray(
            JsonNode resposta,
            String... claus
    ) {
        if (resposta == null || resposta.isMissingNode()) {
            return mapper.createArrayNode();
        }

        if (resposta.isArray()) {
            return resposta;
        }

        for (String clau : claus) {
            JsonNode node = resposta.path(clau);

            if (node.isArray()) {
                return node;
            }

            if (node.isObject()) {
                for (String subclau :
                        new String[]{"data", "pistes", "tarifes", "socis"}) {
                    JsonNode intern = node.path(subclau);
                    if (intern.isArray()) {
                        return intern;
                    }
                }
            }
        }

        return mapper.createArrayNode();
    }

    private void carregarReserves() {
        try {
            StringBuilder recurs = new StringBuilder("reserves");

            if (filtreData.getValue() != null) {
                recurs.append("&data=")
                        .append(filtreData.getValue());
            }

            Opcio pistaFiltre =
                    filtrePista.getSelectionModel().getSelectedItem();

            if (pistaFiltre != null) {
                recurs.append("&pista_id=")
                        .append(pistaFiltre.getId());
            }

            JsonNode resposta = api.get(
                    recurs.toString(),
                    Sessio.getToken()
            );

            System.out.println("RESPOSTA RESERVES: " + resposta);

            if (resposta == null || resposta.isNull()
                    || (resposta.isObject()
                    && resposta.has("success")
                    && !resposta.path("success").asBoolean(false))) {
                mostrarError("No s'han pogut consultar les reserves.", resposta);
                return;
            }

            JsonNode nodes = extreureArray(resposta, "data", "reserves");

            if (nodes == null || !nodes.isArray()) {
                missatge.setText("Format de resposta de reserves inesperat: " + resposta);
                return;
            }

            var reserves = FXCollections.<Reserva>observableArrayList();

            for (JsonNode node : nodes) {
                String nom = node.path("usuari_nom").asText("");
                String cognoms = node.path("usuari_cognoms").asText("");

                if (nom.isBlank() && cognoms.isBlank()) {
                    nom = node.path("nom").asText("");
                    cognoms = node.path("cognoms").asText("");
                }

                String soci = (nom + " " + cognoms).trim();

                if (soci.isBlank()) {
                    soci = "Soci " + node.path("soci_id").asInt();
                }

                String pistaNom = node.path("pista_nom").asText("");

                if (pistaNom.isBlank()) {
                    pistaNom = "Pista " + node.path("pista_id").asInt();
                }

                reserves.add(new Reserva(
                        node.path("id").asInt(),
                        node.path("soci_id").asInt(),
                        node.path("pista_id").asInt(),
                        node.path("tarifa_id").asInt(),
                        node.path("data_reserva").asText(""),
                        node.path("hora_inici").asText(""),
                        node.path("hora_fi").asText(""),
                        pistaNom,
                        soci,
                        node.path("estat").asText(""),
                        node.path("pagament_estat").asText("")
                ));
            }

            taula.setItems(reserves);
            actualitzarBotons();
            System.out.println("Reserves a la taula: " + taula.getItems().size());
            System.out.println("Columnes de la taula: " + taula.getColumns().size());

            missatge.setText(
                    "Reserves carregades: " + reserves.size()
            );

        } catch (Exception ex) {
            missatge.setText(
                    "Error en consultar les reserves: " + ex.getMessage()
            );
        }
    }

    private void omplirFormulari(Reserva reserva) {
        try {
            if (!reserva.getDataReserva().isBlank()) {
                campData.setValue(
                        LocalDate.parse(reserva.getDataReserva())
                );
            }

            seleccionarId(campSoci, reserva.getSociId());
            seleccionarId(campPista, reserva.getPistaId());
            seleccionarId(campTarifa, reserva.getTarifaId());

            campHoraInici.setText(
                    formatHora(reserva.getHoraInici())
            );

            campHoraFi.setText(
                    formatHora(reserva.getHoraFi())
            );

        } catch (DateTimeParseException ex) {
            missatge.setText(
                    "La data de la reserva seleccionada no és vàlida."
            );
        }
    }

    private String formatHora(String hora) {
        if (hora == null) {
            return "";
        }

        return hora.length() >= 5
                ? hora.substring(0, 5)
                : hora;
    }

    private void seleccionarId(
            ComboBox<Opcio> combo,
            int id
    ) {
        for (Opcio opcio : combo.getItems()) {
            if (opcio.getId() == id) {
                combo.getSelectionModel().select(opcio);
                return;
            }
        }

        // Permet mostrar una reserva antiga encara que l'opció
        // ja no estigui entre les opcions actives.
        Opcio antiga = new Opcio(id, "ID " + id + " (no disponible)");
        combo.getItems().add(antiga);
        combo.getSelectionModel().select(antiga);
    }

    private void crearReserva() {
        if (!validarFormulari(false)) {
            return;
        }

        try {
            ObjectNode dades = dadesFormulari();

            dades.put(
                    "soci_id",
                    campSoci.getSelectionModel()
                            .getSelectedItem().getId()
            );

            JsonNode resposta = api.post(
                    "reserves",
                    Sessio.getToken(),
                    dades
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(
                        resposta.path("message")
                                .asText("Reserva creada correctament.")
                );

                netejar();
                carregarReserves();

            } else {
                mostrarError("No s'ha pogut crear la reserva.", resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en crear la reserva: " + ex.getMessage()
            );
        }
    }

    private void modificarReserva() {
        Reserva seleccionada =
                taula.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            missatge.setText(
                    "Selecciona una reserva de la taula per modificar-la."
            );
            return;
        }

        if (!validarFormulari(true)) {
            return;
        }

        try {
            JsonNode resposta = api.patch(
                    "reserva&id=" + seleccionada.getId()
                            + "&accio=modificar",
                    Sessio.getToken(),
                    dadesFormulari()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(
                        resposta.path("message")
                                .asText("Reserva modificada correctament.")
                );

                carregarReserves();

            } else {
                mostrarError("No s'ha pogut modificar la reserva.", resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en modificar la reserva: " + ex.getMessage()
            );
        }
    }

    private void cancelLarReserva() {
        Reserva seleccionada =
                taula.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            missatge.setText("Selecciona una reserva per cancel·lar-la.");
            return;
        }

        if (!"CONFIRMADA".equalsIgnoreCase(seleccionada.getEstat())) {
            missatge.setText(
                    "Només es poden cancel·lar reserves confirmades."
            );
            return;
        }

        Alert confirmacio = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Vols cancel·lar la reserva "
                        + seleccionada.getId() + "?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacio.setHeaderText("Confirmar cancel·lació");

        if (confirmacio.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            JsonNode resposta = api.patch(
                    "reserva&id=" + seleccionada.getId(),
                    Sessio.getToken()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(
                        resposta.path("message")
                                .asText("Reserva cancel·lada correctament.")
                );

                netejar();
                carregarReserves();

            } else {
                mostrarError("No s'ha pogut cancel·lar la reserva.", resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en cancel·lar la reserva: " + ex.getMessage()
            );
        }
    }

    private void marcarPagada() {
        Reserva seleccionada =
                taula.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            missatge.setText(
                    "Selecciona una reserva per registrar-ne el pagament."
            );
            return;
        }

        if (!"CONFIRMADA".equalsIgnoreCase(seleccionada.getEstat())) {
            missatge.setText(
                    "Només es pot registrar el pagament d'una reserva confirmada."
            );
            return;
        }

        if ("PAGAT".equalsIgnoreCase(seleccionada.getPagament())) {
            missatge.setText("Aquesta reserva ja consta com a pagada.");
            return;
        }

        Alert confirmacio = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Vols marcar com a pagada la reserva "
                        + seleccionada.getId() + "?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacio.setHeaderText("Confirmar pagament");

        if (confirmacio.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            JsonNode resposta = api.patch(
                    "reserva&id=" + seleccionada.getId()
                            + "&accio=pagament",
                    Sessio.getToken()
            );

            if (resposta.path("success").asBoolean(false)) {
                missatge.setText(
                        resposta.path("message")
                                .asText("Pagament registrat correctament.")
                );

                carregarReserves();

            } else {
                mostrarError("No s'ha pogut registrar el pagament.", resposta);
            }

        } catch (Exception ex) {
            missatge.setText(
                    "Error en registrar el pagament: " + ex.getMessage()
            );
        }
    }

    private boolean validarFormulari(boolean modificacio) {
        if (!modificacio
                && campSoci.getSelectionModel().getSelectedItem() == null) {
            missatge.setText("Selecciona el soci de la reserva.");
            return false;
        }

        if (campPista.getSelectionModel().getSelectedItem() == null) {
            missatge.setText("Selecciona una pista.");
            return false;
        }

        if (campTarifa.getSelectionModel().getSelectedItem() == null) {
            missatge.setText("Selecciona una tarifa.");
            return false;
        }

        if (campData.getValue() == null) {
            missatge.setText("Selecciona la data de la reserva.");
            return false;
        }

        String iniciText = campHoraInici.getText().trim();
        String fiText = campHoraFi.getText().trim();

        if (!iniciText.matches("\\d{2}:\\d{2}")
                || !fiText.matches("\\d{2}:\\d{2}")) {
            missatge.setText(
                    "Les hores han de tenir el format HH:mm, per exemple 10:30."
            );
            return false;
        }

        try {
            LocalTime inici = LocalTime.parse(iniciText);
            LocalTime fi = LocalTime.parse(fiText);

            if (!fi.isAfter(inici)) {
                missatge.setText(
                        "L'hora de fi ha de ser posterior a l'hora d'inici."
                );
                return false;
            }

        } catch (DateTimeParseException ex) {
            missatge.setText("Les hores introduïdes no són vàlides.");
            return false;
        }

        return true;
    }

    private ObjectNode dadesFormulari() {
        ObjectNode dades = mapper.createObjectNode();

        dades.put(
                "pista_id",
                campPista.getSelectionModel().getSelectedItem().getId()
        );

        dades.put(
                "tarifa_id",
                campTarifa.getSelectionModel().getSelectedItem().getId()
        );

        dades.put("data_reserva", campData.getValue().toString());
        dades.put("hora_inici", campHoraInici.getText().trim());
        dades.put("hora_fi", campHoraFi.getText().trim());

        return dades;
    }

    private void netejar() {
        taula.getSelectionModel().clearSelection();

        campSoci.getSelectionModel().clearSelection();
        campPista.getSelectionModel().clearSelection();
        campTarifa.getSelectionModel().clearSelection();

        campData.setValue(null);
        campHoraInici.clear();
        campHoraFi.clear();

        actualitzarBotons();
    }

    private void actualitzarBotons() {
        Reserva seleccionada =
                taula.getSelectionModel().getSelectedItem();

        boolean hiHaSeleccio = seleccionada != null;

        botoModificar.setDisable(!hiHaSeleccio);
        botoCancelar.setDisable(
                !hiHaSeleccio
                        || !"CONFIRMADA".equalsIgnoreCase(
                                seleccionada == null ? "" : seleccionada.getEstat()
                        )
        );

        botoPagada.setDisable(
                !hiHaSeleccio
                        || !"CONFIRMADA".equalsIgnoreCase(
                                seleccionada == null ? "" : seleccionada.getEstat()
                        )
                        || "PAGAT".equalsIgnoreCase(
                                seleccionada == null ? "" : seleccionada.getPagament()
                        )
        );
    }

    private void mostrarError(String prefix, JsonNode resposta) {
        String detall = resposta.path("message").asText("");

        if (detall.isBlank()) {
            detall = resposta.path("error").asText("");
        }

        if (detall.isBlank()) {
            detall = resposta.toString();
        }

        missatge.setText(prefix + " " + detall);
    }

    /*
     * Element que es mostra als desplegables.
     */
    private static class Opcio {
        private final int id;
        private final String etiqueta;

        Opcio(int id, String etiqueta) {
            this.id = id;
            this.etiqueta = etiqueta;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    /*
     * Model de reserva utilitzat per la taula.
     */
    public static class Reserva {
        private final int id;
        private final int sociId;
        private final int pistaId;
        private final int tarifaId;
        private final String dataReserva;
        private final String horaInici;
        private final String horaFi;
        private final String pistaNom;
        private final String soci;
        private final String estat;
        private final String pagament;

        public Reserva(
                int id,
                int sociId,
                int pistaId,
                int tarifaId,
                String dataReserva,
                String horaInici,
                String horaFi,
                String pistaNom,
                String soci,
                String estat,
                String pagament
        ) {
            this.id = id;
            this.sociId = sociId;
            this.pistaId = pistaId;
            this.tarifaId = tarifaId;
            this.dataReserva = dataReserva;
            this.horaInici = horaInici;
            this.horaFi = horaFi;
            this.pistaNom = pistaNom;
            this.soci = soci;
            this.estat = estat;
            this.pagament = pagament;
        }

        public int getId() {
            return id;
        }

        public int getSociId() {
            return sociId;
        }

        public int getPistaId() {
            return pistaId;
        }

        public int getTarifaId() {
            return tarifaId;
        }

        public String getDataReserva() {
            return dataReserva;
        }

        public String getHoraInici() {
            return horaInici;
        }

        public String getHoraFi() {
            return horaFi;
        }

        public String getPistaNom() {
            return pistaNom;
        }

        public String getSoci() {
            return soci;
        }

        public String getEstat() {
            return estat;
        }

        public String getPagament() {
            return pagament;
        }
    }
}