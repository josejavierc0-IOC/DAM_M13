
<?php
// Controlador de reserves d'IOC Padel Pass.
// Gestiona la creació, consulta i cancel·lació de reserves.

/**
 * Busca el soci associat a un usuari.
 */
function obtenirSociUsuari($pdo, $usuariId)
{
    $sql = "SELECT id
            FROM socis
            WHERE usuari_id = :usuari_id";

    $stmt = $pdo->prepare($sql);
    $stmt->execute(['usuari_id' => $usuariId]);

    return $stmt->fetch(PDO::FETCH_ASSOC);
}

/**
 * Comprova si una data té el format correcte.
 */
function dataReservaValida($data)
{
    $dataObjecte = DateTime::createFromFormat('!Y-m-d', $data);

    return $dataObjecte &&
        $dataObjecte->format('Y-m-d') === $data;
}


/**
 * Consulta les reserves pròpies d'un usuari.
 * Un administrador pot consultar totes les reserves.
 * Permet filtrar per data i pista.
 */
function llistarReserves($pdo, $usuari)
{
    $esAdmin = ($usuari['rol'] ?? '') === 'ADMIN';

    // Llegim els filtres opcionals.
    $data = $_GET['data'] ?? null;
    $pistaId = $_GET['pista_id'] ?? null;

    // Validem la data si s'ha indicat.
    if ($data !== null && !dataReservaValida($data)) {
        return [
            'success' => false,
            'message' => 'La data indicada no és vàlida. Utilitza YYYY-MM-DD.'
        ];
    }

    // Validem l'identificador de pista si s'ha indicat.
    if ($pistaId !== null) {
        $pistaId = filter_var($pistaId, FILTER_VALIDATE_INT);

        if (!$pistaId || $pistaId < 1) {
            return [
                'success' => false,
                'message' => 'L’identificador de pista no és vàlid.'
            ];
        }
    }

    // Consulta comuna per a administradors i socis.
    $sql = "SELECT r.*, p.nom AS pista_nom,
                   s.numero_soci,
                   u.nom AS usuari_nom,
                   u.cognoms AS usuari_cognoms
            FROM reserves r
            INNER JOIN pistes p ON p.id = r.pista_id
            INNER JOIN socis s ON s.id = r.soci_id
            INNER JOIN usuaris u ON u.id = s.usuari_id
            WHERE 1 = 1";

    $parametres = [];

    // Els usuaris normals només poden veure les seves reserves.
    if (!$esAdmin) {
        $soci = obtenirSociUsuari($pdo, $usuari['id']);

        if (!$soci) {
            return [
                'success' => false,
                'message' => 'L’usuari no té cap soci associat.'
            ];
        }

        $sql .= " AND r.soci_id = :soci_id";
        $parametres['soci_id'] = $soci['id'];
    }

    // Apliquem el filtre per data, si s'ha indicat.
    if ($data !== null) {
        $sql .= " AND r.data_reserva = :data";
        $parametres['data'] = $data;
    }

    // Apliquem el filtre per pista, si s'ha indicat.
    if ($pistaId !== null) {
        $sql .= " AND r.pista_id = :pista_id";
        $parametres['pista_id'] = $pistaId;
    }

    // Ordenem les reserves per data i hora.
    $sql .= " ORDER BY r.data_reserva DESC, r.hora_inici DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($parametres);

    return [
        'success' => true,
        'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ];
}

/**
 * Consulta una reserva concreta.
 * Els usuaris només poden veure les seves reserves.
 */
function consultarReserva($pdo, $reservaId, $usuari)
{
    $sql = "SELECT r.*, p.nom AS pista_nom,
               s.numero_soci,
               s.usuari_id AS usuari_id,
               u.nom AS usuari_nom,
               u.cognoms AS usuari_cognoms
        FROM reserves r
        INNER JOIN pistes p ON p.id = r.pista_id
        INNER JOIN socis s ON s.id = r.soci_id
        INNER JOIN usuaris u ON u.id = s.usuari_id
        WHERE r.id = :id";

    $stmt = $pdo->prepare($sql);
    $stmt->execute(['id' => $reservaId]);

    $reserva = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$reserva) {
        return [
            'success' => false,
            'message' => 'No s’ha trobat la reserva.'
        ];
    }

    // Només un ADMIN o el propietari pot consultar-la.
    $esAdmin = ($usuari['rol'] ?? '') === 'ADMIN';

    if (!$esAdmin && (int) $reserva['usuari_id'] !== (int) $usuari['id']) {
        return [
            'success' => false,
            'message' => 'No tens permís per consultar aquesta reserva.'
        ];
    }

    return [
        'success' => true,
        'data' => $reserva
    ];
}

