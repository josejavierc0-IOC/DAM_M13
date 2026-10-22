

package dam.clubpadel.escriptori;



import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.collections.FXCollections;

import javafx.geometry.Insets;

import javafx.geometry.Pos;

import javafx.scene.Parent;

import javafx.scene.control.*;

import javafx.scene.layout.*;

import javafx.scene.control.cell.PropertyValueFactory;



import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.LocalTime;

import java.time.format.DateTimeFormatter;



public class IncidenciesView {



    private final ApiClient api = new ApiClient();

    private final ObjectMapper mapper = new ObjectMapper();



    private TableView<Incidencia> taula;

    private ComboBox<PistaOpcio> comboPista;

    private TextArea campDescripcio;



    private DatePicker campDataInici;

    private TextField campHoraInici;

    private DatePicker campDataFi;

    private TextField campHoraFi;



    private Label missatge;

    private Button botoCrear;

    private Button botoModificar;

    private Button botoResoldre;



    private Long incidenciaSeleccionada;



    private static final DateTimeFormatter FORMAT_HORA =

            DateTimeFormatter.ofPattern("HH:mm");



    private static final DateTimeFormatter FORMAT_DATA_HORA =

            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");



    public Parent crearVista(Runnable tornar) {



        BorderPane arrel = new BorderPane();

        arrel.setPadding(new Insets(15));



        // Capçalera.

        Label titol = new Label("Gestió d'incidències");

        titol.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");



        Button botoTornar = new Button("Tornar al menú");

        botoTornar.setOnAction(e -> tornar.run());



        Region espaiCapcalera = new Region();

        HBox.setHgrow(espaiCapcalera, Priority.ALWAYS);



        HBox capcalera = new HBox(10, titol, espaiCapcalera, botoTornar);

        capcalera.setAlignment(Pos.CENTER_LEFT);

        capcalera.setPadding(new Insets(0, 0, 15, 0));



        arrel.setTop(capcalera);



        // Taula d'incidències.

        taula = new TableView<>();

        taula.setColumnResizePolicy(

                TableView.CONSTRAINED_RESIZE_POLICY

        );

        taula.setPlaceholder(

                new Label("No hi ha incidències per mostrar")

        );



        TableColumn<Incidencia, Long> colId =

                new TableColumn<>("ID");

        colId.setCellValueFactory(

                new PropertyValueFactory<>("id")

        );



        TableColumn<Incidencia, String> colPista =

                new TableColumn<>("Pista");

        colPista.setCellValueFactory(

                new PropertyValueFactory<>("pista")

        );



        TableColumn<Incidencia, String> colDescripcio =

                new TableColumn<>("Descripció");

        colDescripcio.setCellValueFactory(

                new PropertyValueFactory<>("descripcio")

        );



        TableColumn<Incidencia, String> colInici =

                new TableColumn<>("Data inici");

        colInici.setCellValueFactory(

                new PropertyValueFactory<>("dataInici")

        );



        TableColumn<Incidencia, String> colFi =

                new TableColumn<>("Data fi");

        colFi.setCellValueFactory(

                new PropertyValueFactory<>("dataFi")

        );



        TableColumn<Incidencia, String> colEstat =

                new TableColumn<>("Estat");

        colEstat.setCellValueFactory(

                new PropertyValueFactory<>("estat")

        );



        taula.getColumns().addAll(

                colId,

                colPista,

                colDescripcio,

                colInici,

                colFi,

                colEstat

        );



        taula.getSelectionModel()

                .selectedItemProperty()

                .addListener((observable, anterior, actual) -> {

                    if (actual != null) {

                        carregarAlFormulari(actual);

                    }

                });



        // Formulari.

        Label titolFormulari = new Label("Dades de la incidència");

        titolFormulari.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");



        comboPista = new ComboBox<>();

        comboPista.setPromptText("Selecciona una pista");

        comboPista.setMaxWidth(Double.MAX_VALUE);



        campDescripcio = new TextArea();

        campDescripcio.setPromptText("Descripció de la incidència");

        campDescripcio.setPrefRowCount(3);

        campDescripcio.setWrapText(true);



        campDataInici = new DatePicker(LocalDate.now());

        campHoraInici = new TextField(

                LocalTime.now().format(FORMAT_HORA)

        );

        campHoraInici.setPromptText("HH:mm");



        campDataFi = new DatePicker();

        campHoraFi = new TextField();

        campHoraFi.setPromptText("HH:mm (opcional)");



        GridPane formulari = new GridPane();

        formulari.setHgap(10);

        formulari.setVgap(8);



        formulari.add(new Label("Pista:"), 0, 0);

        formulari.add(comboPista, 1, 0, 3, 1);



        formulari.add(new Label("Descripció:"), 0, 1);

        formulari.add(campDescripcio, 1, 1, 3, 1);



        formulari.add(new Label("Data d'inici:"), 0, 2);

        formulari.add(campDataInici, 1, 2);

        formulari.add(new Label("Hora:"), 2, 2);

        formulari.add(campHoraInici, 3, 2);



        formulari.add(new Label("Data final:"), 0, 3);

        formulari.add(campDataFi, 1, 3);

        formulari.add(new Label("Hora final:"), 2, 3);

        formulari.add(campHoraFi, 3, 3);



        ColumnConstraints columnaEtiqueta = new ColumnConstraints();

        columnaEtiqueta.setMinWidth(90);



        ColumnConstraints columnaControl = new ColumnConstraints();

        columnaControl.setHgrow(Priority.ALWAYS);



        formulari.getColumnConstraints().addAll(

                columnaEtiqueta,

                columnaControl,

                columnaEtiqueta,

                columnaControl

        );



        campDataFi.valueProperty().addListener(

                (observable, anterior, actual) -> {

                    if (actual == null) {

                        campHoraFi.clear();

                    }

                }

        );



        // Botons d'acció.

        botoCrear = new Button("Crear incidència");

        botoModificar = new Button("Modificar seleccionada");

        botoResoldre = new Button("Marcar com a resolta");

        Button botoNetejar = new Button("Netejar formulari");

        Button botoActualitzar = new Button("Actualitzar llista");



        botoCrear.setOnAction(e -> crearIncidencia());

        botoModificar.setOnAction(e -> modificarIncidencia());

        botoResoldre.setOnAction(e -> resoldreIncidencia());

        botoNetejar.setOnAction(e -> netejarFormulari());

        botoActualitzar.setOnAction(e -> carregarIncidencies());



        HBox botons = new HBox(

                8,

                botoCrear,

                botoModificar,

                botoResoldre,

                botoNetejar,

                botoActualitzar

        );

        botons.setAlignment(Pos.CENTER_LEFT);



        missatge = new Label();

        missatge.setWrapText(true);



        VBox zonaInferior = new VBox(

                10,

                new Separator(),

                titolFormulari,

                formulari,

                botons,

                missatge

        );

        zonaInferior.setPadding(new Insets(15, 0, 0, 0));



        arrel.setCenter(taula);

        arrel.setBottom(zonaInferior);



        carregarPistes();

        carregarIncidencies();



        return arrel;

    }






