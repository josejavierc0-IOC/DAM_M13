
<?php

/**
 * Comprova i normalitza les dades d'una tarifa.
 * Les dates de vigència poden ser NULL.
 */
function validarDadesTarifa($dades)
{
    if (!is_array($dades)) {
        return [
            'success' => false,
            'message' => 'Les dades de la tarifa no són vàlides.'
        ];
    }

    $nom = trim($dades['nom'] ?? '');
    $preu = $dades['preu'] ?? null;
    $durada = filter_var(
        $dades['durada_minuts'] ?? null,
        FILTER_VALIDATE_INT
    );

    $dataInici = $dades['data_inici'] ?? null;
    $dataFi = $dades['data_fi'] ?? null;
    $activa = $dades['activa'] ?? 1;

    // Validem el nom.
    if ($nom === '' || mb_strlen($nom) > 80) {
        return [
            'success' => false,
            'message' => 'El nom és obligatori i no pot superar els 80 caràcters.'
        ];
    }

    // Validem el preu.
    if (
        $preu === null ||
        !is_numeric($preu) ||
        !is_finite((float) $preu) ||
        (float) $preu < 0 ||
        (float) $preu > 999999.99
    ) {
        return [
            'success' => false,
            'message' => 'El preu ha de ser un número entre 0 i 999999.99.'
        ];
    }

    // Validem la durada.
    if ($durada === false || $durada < 1 || $durada > 32767) {
        return [
            'success' => false,
            'message' => 'La durada ha de ser un nombre de minuts vàlid.'
        ];
    }

    // Les dates són opcionals, però han de tenir format AAAA-MM-DD.
    foreach ([
        'data_inici' => $dataInici,
        'data_fi' => $dataFi
    ] as $camp => $data) {
        if (
            $data !== null &&
            $data !== '' &&
            (
                !is_string($data) ||
                !preg_match('/^\d{4}-\d{2}-\d{2}$/', $data) ||
                !checkdate(
                    (int) substr($data, 5, 2),
                    (int) substr($data, 8, 2),
                    (int) substr($data, 0, 4)
                )
            )
        ) {
            return [
                'success' => false,
                'message' => "El camp $camp ha de tenir el format AAAA-MM-DD."
            ];
        }
    }

    // Si s'indiquen les dues dates, l'inici no pot ser posterior al final.
    if (
        !empty($dataInici) &&
        !empty($dataFi) &&
        $dataInici > $dataFi
    ) {
        return [
            'success' => false,
            'message' => 'La data d’inici no pot ser posterior a la data de fi.'
        ];
    }

    // activa només pot ser 0 o 1.
    if (!in_array($activa, [0, 1, '0', '1', true, false], true)) {
        return [
            'success' => false,
            'message' => 'El camp activa només pot ser 0 o 1.'
        ];
    }

    return [
        'success' => true,
        'dades' => [
            'nom' => $nom,
            'preu' => number_format((float) $preu, 2, '.', ''),
            'durada_minuts' => $durada,
            'activa' => (int) $activa,
            'data_inici' => $dataInici === '' ? null : $dataInici,
            'data_fi' => $dataFi === '' ? null : $dataFi
        ]
    ];
}


/**
 * Consulta les tarifes.
 * Els administradors veuen totes les tarifes.
 * La resta d'usuaris veuen les tarifes actives i vigents avui.
 */
function llistarTarifes($pdo, $usuari)
{
    try {
        $esAdmin = strtoupper($usuari['rol'] ?? '') === 'ADMIN';

        if ($esAdmin) {
            $sql = "
                SELECT id, nom, preu, durada_minuts, activa,
                       data_inici, data_fi
                FROM tarifes
                ORDER BY id
            ";

            $stmt = $pdo->query($sql);
        } else {
            $sql = "
                SELECT id, nom, preu, durada_minuts, activa,
                       data_inici, data_fi
                FROM tarifes
                WHERE activa = 1
                  AND (data_inici IS NULL OR data_inici <= CURDATE())
                  AND (data_fi IS NULL OR data_fi >= CURDATE())
                ORDER BY preu, durada_minuts
            ";

            $stmt = $pdo->query($sql);
        }

        return [
            'success' => true,
            'tarifes' => $stmt->fetchAll(PDO::FETCH_ASSOC)
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en consultar les tarifes.'
        ];
    }
}


