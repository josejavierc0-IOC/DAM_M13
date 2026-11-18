<?php

/**
 * Crea una notificació per a un usuari.
 */
function crearNotificacio(
    $pdo,
    $usuariId,
    $reservaId,
    $tipus,
    $titol,
    $missatge
) {
    $tipusPermesos = [
        'NOVA_RESERVA',
        'CANCEL·LACIO_RESERVA',
        'MODIFICACIO_RESERVA',
        'GENERAL'
    ];

    if (!in_array($tipus, $tipusPermesos, true)) {
        return false;
    }

    $stmt = $pdo->prepare(
        "INSERT INTO notificacions
            (usuari_id, reserva_id, tipus, titol, missatge)
         VALUES
            (:usuari_id, :reserva_id, :tipus, :titol, :missatge)"
    );

    return $stmt->execute([
        'usuari_id' => (int) $usuariId,
        'reserva_id' => $reservaId !== null ? (int) $reservaId : null,
        'tipus' => $tipus,
        'titol' => $titol,
        'missatge' => $missatge
    ]);
}


/**
 * Determina si l'usuari autenticat és administrador.
 *
 * El login i la validació del token han de proporcionar
 * el rol amb la clau "rol", tal com fa l'API.
 */
function esAdministradorNotificacions($usuari)
{
    return strtoupper(trim((string) ($usuari['rol'] ?? ''))) === 'ADMIN';
}


/**
 * Retorna les notificacions.
 *
 * ADMIN: pot consultar les notificacions de tots els usuaris.
 * Altres usuaris: només poden consultar les seves notificacions.
 *
 * Es pot filtrar per notificacions llegides o pendents.
 */
function llistarNotificacions($pdo, $usuari, $dades = [])
{
    $esAdmin = esAdministradorNotificacions($usuari);

    $sql = "SELECT
                n.id,
                n.usuari_id,
                n.reserva_id,
                n.tipus,
                n.titol,
                n.missatge,
                n.llegida,
                n.creat_at,
                u.nom AS usuari_nom,
                u.cognoms AS usuari_cognoms,
                u.email AS usuari_email
            FROM notificacions n
            INNER JOIN usuaris u ON u.id = n.usuari_id";

    $parametres = [];

    // Els socis només poden consultar les notificacions pròpies.
    if (!$esAdmin) {
        $sql .= " WHERE n.usuari_id = :usuari_id";
        $parametres['usuari_id'] = (int) $usuari['id'];
    } else {
        $sql .= " WHERE 1 = 1";
    }

    // Filtre opcional: 0 = pendent, 1 = llegida.
    if (isset($dades['llegida'])) {
        $llegida = filter_var(
            $dades['llegida'],
            FILTER_VALIDATE_INT
        );

        if ($llegida !== 0 && $llegida !== 1) {
            return [
                'success' => false,
                'message' => 'El filtre llegida només pot ser 0 o 1.'
            ];
        }

        $sql .= " AND n.llegida = :llegida";
        $parametres['llegida'] = $llegida;
    }

    $sql .= " ORDER BY n.creat_at DESC, n.id DESC";

    try {
        $stmt = $pdo->prepare($sql);
        $stmt->execute($parametres);

        return [
            'success' => true,
            'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)
        ];
    } catch (PDOException $e) {
        error_log(
            'Error en llistar notificacions: ' . $e->getMessage()
        );

        return [
            'success' => false,
            'message' => 'Error en consultar les notificacions.'
        ];
    }
}


/**
 * Marca una notificació com a llegida.
 *
 * ADMIN pot marcar qualsevol notificació.
 * Els altres usuaris només poden marcar les seves.
 */
function marcarNotificacioLlegida(
    $pdo,
    $notificacioId,
    $usuari
) {
    $notificacioId = filter_var(
        $notificacioId,
        FILTER_VALIDATE_INT
    );

    if (!$notificacioId || $notificacioId < 1) {
        return [
            'success' => false,
            'message' => 'L’identificador de notificació no és vàlid.'
        ];
    }

    $esAdmin = esAdministradorNotificacions($usuari);

    $sql = "UPDATE notificacions
            SET llegida = 1
            WHERE id = :id";

    $parametres = [
        'id' => $notificacioId
    ];

    if (!$esAdmin) {
        $sql .= " AND usuari_id = :usuari_id";
        $parametres['usuari_id'] = (int) $usuari['id'];
    }

    $stmt = $pdo->prepare($sql);
    $stmt->execute($parametres);

    // Comprovem que la notificació existeix i que hi té accés.
    $sqlComprovar = "SELECT id, llegida
                     FROM notificacions
                     WHERE id = :id";

    $parametresComprovar = [
        'id' => $notificacioId
    ];

    if (!$esAdmin) {
        $sqlComprovar .= " AND usuari_id = :usuari_id";
        $parametresComprovar['usuari_id'] = (int) $usuari['id'];
    }

    $comprovar = $pdo->prepare($sqlComprovar);
    $comprovar->execute($parametresComprovar);

    $notificacio = $comprovar->fetch(PDO::FETCH_ASSOC);

    if (!$notificacio) {
        return [
            'success' => false,
            'message' => 'Notificació inexistent o sense permisos.'
        ];
    }

    return [
        'success' => true,
        'message' => 'Notificació marcada com a llegida.',
        'id' => (int) $notificacioId,
        'llegida' => (int) $notificacio['llegida']
    ];
}


/**
 * Marca com a llegides les notificacions accessibles.
 *
 * ADMIN: totes les notificacions.
 * Altres usuaris: només les pròpies.
 */
function marcarTotesNotificacionsLlegides($pdo, $usuari)
{
    $esAdmin = esAdministradorNotificacions($usuari);

    $sql = "UPDATE notificacions
            SET llegida = 1
            WHERE llegida = 0";

    $parametres = [];

    if (!$esAdmin) {
        $sql .= " AND usuari_id = :usuari_id";
        $parametres['usuari_id'] = (int) $usuari['id'];
    }

    $stmt = $pdo->prepare($sql);
    $stmt->execute($parametres);

    return [
        'success' => true,
        'message' => 'Notificacions pendents marcades com a llegides.',
        'actualitzades' => $stmt->rowCount()
    ];
}