/**
 * Crea una reserva comprovant horaris, tarifa,
 * disponibilitat, reserves existents i incidències.
 */
function crearReserva($pdo, $usuari, $dades)
{
    $pistaId = filter_var(
        $dades['pista_id'] ?? null,
        FILTER_VALIDATE_INT
    );

    $tarifaId = filter_var(
        $dades['tarifa_id'] ?? null,
        FILTER_VALIDATE_INT
    );

    $data = $dades['data_reserva'] ?? '';
    $horaInici = $dades['hora_inici'] ?? '';
    $horaFi = $dades['hora_fi'] ?? '';

    if (
        !$pistaId || !$tarifaId ||
        !dataReservaValida($data) ||
        !preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $horaInici) ||
        !preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $horaFi)
    ) {
        return [
            'success' => false,
            'message' => 'Les dades de la reserva no són vàlides.'
        ];
    }

    // Normalitzem les hores al format HH:MM:SS.
    if (strlen($horaInici) === 5) {
        $horaInici .= ':00';
    }

    if (strlen($horaFi) === 5) {
        $horaFi .= ':00';
    }

    if ($horaInici >= $horaFi) {
        return [
            'success' => false,
            'message' => 'L’hora final ha de ser posterior a la inicial.'
        ];
    }

    // No permetem reservar dates o hores que ja han passat.
    $inici = DateTime::createFromFormat(
        '!Y-m-d H:i:s',
        "$data $horaInici"
    );

    if (!$inici || $inici <= new DateTime()) {
        return [
            'success' => false,
            'message' => 'La reserva ha de ser en una data i hora futures.'
        ];
    }

    $esAdmin = ($usuari['rol'] ?? '') === 'ADMIN';

    // Un usuari normal només pot reservar per a ell mateix.
    if ($esAdmin && !empty($dades['soci_id'])) {
        $sociId = filter_var(
            $dades['soci_id'],
            FILTER_VALIDATE_INT
        );

        $stmt = $pdo->prepare(
            "SELECT id FROM socis WHERE id = :id"
        );
        $stmt->execute(['id' => $sociId]);

        if (!$sociId || !$stmt->fetch()) {
            return [
                'success' => false,
                'message' => 'El soci indicat no existeix.'
            ];
        }
    } else {
        $soci = obtenirSociUsuari($pdo, $usuari['id']);

        if (!$soci) {
            return [
                'success' => false,
                'message' => 'No tens cap soci associat per reservar.'
            ];
        }

        $sociId = $soci['id'];
    }

    // Comprovem que la pista existeix i està activa.
    $stmt = $pdo->prepare(
        "SELECT id FROM pistes WHERE id = :id AND activa = 1"
    );
    $stmt->execute(['id' => $pistaId]);

    if (!$stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La pista no existeix o no està activa.'
        ];
    }

    // Comprovem la tarifa i la seva vigència.
    $stmt = $pdo->prepare(
        "SELECT *
         FROM tarifes
         WHERE id = :id
           AND activa = 1
           AND (data_inici IS NULL OR data_inici <= :data1)
           AND (data_fi IS NULL OR data_fi >= :data2)"
    );

    $stmt->execute([
        'id' => $tarifaId,
        'data1' => $data,
        'data2' => $data
    ]);

    $tarifa = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$tarifa) {
        return [
            'success' => false,
            'message' => 'La tarifa no existeix o no és vigent.'
        ];
    }

    // La durada ha de coincidir amb la tarifa seleccionada.
    $minutsInici = strtotime("1970-01-01 $horaInici");
    $minutsFi = strtotime("1970-01-01 $horaFi");
    $durada = ($minutsFi - $minutsInici) / 60;

    if ((int) $durada !== (int) $tarifa['durada_minuts']) {
        return [
            'success' => false,
            'message' => 'La durada no coincideix amb la tarifa.'
        ];
    }

    // Comprovem que la reserva està dins de l'horari de la pista.
    $diaSetmana = (int) (new DateTime($data))->format('N');

    $stmt = $pdo->prepare(
        "SELECT id
         FROM horaris_pista
         WHERE pista_id = :pista_id
           AND dia_setmana = :dia
           AND actiu = 1
           AND hora_inici <= :hora_inici
           AND hora_fi >= :hora_fi
         LIMIT 1"
    );

    $stmt->execute([
        'pista_id' => $pistaId,
        'dia' => $diaSetmana,
        'hora_inici' => $horaInici,
        'hora_fi' => $horaFi
    ]);

    if (!$stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La reserva queda fora de l’horari de la pista.'
        ];
    }

    // Una reserva confirmada no pot coincidir amb una altra.
    $stmt = $pdo->prepare(
        "SELECT id
         FROM reserves
         WHERE pista_id = :pista_id
           AND data_reserva = :data
           AND estat = 'CONFIRMADA'
           AND hora_inici < :hora_fi
           AND hora_fi > :hora_inici
         LIMIT 1"
    );

    $stmt->execute([
        'pista_id' => $pistaId,
        'data' => $data,
        'hora_inici' => $horaInici,
        'hora_fi' => $horaFi
    ]);

    if ($stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La pista ja està reservada en aquesta franja.'
        ];
    }

    // Una incidència oberta tampoc permet reservar aquella franja.
    $stmt = $pdo->prepare(
        "SELECT id
         FROM incidencies
         WHERE pista_id = :pista_id
           AND estat = 'OBERTA'
           AND data_inici < :data_fi
           AND (data_fi IS NULL OR data_fi > :data_inici)
         LIMIT 1"
    );

    $stmt->execute([
        'pista_id' => $pistaId,
        'data_inici' => "$data $horaInici",
        'data_fi' => "$data $horaFi"
    ]);

    if ($stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La pista té una incidència en aquesta franja.'
        ];
    }

    // Creem la reserva. El pagament serà presencial.
    $sql = "INSERT INTO reserves
                (soci_id, pista_id, tarifa_id,
                 data_reserva, hora_inici, hora_fi,
                 estat, pagament_estat, pagament_metode, creada_per)
            VALUES
                (:soci_id, :pista_id, :tarifa_id,
                 :data, :hora_inici, :hora_fi,
                 'CONFIRMADA', 'PENDENT', 'PRESENCIAL', :creada_per)";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        'soci_id' => $sociId,
        'pista_id' => $pistaId,
        'tarifa_id' => $tarifaId,
        'data' => $data,
        'hora_inici' => $horaInici,
        'hora_fi' => $horaFi,
        'creada_per' => $usuari['id']
    ]);
    $reservaId = (int) $pdo->lastInsertId();

