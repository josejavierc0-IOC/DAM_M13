
<?php
// Controlador de reserves d'IOC Padel Pass.
// Gestiona la creació, consulta, modificació, cancel·lació i pagament de reserves.

/**
 * Busca el soci associat a un usuari.
 */
function obtenirSociUsuari($pdo, $usuariId)
{
    $stmt = $pdo->prepare(
        "SELECT id
         FROM socis
         WHERE usuari_id = :usuari_id
         LIMIT 1"
    );

    $stmt->execute(['usuari_id' => $usuariId]);

    return $stmt->fetch(PDO::FETCH_ASSOC);
}

/**
 * Crea una notificació si el controlador està carregat.
 * La notificació s'adreça al propietari de la reserva.
 */
function notificarReservaSiDisponible(
    $pdo,
    $usuariId,
    $reservaId,
    $tipus,
    $titol,
    $missatge
) {
    if (!function_exists('crearNotificacio')) {
        error_log(
            'No s\'ha pogut crear la notificació: ' .
            'la funció crearNotificacio() no està carregada.'
        );
        return false;
    }

    try {
        return crearNotificacio(
            $pdo,
            (int) $usuariId,
            $reservaId === null ? null : (int) $reservaId,
            $tipus,
            $titol,
            $missatge
        );
    } catch (Throwable $e) {
        // Una fallada de notificació no ha de desfer una reserva.
        error_log('Error creant notificació de reserva: ' . $e->getMessage());
        return false;
    }
}

/**
 * Obté l'usuari propietari d'una reserva.
 */
function obtenirPropietariReserva($pdo, $reservaId)
{
    $stmt = $pdo->prepare(
        "SELECT s.usuari_id
         FROM reserves r
         INNER JOIN socis s ON s.id = r.soci_id
         WHERE r.id = :id
         LIMIT 1"
    );

    $stmt->execute(['id' => $reservaId]);

    $resultat = $stmt->fetch(PDO::FETCH_ASSOC);

    return $resultat ? (int) $resultat['usuari_id'] : null;
}

/**
 * Comprova si una data té el format correcte.
 */
function dataReservaValida($data)
{
    if (!is_string($data) || $data === '') {
        return false;
    }

    $dataObjecte = DateTime::createFromFormat('!Y-m-d', $data);
    $errors = DateTime::getLastErrors();

    return $dataObjecte !== false
        && ($errors === false
            || ($errors['warning_count'] === 0
                && $errors['error_count'] === 0))
        && $dataObjecte->format('Y-m-d') === $data;
}

/**
 * Valida i normalitza una hora al format HH:MM:SS.
 */
function normalitzarHoraReserva($hora)
{
    if (!is_string($hora)
        || !preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $hora)) {
        return false;
    }

    if (strlen($hora) === 5) {
        $hora .= ':00';
    }

    $objecte = DateTime::createFromFormat('!H:i:s', $hora);
    $errors = DateTime::getLastErrors();

    if (
        !$objecte
        || ($errors !== false
            && ($errors['warning_count'] > 0 || $errors['error_count'] > 0))
        || $objecte->format('H:i:s') !== $hora
    ) {
        return false;
    }

    return $hora;
}

/**
 * Consulta les reserves pròpies d'un usuari.
 * Un administrador pot consultar totes les reserves.
 * Permet filtrar per data i pista.
 */