    private void carregarPistes() {

        try {

            JsonNode resposta = api.get(

                    "pistes",

                    Sessio.getToken()

            );



            if (!resposta.path("success").asBoolean(false)) {

                mostrarError(resposta);

                return;

            }



            comboPista.setItems(

                    FXCollections.observableArrayList()

            );



            JsonNode nodes = resposta.path("pistes");



            for (JsonNode node : nodes) {

                if (node.path("activa").asInt(0) == 1) {

                    comboPista.getItems().add(

                            new PistaOpcio(

                                    node.path("id").asInt(),

                                    node.path("nom").asText("")

                            )

                    );

                }

            }



        } catch (Exception ex) {

            missatge.setText(

                    "Error en carregar les pistes: " + ex.getMessage()

            );

        }

    }






    private void carregarIncidencies() {

        try {

            JsonNode resposta = api.get(

                    "incidencies",

                    Sessio.getToken()

            );



            if (!resposta.path("success").asBoolean(false)) {

                mostrarError(resposta);

                return;

            }



            var incidencies =

                    FXCollections.<Incidencia>observableArrayList();



            for (JsonNode node : resposta.path("incidencies")) {

                incidencies.add(

                        new Incidencia(

                                node.path("id").asLong(),

                                node.path("pista_id").asInt(),

                                node.path("pista").asText(""),

                                node.path("descripcio").asText(""),

                                node.path("data_inici").asText(""),

                                node.path("data_fi").isNull()

                                        ? ""

                                        : node.path("data_fi").asText(""),

                                node.path("estat").asText("OBERTA")

                        )

                );

            }



            taula.setItems(incidencies);

            missatge.setText(

                    "Incidències carregades: " + incidencies.size()

            );



        } catch (Exception ex) {

            missatge.setText(

                    "Error en consultar les incidències: "

                            + ex.getMessage()

            );

        }

    }