$stmtSoci = $pdo->prepare(
    "SELECT usuari_id
     FROM socis
     WHERE id = :soci_id"
);

$stmtSoci->execute(['soci_id' => $sociId]);
$usuariSociId = $stmtSoci->fetchColumn();

if ($usuariSociId) {
    crearNotificacio(
        $pdo,
        (int) $usuariSociId,
        $reservaId,
        'NOVA_RESERVA',
        'Reserva confirmada',
        "La reserva núm. {$reservaId} s'ha creat correctament."
    );
}

    return [
        'success' => true,
        'message' => 'Reserva creada correctament.',
        'id' => $reservaId,
        'preu' => round(
            (float) $tarifa['preu'] * $durada / 60,
            2
        )
    ];
}


/**
 * Modifica una reserva existent.
 * Només l'administrador pot modificar reserves confirmades.
 */
function modificarReserva($pdo, $reservaId, $usuari, $dades)
{
    // Només els administradors poden modificar reserves.
    if (($usuari['rol'] ?? '') !== 'ADMIN') {
        return [
            'success' => false,
            'message' => 'No tens permís per modificar reserves.'
        ];
    }

    // Validem l'identificador de la reserva.
    $reservaId = filter_var($reservaId, FILTER_VALIDATE_INT);

    if (!$reservaId || $reservaId < 1) {
        return [
            'success' => false,
            'message' => 'L’identificador de la reserva no és vàlid.'
        ];
    }

    // Comprovem que la reserva existeix i està confirmada.
    $stmt = $pdo->prepare(
        "SELECT r.id, r.estat, s.usuari_id
         FROM reserves r
         INNER JOIN socis s ON s.id = r.soci_id
         WHERE r.id = :id"
    );
    $stmt->execute(['id' => $reservaId]);
    $reservaActual = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$reservaActual) {
        return [
            'success' => false,
            'message' => 'No s’ha trobat la reserva.'
        ];
    }

    if ($reservaActual['estat'] !== 'CONFIRMADA') {
        return [
            'success' => false,
            'message' => 'Només es poden modificar reserves confirmades.'
        ];
    }

    // Llegim i validem les dades noves.
    $pistaId = filter_var(
        $dades['pista_id'] ?? null,
        FILTER_VALIDATE_INT
    );

    $tarifaId = filter_var(
        $dades['tarifa_id'] ?? null,
        FILTER_VALIDATE_INT
    );

    $data = $dades['data_reserva'] ?? '';
    $horaInici = $dades['hora_inici'] ?? '';
    $horaFi = $dades['hora_fi'] ?? '';

    if (
        !$pistaId || !$tarifaId ||
        !dataReservaValida($data) ||
        !preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $horaInici) ||
        !preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $horaFi)
    ) {
        return [
            'success' => false,
            'message' => 'Les dades de la reserva no són vàlides.'
        ];
    }

    // Normalitzem les hores al format HH:MM:SS.
    if (strlen($horaInici) === 5) {
        $horaInici .= ':00';
    }

    if (strlen($horaFi) === 5) {
        $horaFi .= ':00';
    }

    // Validem que les hores existeixin i estiguin ordenades.
    $inici = DateTime::createFromFormat('!H:i:s', $horaInici);
    $fi = DateTime::createFromFormat('!H:i:s', $horaFi);

    if (
        !$inici || !$fi ||
        $inici->format('H:i:s') !== $horaInici ||
        $fi->format('H:i:s') !== $horaFi ||
        $horaInici >= $horaFi
    ) {
        return [
            'success' => false,
            'message' => 'Les hores indicades no són vàlides.'
        ];
    }

    // La nova data i hora han de ser futures.
    $dataHoraInici = DateTime::createFromFormat(
        '!Y-m-d H:i:s',
        "$data $horaInici"
    );

    if (!$dataHoraInici || $dataHoraInici <= new DateTime()) {
        return [
            'success' => false,
            'message' => 'La reserva ha de ser en una data i hora futures.'
        ];
    }

    // Comprovem que la pista existeix i està activa.
    $stmt = $pdo->prepare(
        "SELECT id FROM pistes WHERE id = :id AND activa = 1"
    );
    $stmt->execute(['id' => $pistaId]);

    if (!$stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La pista no existeix o no està activa.'
        ];
    }

    // Comprovem que la tarifa està activa i és vigent.
    $stmt = $pdo->prepare(
        "SELECT *
         FROM tarifes
         WHERE id = :id
           AND activa = 1
           AND (data_inici IS NULL OR data_inici <= :data1)
           AND (data_fi IS NULL OR data_fi >= :data2)"
    );
    $stmt->execute([
        'id' => $tarifaId,
        'data1' => $data,
        'data2' => $data
    ]);

    $tarifa = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$tarifa) {
        return [
            'success' => false,
            'message' => 'La tarifa no existeix o no és vigent.'
        ];
    }

    // La durada ha de coincidir amb la tarifa.
    $minutsInici = strtotime("1970-01-01 $horaInici");
    $minutsFi = strtotime("1970-01-01 $horaFi");
    $durada = ($minutsFi - $minutsInici) / 60;

    if ((int) $durada !== (int) $tarifa['durada_minuts']) {
        return [
            'success' => false,
            'message' => 'La durada no coincideix amb la tarifa.'
        ];
    }

    // Comprovem que la franja està dins l'horari de la pista.
    $diaSetmana = (int) (new DateTime($data))->format('N');

    $stmt = $pdo->prepare(
        "SELECT id
         FROM horaris_pista
         WHERE pista_id = :pista_id
           AND dia_setmana = :dia
           AND actiu = 1
           AND hora_inici <= :hora_inici
           AND hora_fi >= :hora_fi
         LIMIT 1"
    );
    $stmt->execute([
        'pista_id' => $pistaId,
        'dia' => $diaSetmana,
        'hora_inici' => $horaInici,
        'hora_fi' => $horaFi
    ]);

    if (!$stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La reserva queda fora de l’horari de la pista.'
        ];
    }

    // Comprovem que no coincideixi amb una altra reserva confirmada.
    // Excloem la reserva que estem modificant.
    $stmt = $pdo->prepare(
        "SELECT id
         FROM reserves
         WHERE pista_id = :pista_id
           AND data_reserva = :data
           AND estat = 'CONFIRMADA'
           AND id <> :reserva_id
           AND hora_inici < :hora_fi
           AND hora_fi > :hora_inici
         LIMIT 1"
    );
    $stmt->execute([
        'pista_id' => $pistaId,
        'data' => $data,
        'reserva_id' => $reservaId,
        'hora_fi' => $horaFi,
        'hora_inici' => $horaInici
    ]);

    if ($stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La pista ja està reservada en aquesta franja.'
        ];
    }

    // Comprovem que no hi hagi cap incidència oberta en aquella franja.
    $stmt = $pdo->prepare(
        "SELECT id
         FROM incidencies
         WHERE pista_id = :pista_id
           AND estat = 'OBERTA'
           AND data_inici < :data_fi
           AND (data_fi IS NULL OR data_fi > :data_inici)
         LIMIT 1"
    );
    $stmt->execute([
        'pista_id' => $pistaId,
        'data_fi' => "$data $horaFi",
        'data_inici' => "$data $horaInici"
    ]);

    if ($stmt->fetch()) {
        return [
            'success' => false,
            'message' => 'La pista té una incidència en aquesta franja.'
        ];
    }

    // Actualitzem la reserva sense alterar-ne l'estat de pagament.
    $stmt = $pdo->prepare(
        "UPDATE reserves
         SET pista_id = :pista_id,
             tarifa_id = :tarifa_id,
             data_reserva = :data,
             hora_inici = :hora_inici,
             hora_fi = :hora_fi
         WHERE id = :id
           AND estat = 'CONFIRMADA'"
    );

    $stmt->execute([
        'pista_id' => $pistaId,
        'tarifa_id' => $tarifaId,
        'data' => $data,
        'hora_inici' => $horaInici,
        'hora_fi' => $horaFi,
        'id' => $reservaId
    ]);

    if ($stmt->rowCount() > 0 && !empty($reservaActual['usuari_id'])) {
        crearNotificacio(
            $pdo,
            (int) $reservaActual['usuari_id'],
            $reservaId,
            'MODIFICACIO_RESERVA',
            'Reserva modificada',
            "La reserva núm. {$reservaId} ha estat modificada pel personal del club."
        );
    }

    return [
        'success' => true,
        'message' => 'Reserva modificada correctament.',
        'id' => $reservaId,
        'preu' => round(
            (float) $tarifa['preu'] * $durada / 60,
            2
        )
    ];
}
/**
 * Cancel·la una reserva pròpia o, si és ADMIN,
 * una reserva de qualsevol soci.
 */