function llistarReserves($pdo, $usuari)
{
    $esAdmin = ($usuari['rol'] ?? '') === 'ADMIN';

    $data = $_GET['data'] ?? null;
    $pistaId = $_GET['pista_id'] ?? null;

    if ($data !== null && !dataReservaValida($data)) {
        return [
            'success' => false,
            'message' => 'La data indicada no és vàlida. Utilitza YYYY-MM-DD.'
        ];
    }

    if ($pistaId !== null) {
        $pistaId = filter_var($pistaId, FILTER_VALIDATE_INT);

        if (!$pistaId || $pistaId < 1) {
            return [
                'success' => false,
                'message' => 'L’identificador de pista no és vàlid.'
            ];
        }
    }

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

    if (!$esAdmin) {
        $soci = obtenirSociUsuari($pdo, $usuari['id']);

        if (!$soci) {
            return [
                'success' => false,
                'message' => 'L’usuari no té cap soci associat.'
            ];
        }

        $sql .= " AND r.soci_id = :soci_id";
        $parametres['soci_id'] = (int) $soci['id'];
    }

    if ($data !== null) {
        $sql .= " AND r.data_reserva = :data";
        $parametres['data'] = $data;
    }

    if ($pistaId !== null) {
        $sql .= " AND r.pista_id = :pista_id";
        $parametres['pista_id'] = $pistaId;
    }

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

    $esAdmin = ($usuari['rol'] ?? '') === 'ADMIN';

    if (
        !$esAdmin
        && (int) $reserva['usuari_id'] !== (int) $usuari['id']
    ) {
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
    $horaInici = normalitzarHoraReserva($dades['hora_inici'] ?? '');
    $horaFi = normalitzarHoraReserva($dades['hora_fi'] ?? '');

    if (
        !$pistaId || $pistaId < 1
        || !$tarifaId || $tarifaId < 1
        || !dataReservaValida($data)
        || $horaInici === false
        || $horaFi === false
    ) {
        return [
            'success' => false,
            'message' => 'Les dades de la reserva no són vàlides.'
        ];
    }

    if ($horaInici >= $horaFi) {
        return [
            'success' => false,
            'message' => 'L’hora final ha de ser posterior a la inicial.'
        ];
    }

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

    // Determinem el soci propietari de la reserva.
    if ($esAdmin && !empty($dades['soci_id'])) {
        $sociId = filter_var(
            $dades['soci_id'],
            FILTER_VALIDATE_INT
        );

        if (!$sociId || $sociId < 1) {
            return [
                'success' => false,
                'message' => 'L’identificador del soci no és vàlid.'
            ];
        }

        $stmt = $pdo->prepare(
            "SELECT id, usuari_id
             FROM socis
             WHERE id = :id"
        );
        $stmt->execute(['id' => $sociId]);
        $soci = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$soci) {
            return [
                'success' => false,
                'message' => 'El soci indicat no existeix.'
            ];
        }

        $usuariDestinatari = (int) $soci['usuari_id'];
    } else {
        $soci = obtenirSociUsuari($pdo, $usuari['id']);

        if (!$soci) {
            return [
                'success' => false,
                'message' => 'No tens cap soci associat per reservar.'
            ];
        }

        $sociId = (int) $soci['id'];
        $usuariDestinatari = (int) $usuari['id'];
    }

    // Comprovem que la pista existeix i està activa.
    $stmt = $pdo->prepare(
        "SELECT id
         FROM pistes
         WHERE id = :id AND activa = 1"
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

    // Comprovem l'horari de la pista.
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

    // Comprovem que no hi hagi una reserva coincident.
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

    // Una incidència oberta impedeix reservar aquella franja.
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

    // Creem la reserva.
    $stmt = $pdo->prepare(
        "INSERT INTO reserves
            (soci_id, pista_id, tarifa_id,
             data_reserva, hora_inici, hora_fi,
             estat, pagament_estat, pagament_metode, creada_per)
         VALUES
            (:soci_id, :pista_id, :tarifa_id,
             :data, :hora_inici, :hora_fi,
             'CONFIRMADA', 'PENDENT', 'PRESENCIAL', :creada_per)"
    );

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

    // Notifiquem el propietari de la reserva, no necessàriament
    // l'administrador que l'ha creat.
    notificarReservaSiDisponible(
        $pdo,
        $usuariDestinatari,
        $reservaId,
        'NOVA_RESERVA',
        'Nova reserva creada',
        "S'ha creat una reserva per al dia $data, de $horaInici a $horaFi."
    );

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
    if (($usuari['rol'] ?? '') !== 'ADMIN') {
        return [
            'success' => false,
            'message' => 'No tens permís per modificar reserves.'
        ];
    }

    $reservaId = filter_var($reservaId, FILTER_VALIDATE_INT);

    if (!$reservaId || $reservaId < 1) {
        return [
            'success' => false,
            'message' => 'L’identificador de la reserva no és vàlid.'
        ];
    }

    // Obtenim la reserva i el propietari per notificar-lo després.
    $stmt = $pdo->prepare(
        "SELECT r.id, r.estat, r.data_reserva, r.hora_inici,
                r.hora_fi, s.usuari_id
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

    $pistaId = filter_var(
        $dades['pista_id'] ?? null,
        FILTER_VALIDATE_INT
    );

    $tarifaId = filter_var(
        $dades['tarifa_id'] ?? null,
        FILTER_VALIDATE_INT
    );

    $data = $dades['data_reserva'] ?? '';
    $horaInici = normalitzarHoraReserva($dades['hora_inici'] ?? '');
    $horaFi = normalitzarHoraReserva($dades['hora_fi'] ?? '');

    if (
        !$pistaId || $pistaId < 1
        || !$tarifaId || $tarifaId < 1
        || !dataReservaValida($data)
        || $horaInici === false
        || $horaFi === false
    ) {
        return [
            'success' => false,
            'message' => 'Les dades de la reserva no són vàlides.'
        ];
    }

    if ($horaInici >= $horaFi) {
        return [
            'success' => false,
            'message' => 'L’hora final ha de ser posterior a la inicial.'
        ];
    }

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

    $minutsInici = strtotime("1970-01-01 $horaInici");
    $minutsFi = strtotime("1970-01-01 $horaFi");
    $durada = ($minutsFi - $minutsInici) / 60;

    if ((int) $durada !== (int) $tarifa['durada_minuts']) {
        return [
            'success' => false,
            'message' => 'La durada no coincideix amb la tarifa.'
        ];
    }

    // Comprovem l'horari de la pista.
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

    // Excloem la reserva que estem modificant de la comprovació.
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

    // Comprovem si hi ha una incidència oberta.
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

    // Notifiquem el propietari de la reserva modificada.
    $canviReserva = (
        $reservaActual['data_reserva'] !== $data
        || $reservaActual['hora_inici'] !== $horaInici
        || $reservaActual['hora_fi'] !== $horaFi
    );

    if ($canviReserva) {
        notificarReservaSiDisponible(
            $pdo,
            (int) $reservaActual['usuari_id'],
            $reservaId,
            'MODIFICACIO_RESERVA',
            'Reserva modificada',
            "La reserva ha estat modificada. Nova data: $data, de $horaInici a $horaFi."
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
    $reservaId = filter_var($reservaId, FILTER_VALIDATE_INT);

    if (!$reservaId || $reservaId < 1) {
        return [
            'success' => false,
            'message' => 'L’identificador de la reserva no és vàlid.'
        ];
    }

    $stmt = $pdo->prepare(
        "SELECT r.id, r.estat, r.data_reserva, r.hora_inici,
                r.hora_fi, s.usuari_id
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

    if (
        !$esAdmin
        && (int) $reserva['usuari_id'] !== (int) $usuari['id']
    ) {
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
         WHERE id = :id
           AND estat = 'CONFIRMADA'"
    );

    $stmt->execute(['id' => $reservaId]);

    if ($stmt->rowCount() !== 1) {
        return [
            'success' => false,
            'message' => 'No s’ha pogut cancel·lar la reserva.'
        ];
    }

    // Notifiquem el propietari de la reserva cancel·lada.
    notificarReservaSiDisponible(
        $pdo,
        (int) $reserva['usuari_id'],
        $reservaId,
        'CANCEL·LACIO_RESERVA',
        'Reserva cancel·lada',
        "La reserva del dia {$reserva['data_reserva']}, de {$reserva['hora_inici']} a {$reserva['hora_fi']}, ha estat cancel·lada."
    );

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
    if (($usuari['rol'] ?? '') !== 'ADMIN') {
        return [
            'success' => false,
            'message' => 'No tens permís per gestionar els pagaments.'
        ];
    }

    $reservaId = filter_var($reservaId, FILTER_VALIDATE_INT);

    if (!$reservaId || $reservaId < 1) {
        return [
            'success' => false,
            'message' => 'L’identificador de la reserva no és vàlid.'
        ];
    }

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

    if ($reserva['estat'] !== 'CONFIRMADA') {
        return [
            'success' => false,
            'message' => 'Només es poden cobrar reserves confirmades.'
        ];
    }

    if ($reserva['pagament_estat'] === 'PAGAT') {
        return [
            'success' => false,
            'message' => 'Aquesta reserva ja està pagada.'
        ];
    }

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