    private void crearIncidencia() {

        PistaOpcio pista = comboPista.getValue();



        if (pista == null) {

            missatge.setText("Selecciona una pista.");

            return;

        }



        if (campDescripcio.getText().trim().isEmpty()) {

            missatge.setText("La descripció és obligatòria.");

            return;

        }



        try {

            ObjectNode dades = construirDades(true);



            dades.put("pista_id", pista.id);



            JsonNode resposta = api.post(

                    "incidencies",

                    Sessio.getToken(),

                    dades

            );



            if (resposta.path("success").asBoolean(false)) {

                missatge.setText(

                        resposta.path("message").asText(

                                "Incidència creada correctament"

                        )

                );



                netejarFormulari();

                carregarIncidencies();



            } else {

                mostrarError(resposta);

            }



        } catch (Exception ex) {

            missatge.setText(

                    "Error en crear la incidència: " + ex.getMessage()

            );

        }

    }






    private void modificarIncidencia() {

        Incidencia seleccionada =

                taula.getSelectionModel().getSelectedItem();



        if (seleccionada == null) {

            missatge.setText("Selecciona una incidència de la taula.");

            return;

        }



        if (!"OBERTA".equals(seleccionada.getEstat())) {

            missatge.setText(

                    "Només es poden modificar incidències obertes."

            );

            return;

        }



        try {

            ObjectNode dades = construirDades(false);



            JsonNode resposta = api.put(

                    "incidencies&id=" + seleccionada.getId(),

                    Sessio.getToken(),

                    dades

            );



            if (resposta.path("success").asBoolean(false)) {

                missatge.setText(

                        resposta.path("message").asText(

                                "Incidència modificada correctament"

                        )

                );



                carregarIncidencies();

                netejarFormulari();



            } else {

                mostrarError(resposta);

            }



        } catch (Exception ex) {

            missatge.setText(

                    "Error en modificar la incidència: "

                            + ex.getMessage()

            );

        }

    }






    private void resoldreIncidencia() {

        Incidencia seleccionada =

                taula.getSelectionModel().getSelectedItem();



        if (seleccionada == null) {

            missatge.setText("Selecciona una incidència de la taula.");

            return;

        }



        if (!"OBERTA".equals(seleccionada.getEstat())) {

            missatge.setText("Aquesta incidència ja està resolta.");

            return;

        }



        Alert confirmacio = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacio.setTitle("Resoldre incidència");

        confirmacio.setHeaderText("Vols marcar la incidència com a resolta?");

        confirmacio.setContentText(

                "Incidència #" + seleccionada.getId()

        );



        var resultat = confirmacio.showAndWait();



        if (resultat.isEmpty()

                || resultat.get() != ButtonType.OK) {

            return;

        }



        try {

            JsonNode resposta = api.patch(

                    "incidencies&id=" + seleccionada.getId(),

                    Sessio.getToken(),

                    mapper.createObjectNode()

            );



            if (resposta.path("success").asBoolean(false)) {

                missatge.setText(

                        resposta.path("message").asText(

                                "Incidència resolta correctament"

                        )

                );



                carregarIncidencies();

                netejarFormulari();



            } else {

                mostrarError(resposta);

            }



        } catch (Exception ex) {

            missatge.setText(

                    "Error en resoldre la incidència: "

                            + ex.getMessage()

            );

        }

    }