/**
 * Crea una tarifa nova.
 * Només s'ha de cridar després de validar que l'usuari és ADMIN.
 */
function crearTarifa($pdo, $dades)
{
    $validacio = validarDadesTarifa($dades);

    if (!$validacio['success']) {
        return $validacio;
    }

    $t = $validacio['dades'];

    try {
        $sql = "
            INSERT INTO tarifes
                (nom, preu, durada_minuts, activa, data_inici, data_fi)
            VALUES
                (:nom, :preu, :durada_minuts, :activa, :data_inici, :data_fi)
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':nom' => $t['nom'],
            ':preu' => $t['preu'],
            ':durada_minuts' => $t['durada_minuts'],
            ':activa' => $t['activa'],
            ':data_inici' => $t['data_inici'],
            ':data_fi' => $t['data_fi']
        ]);

        return [
            'success' => true,
            'message' => 'Tarifa creada correctament.',
            'id' => (int) $pdo->lastInsertId()
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en crear la tarifa.'
        ];
    }
}


/**
 * Modifica una tarifa existent.
 * Només s'ha de cridar després de validar que l'usuari és ADMIN.
 */
function modificarTarifa($pdo, $id, $dades)
{
    $validacio = validarDadesTarifa($dades);

    if (!$validacio['success']) {
        return $validacio;
    }

    $t = $validacio['dades'];

    try {
        // Comprovem que la tarifa existeix.
        $comprovar = $pdo->prepare(
            "SELECT id FROM tarifes WHERE id = :id"
        );
        $comprovar->execute([':id' => $id]);

        if (!$comprovar->fetch(PDO::FETCH_ASSOC)) {
            return [
                'success' => false,
                'message' => 'Tarifa no trobada.'
            ];
        }

        $sql = "
            UPDATE tarifes
            SET nom = :nom,
                preu = :preu,
                durada_minuts = :durada_minuts,
                activa = :activa,
                data_inici = :data_inici,
                data_fi = :data_fi
            WHERE id = :id
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':nom' => $t['nom'],
            ':preu' => $t['preu'],
            ':durada_minuts' => $t['durada_minuts'],
            ':activa' => $t['activa'],
            ':data_inici' => $t['data_inici'],
            ':data_fi' => $t['data_fi'],
            ':id' => $id
        ]);

        return [
            'success' => true,
            'message' => 'Tarifa modificada correctament.',
            'id' => (int) $id
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en modificar la tarifa.'
        ];
    }
}


/**
 * Desactiva una tarifa sense esborrar-la de la base de dades.
 */
function desactivarTarifa($pdo, $id)
{
    try {
        $stmt = $pdo->prepare(
            "UPDATE tarifes SET activa = 0 WHERE id = :id"
        );
        $stmt->execute([':id' => $id]);

        if ($stmt->rowCount() === 0) {
            // Pot ser una tarifa existent que ja estava desactivada.
            $comprovar = $pdo->prepare(
                "SELECT id FROM tarifes WHERE id = :id"
            );
            $comprovar->execute([':id' => $id]);

            if (!$comprovar->fetch(PDO::FETCH_ASSOC)) {
                return [
                    'success' => false,
                    'message' => 'Tarifa no trobada.'
                ];
            }
        }

        return [
            'success' => true,
            'message' => 'Tarifa desactivada correctament.',
            'id' => (int) $id
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en desactivar la tarifa.'
        ];
    }
}
