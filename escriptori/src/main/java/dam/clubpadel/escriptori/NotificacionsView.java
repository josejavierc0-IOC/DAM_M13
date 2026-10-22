package dam.clubpadel.escriptori;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificacionsView {

    private final ApiClient api = new ApiClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private TableView<Notificacio> taula;
    private ComboBox<String> filtre;
    private Label missatge;
    private Label resum;
    private Button botoLlegir;
    private Button botoLlegirTotes;

    private static final DateTimeFormatter FORMAT_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Parent crearVista(Runnable tornar) {
        BorderPane arrel = new BorderPane();
        arrel.setPadding(new Insets(16));

        Label titol = new Label("Notificacions");
        titol.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Button botoTornar = new Button("Tornar al menú");
        botoTornar.setOnAction(e -> tornar.run());

        Region espai = new Region();
        HBox.setHgrow(espai, Priority.ALWAYS);

        HBox capcalera = new HBox(12, titol, espai, botoTornar);
        capcalera.setAlignment(Pos.CENTER_LEFT);
        capcalera.setPadding(new Insets(0, 0, 14, 0));
        arrel.setTop(capcalera);

        taula = new TableView<>();
        taula.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        taula.setPlaceholder(new Label("No hi ha notificacions per mostrar."));

        TableColumn<Notificacio, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(75);

        TableColumn<Notificacio, String> colTipus = new TableColumn<>("Tipus");
        colTipus.setCellValueFactory(new PropertyValueFactory<>("tipus"));
        colTipus.setMinWidth(145);

        TableColumn<Notificacio, String> colTitol = new TableColumn<>("Títol");
        colTitol.setCellValueFactory(new PropertyValueFactory<>("titol"));
        colTitol.setMinWidth(170);

        TableColumn<Notificacio, String> colMissatge = new TableColumn<>("Missatge");
        colMissatge.setCellValueFactory(new PropertyValueFactory<>("missatge"));
        colMissatge.setMinWidth(260);

        TableColumn<Notificacio, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colData.setMinWidth(135);

        TableColumn<Notificacio, String> colEstat = new TableColumn<>("Estat");
        colEstat.setCellValueFactory(new PropertyValueFactory<>("estatText"));
        colEstat.setMaxWidth(100);

        taula.getColumns().addAll(colId, colTipus, colTitol, colMissatge, colData, colEstat);
        taula.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Notificacio item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (!item.isLlegida()) {
                    setStyle("-fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        filtre = new ComboBox<>(FXCollections.observableArrayList(
                "Totes", "Pendents de llegir", "Llegides"
        ));
        filtre.setValue("Totes");
        filtre.setOnAction(e -> carregarNotificacions());

        Button botoActualitzar = new Button("Actualitzar");
        botoActualitzar.setOnAction(e -> carregarNotificacions());

        botoLlegir = new Button("Marcar seleccionada com a llegida");
        botoLlegir.setOnAction(e -> marcarSeleccionadaComLlegida());

        botoLlegirTotes = new Button("Marcar totes com a llegides");
        botoLlegirTotes.setOnAction(e -> marcarTotesComLlegides());

        HBox controls = new HBox(10,
                new Label("Mostrar:"), filtre, botoActualitzar,
                botoLlegir, botoLlegirTotes
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 0, 12, 0));

        resum = new Label();
        missatge = new Label();
        missatge.setWrapText(true);
        missatge.getStyleClass().add("missatge");

        VBox centre = new VBox(10, controls, taula, resum, missatge);
        VBox.setVgrow(taula, Priority.ALWAYS);
        arrel.setCenter(centre);

        taula.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> actualitzarBotons()
        );

        carregarNotificacions();
        return arrel;
    }

    private void carregarNotificacions() {
        missatge.setText("Carregant notificacions...");
        String token = Sessio.getToken();

        if (token == null || token.isBlank()) {
            missatge.setText("No hi ha cap sessió activa. Torna a iniciar sessió.");
            return;
        }

        String endpoint = "notificacions";
        String valorFiltre = filtre == null ? "Totes" : filtre.getValue();

        if ("Pendents de llegir".equals(valorFiltre)) {
            endpoint += "&llegida=0";
        } else if ("Llegides".equals(valorFiltre)) {
            endpoint += "&llegida=1";
        }

        try {
            JsonNode resposta = api.get(endpoint, token);

            if (!resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message")
                        .asText("No s'han pogut carregar les notificacions."));
                return;
            }

            var elements = FXCollections.<Notificacio>observableArrayList();
            JsonNode dades = resposta.path("data");

            if (dades.isArray()) {
                for (JsonNode node : dades) {
                    elements.add(new Notificacio(
                            node.path("id").asLong(),
                            node.path("reserva_id").isNull()
                                    ? "" : node.path("reserva_id").asText(""),
                            node.path("tipus").asText("GENERAL"),
                            node.path("titol").asText(""),
                            node.path("missatge").asText(""),
                            node.path("llegida").asInt(0) == 1,
                            node.path("creat_at").asText("")
                    ));
                }
            }

            taula.setItems(elements);
            int pendents = 0;
            for (Notificacio n : elements) {
                if (!n.isLlegida()) pendents++;
            }
            resum.setText("Notificacions mostrades: " + elements.size()
                    + " · Pendents en aquesta llista: " + pendents);
            missatge.setText(elements.isEmpty()
                    ? "No hi ha notificacions amb aquest filtre."
                    : "Notificacions carregades correctament.");
            actualitzarBotons();

        } catch (IOException ex) {
            missatge.setText("Error de comunicació amb l'API: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            missatge.setText("La petició s'ha interromput.");
        }
    }

    private void marcarSeleccionadaComLlegida() {
        Notificacio seleccionada = taula.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            missatge.setText("Selecciona una notificació de la taula.");
            return;
        }

        if (seleccionada.isLlegida()) {
            missatge.setText("Aquesta notificació ja està llegida.");
            return;
        }

        executarPatch(
                "notificacions&accio=llegir&id=" + seleccionada.getId(),
                "La notificació s'ha marcat com a llegida."
        );
    }

    private void marcarTotesComLlegides() {
        Alert confirmacio = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Vols marcar totes les notificacions com a llegides?",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirmacio.setTitle("Confirmar acció");
        confirmacio.setHeaderText("Marcar totes com a llegides");

        if (confirmacio.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        executarPatch(
                "notificacions&accio=llegir_totes",
                "Totes les notificacions s'han marcat com a llegides."
        );
    }

    private void executarPatch(String endpoint, String missatgeExit) {
        try {
            ObjectNode dadesBuides = mapper.createObjectNode();
            JsonNode resposta = api.patch(endpoint, Sessio.getToken(), dadesBuides);

            if (!resposta.path("success").asBoolean(false)) {
                missatge.setText(resposta.path("message")
                        .asText("No s'ha pogut actualitzar la notificació."));
                return;
            }

            missatge.setText(missatgeExit);
            carregarNotificacions();

        } catch (IOException ex) {
            missatge.setText("Error de comunicació amb l'API: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            missatge.setText("La petició s'ha interromput.");
        }
    }

    private void actualitzarBotons() {
        if (botoLlegir == null || botoLlegirTotes == null || taula == null) return;

        Notificacio seleccionada = taula.getSelectionModel().getSelectedItem();
        botoLlegir.setDisable(seleccionada == null || seleccionada.isLlegida());

        boolean hiHaPendents = taula.getItems().stream().anyMatch(n -> !n.isLlegida());
        botoLlegirTotes.setDisable(!hiHaPendents);
    }

    public static class Notificacio {
        private final long id;
        private final String reservaId;
        private final String tipus;
        private final String titol;
        private final String missatge;
        private final boolean llegida;
        private final String data;
        private final String estatText;

        public Notificacio(long id, String reservaId, String tipus,
                           String titol, String missatge,
                           boolean llegida, String dataOriginal) {
            this.id = id;
            this.reservaId = reservaId;
            this.tipus = tipus;
            this.titol = titol;
            this.missatge = missatge;
            this.llegida = llegida;
            this.data = formatarData(dataOriginal);
            this.estatText = llegida ? "Llegida" : "Pendent";
        }

        private static String formatarData(String valor) {
            if (valor == null || valor.isBlank() || "null".equalsIgnoreCase(valor)) {
                return "";
            }

            try {
                return LocalDateTime.parse(valor.replace(' ', 'T'))
                        .format(FORMAT_DATA_HORA);
            } catch (Exception ex) {
                return valor;
            }
        }

        public long getId() { return id; }
        public String getReservaId() { return reservaId; }
        public String getTipus() { return tipus; }
        public String getTitol() { return titol; }
        public String getMissatge() { return missatge; }
        public boolean isLlegida() { return llegida; }
        public String getData() { return data; }
        public String getEstatText() { return estatText; }
    }
}