    private ObjectNode construirDades(boolean validarPista) {

        if (campDataInici.getValue() == null) {

            throw new IllegalArgumentException(

                    "La data d'inici és obligatòria."

            );

        }



        LocalDateTime inici = combinarDataHora(

                campDataInici.getValue(),

                campHoraInici.getText(),

                "La data i l'hora d'inici són obligatòries."

        );



        LocalDateTime fi = null;



        boolean teDataFi = campDataFi.getValue() != null;

        boolean teHoraFi = campHoraFi.getText() != null

                && !campHoraFi.getText().trim().isEmpty();



        if (teDataFi != teHoraFi) {

            throw new IllegalArgumentException(

                    "Per indicar la data final, cal indicar també l'hora final."

            );

        }



        if (teDataFi) {

            fi = combinarDataHora(

                    campDataFi.getValue(),

                    campHoraFi.getText(),

                    "La data i l'hora finals no són vàlides."

            );



            if (!fi.isAfter(inici)) {

                throw new IllegalArgumentException(

                        "La data final ha de ser posterior a la inicial."

                );

            }

        }



        ObjectNode dades = mapper.createObjectNode();



        dades.put("descripcio", campDescripcio.getText().trim());

        dades.put(

                "data_inici",

                inici.format(

                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

                )

        );



        if (fi == null) {

            dades.putNull("data_fi");

        } else {

            dades.put(

                    "data_fi",

                    fi.format(

                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

                    )

            );

        }



        return dades;

    }



    private LocalDateTime combinarDataHora(

            LocalDate data,

            String horaText,

            String missatgeError

    ) {

        if (data == null || horaText == null

                || horaText.trim().isEmpty()) {

            throw new IllegalArgumentException(missatgeError);

        }



        try {

            LocalTime hora = LocalTime.parse(

                    horaText.trim(),

                    FORMAT_HORA

            );



            return LocalDateTime.of(data, hora);



        } catch (Exception ex) {

            throw new IllegalArgumentException(

                    "Escriu l'hora en format HH:mm, per exemple 14:30."

            );

        }

    }






    private void carregarAlFormulari(Incidencia incidencia) {

        incidenciaSeleccionada = incidencia.getId();



        campDescripcio.setText(incidencia.getDescripcio());



        for (PistaOpcio pista : comboPista.getItems()) {

            if (pista.id == incidencia.getPistaId()) {

                comboPista.setValue(pista);

                break;

            }

        }



        try {

            LocalDateTime inici = parseDataHora(incidencia.getDataIniciOriginal());



            campDataInici.setValue(inici.toLocalDate());

            campHoraInici.setText(inici.toLocalTime().format(FORMAT_HORA));



            if (incidencia.getDataFiOriginal() != null

                    && !incidencia.getDataFiOriginal().isBlank()) {



                LocalDateTime fi = parseDataHora(incidencia.getDataFiOriginal());



                campDataFi.setValue(fi.toLocalDate());

                campHoraFi.setText(fi.toLocalTime().format(FORMAT_HORA));



            } else {

                campDataFi.setValue(null);

                campHoraFi.clear();

            }



        } catch (Exception ex) {

            missatge.setText(

                    "No s'han pogut interpretar les dates de la incidència."

            );

        }



        boolean oberta = "OBERTA".equals(incidencia.getEstat());



        botoModificar.setDisable(!oberta);

        botoResoldre.setDisable(!oberta);

        botoCrear.setDisable(false);



        missatge.setText(

                "Incidència seleccionada: #" + incidencia.getId()

                        + " (" + incidencia.getEstat() + ")"

        );

    }



