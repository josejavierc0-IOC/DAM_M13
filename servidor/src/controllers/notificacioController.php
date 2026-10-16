<?php

/**
 * Crea una notificació per a un usuari.
 */
function crearNotificacio($pdo, $usuariId, $reservaId, $tipus, $titol, $missatge)
{
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
        'usuari_id' => $usuariId,
        'reserva_id' => $reservaId,
        'tipus' => $tipus,
        'titol' => $titol,
        'missatge' => $missatge
    ]);
}


/**
 * Retorna les notificacions de l'usuari autenticat.
 * Es pot filtrar per notificacions llegides o pendents.
 */
function llistarNotificacions($pdo, $usuari, $dades = [])
{
    $sql = "SELECT
                n.id,
                n.reserva_id,
                n.tipus,
                n.titol,
                n.missatge,
                n.llegida,
                n.creat_at
            FROM notificacions n
            WHERE n.usuari_id = :usuari_id";

    $parametres = [
        'usuari_id' => (int) $usuari['id']
    ];

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

    $stmt = $pdo->prepare($sql);
    $stmt->execute($parametres);

    return [
        'success' => true,
        'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ];
}


/**
 * Marca com a llegida una notificació pròpia.
 */
function marcarNotificacioLlegida($pdo, $notificacioId, $usuari)
{
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

    $stmt = $pdo->prepare(
        "UPDATE notificacions
         SET llegida = 1
         WHERE id = :id
           AND usuari_id = :usuari_id"
    );

    $stmt->execute([
        'id' => $notificacioId,
        'usuari_id' => (int) $usuari['id']
    ]);

    if ($stmt->rowCount() === 0) {
        // Diferenciem una notificació pròpia ja llegida
        // d'una notificació inexistent o d'un altre usuari.
        $comprovar = $pdo->prepare(
            "SELECT id, llegida
             FROM notificacions
             WHERE id = :id
               AND usuari_id = :usuari_id"
        );

        $comprovar->execute([
            'id' => $notificacioId,
            'usuari_id' => (int) $usuari['id']
        ]);

        $notificacio = $comprovar->fetch(PDO::FETCH_ASSOC);

        if (!$notificacio) {
            return [
                'success' => false,
                'message' => 'Notificació inexistent o sense permisos.'
            ];
        }
    }

    return [
        'success' => true,
        'message' => 'Notificació marcada com a llegida.',
        'id' => $notificacioId,
        'llegida' => 1
    ];
}


/**
 * Marca totes les notificacions pròpies com a llegides.
 */
function marcarTotesNotificacionsLlegides($pdo, $usuari)
{
    $stmt = $pdo->prepare(
        "UPDATE notificacions
         SET llegida = 1
         WHERE usuari_id = :usuari_id
           AND llegida = 0"
    );

    $stmt->execute([
        'usuari_id' => (int) $usuari['id']
    ]);

    return [
        'success' => true,
        'message' => 'Notificacions pendents marcades com a llegides.',
        'actualitzades' => $stmt->rowCount()
    ];
}