function cancelLarReserva($pdo, $reservaId, $usuari)
{
    $stmt = $pdo->prepare(
        "SELECT r.id, r.estat, s.usuari_id
         FROM reserves r
         INNER JOIN socis s ON s.id = r.soci_id
         WHERE r.id = :id"
    );

    $stmt->execute(['id' => $reservaId]);
    $reserva = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$reserva) {
        return [
            'success' => false,
            'message' => 'No s’ha trobat la reserva.'
        ];
    }

    $esAdmin = ($usuari['rol'] ?? '') === 'ADMIN';

    if (!$esAdmin && (int) $reserva['usuari_id'] !== (int) $usuari['id']) {
        return [
            'success' => false,
            'message' => 'No tens permís per cancel·lar aquesta reserva.'
        ];
    }

    if ($reserva['estat'] !== 'CONFIRMADA') {
        return [
            'success' => false,
            'message' => 'Només es poden cancel·lar reserves confirmades.'
        ];
    }

    $stmt = $pdo->prepare(
        "UPDATE reserves
         SET estat = 'CANCEL·LADA'
         WHERE id = :id AND estat = 'CONFIRMADA'"
    );

    $stmt->execute(['id' => $reservaId]);

    if ($stmt->rowCount() > 0 && !empty($reserva['usuari_id'])) {
        crearNotificacio(
            $pdo,
            (int) $reserva['usuari_id'],
            (int) $reservaId,
            'CANCEL·LACIO_RESERVA',
            'Reserva cancel·lada',
            "La reserva núm. {$reservaId} ha estat cancel·lada."
        );
    }

    return [
        'success' => true,
        'message' => 'Reserva cancel·lada correctament.'
    ];
}