    private LocalDateTime parseDataHora(String text) {

        try {

            return LocalDateTime.parse(

                    text,

                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            );

        } catch (Exception ex) {

            return LocalDateTime.parse(

                    text,

                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

            );

        }

    }






    private void netejarFormulari() {

        incidenciaSeleccionada = null;



        if (taula != null) {

            taula.getSelectionModel().clearSelection();

        }



        if (comboPista != null) {

            comboPista.getSelectionModel().clearSelection();

        }



        if (campDescripcio != null) {

            campDescripcio.clear();

        }



        if (campDataInici != null) {

            campDataInici.setValue(LocalDate.now());

        }



        if (campHoraInici != null) {

            campHoraInici.setText(

                    LocalTime.now().format(FORMAT_HORA)

            );

        }



        if (campDataFi != null) {

            campDataFi.setValue(null);

        }



        if (campHoraFi != null) {

            campHoraFi.clear();

        }



        if (botoModificar != null) {

            botoModificar.setDisable(false);

        }



        if (botoResoldre != null) {

            botoResoldre.setDisable(false);

        }



        if (botoCrear != null) {

            botoCrear.setDisable(false);

        }

    }



    private void mostrarError(JsonNode resposta) {

        missatge.setText(

                resposta.path("message").asText(

                        "S'ha produït un error en l'operació."

                )

        );

    }






    private static class PistaOpcio {

        private final int id;

        private final String nom;



        private PistaOpcio(int id, String nom) {

            this.id = id;

            this.nom = nom;

        }



        @Override

        public String toString() {

            return nom + " (ID " + id + ")";

        }

    }






    public static class Incidencia {

        private final long id;

        private final int pistaId;

        private final String pista;

        private final String descripcio;

        private final String dataInici;

        private final String dataFi;

        private final String dataIniciOriginal;

        private final String dataFiOriginal;

        private final String estat;



        public Incidencia(

                long id,

                int pistaId,

                String pista,

                String descripcio,

                String dataInici,

                String dataFi,

                String estat

        ) {

            this.id = id;

            this.pistaId = pistaId;

            this.pista = pista;

            this.descripcio = descripcio;

            this.dataIniciOriginal = dataInici;
            this.dataFiOriginal = dataFi;
            this.dataInici = formatarData(dataInici);
            this.dataFi = formatarData(dataFi);
            this.estat = estat;

        }



        private static String formatarData(String valor) {

            if (valor == null || valor.isBlank()

                    || "null".equalsIgnoreCase(valor)) {

                return "";

            }



            try {

                return LocalDateTime.parse(

                        valor,

                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                ).format(FORMAT_DATA_HORA);

            } catch (Exception ex) {

                try {

                    return LocalDateTime.parse(

                            valor,

                            DateTimeFormatter.ofPattern(

                                    "yyyy-MM-dd'T'HH:mm:ss"

                            )

                    ).format(FORMAT_DATA_HORA);

                } catch (Exception ignorada) {

                    return valor;

                }

            }

        }



        public long getId() {

            return id;

        }



        public int getPistaId() {

            return pistaId;

        }



        public String getPista() {

            return pista;

        }



        public String getDescripcio() {

            return descripcio;

        }



        public String getDataInici() {

            return dataInici;

        }



        public String getDataFi() {

            return dataFi;

        }



        public String getDataIniciOriginal() {
            return dataIniciOriginal;
        }

        public String getDataFiOriginal() {
            return dataFiOriginal;
        }

        public String getEstat() {

            return estat;

        }

    }

}