/**
 * Marca una reserva com a pagada.
 * Només ho pot fer un administrador.
 */
function marcarReservaPagada($pdo, $reservaId, $usuari)
{
    // Només l'administrador pot gestionar els pagaments.
    if (($usuari['rol'] ?? '') !== 'ADMIN') {
        return [
            'success' => false,
            'message' => 'No tens permís per gestionar els pagaments.'
        ];
    }

    // Comprovem que la reserva existeix.
    $stmt = $pdo->prepare(
        "SELECT id, estat, pagament_estat
         FROM reserves
         WHERE id = :id"
    );

    $stmt->execute(['id' => $reservaId]);
    $reserva = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$reserva) {
        return [
            'success' => false,
            'message' => 'No s’ha trobat la reserva.'
        ];
    }

    // No permetem cobrar una reserva cancel·lada.
    if ($reserva['estat'] !== 'CONFIRMADA') {
        return [
            'success' => false,
            'message' => 'Només es poden cobrar reserves confirmades.'
        ];
    }

    // Evitem marcar com a pagada una reserva ja pagada.
    if ($reserva['pagament_estat'] === 'PAGAT') {
        return [
            'success' => false,
            'message' => 'Aquesta reserva ja està pagada.'
        ];
    }

    // Actualitzem l'estat del pagament.
    $stmt = $pdo->prepare(
        "UPDATE reserves
         SET pagament_estat = 'PAGAT',
             pagament_metode = 'PRESENCIAL'
         WHERE id = :id
           AND estat = 'CONFIRMADA'
           AND pagament_estat = 'PENDENT'"
    );

    $stmt->execute(['id' => $reservaId]);

    if ($stmt->rowCount() !== 1) {
        return [
            'success' => false,
            'message' => 'No s’ha pogut actualitzar el pagament.'
        ];
    }

    return [
        'success' => true,
        'message' => 'Pagament registrat correctament.',
        'reserva_id' => $reservaId,
        'pagament_estat' => 'PAGAT',
        'pagament_metode' => 'PRESENCIAL'
    ];
}
