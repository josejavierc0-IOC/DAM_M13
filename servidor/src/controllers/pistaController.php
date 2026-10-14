
<?php

// Comprova si una pista existeix.
function existeixPista($pdo, $pistaId)
{
    $sql = "SELECT id FROM pistes WHERE id = :id LIMIT 1";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':id' => $pistaId]);

    return $stmt->fetch(PDO::FETCH_ASSOC) !== false;
}


// Consulta totes les pistes.
function llistarPistes($pdo)
{
    try {
        $sql = "
            SELECT id, nom, tipus, descripcio, activa
            FROM pistes
            ORDER BY id
        ";

        $stmt = $pdo->query($sql);

        return [
            'success' => true,
            'pistes' => $stmt->fetchAll(PDO::FETCH_ASSOC)
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en consultar les pistes'
        ];
    }
}


// Dona d'alta una pista.
function crearPista($pdo, $dades)
{
    if (!is_array($dades) || empty(trim($dades['nom'] ?? ''))) {
        return [
            'success' => false,
            'message' => 'El nom de la pista és obligatori'
        ];
    }

    try {
        $sql = "
            INSERT INTO pistes (nom, tipus, descripcio, activa)
            VALUES (:nom, :tipus, :descripcio, 1)
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':nom' => trim($dades['nom']),
            ':tipus' => trim($dades['tipus'] ?? '') ?: null,
            ':descripcio' => trim($dades['descripcio'] ?? '') ?: null
        ]);

        return [
            'success' => true,
            'message' => 'Pista creada correctament',
            'id' => $pdo->lastInsertId()
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en crear la pista'
        ];
    }
}


// Modifica les dades d'una pista.
function modificarPista($pdo, $pistaId, $dades)
{
    if (!is_array($dades) || empty(trim($dades['nom'] ?? ''))) {
        return [
            'success' => false,
            'message' => 'El nom de la pista és obligatori'
        ];
    }

    try {
        if (!existeixPista($pdo, $pistaId)) {
            return [
                'success' => false,
                'message' => 'Pista no trobada'
            ];
        }

        $sql = "
            UPDATE pistes
            SET nom = :nom,
                tipus = :tipus,
                descripcio = :descripcio
            WHERE id = :id
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':nom' => trim($dades['nom']),
            ':tipus' => trim($dades['tipus'] ?? '') ?: null,
            ':descripcio' => trim($dades['descripcio'] ?? '') ?: null,
            ':id' => $pistaId
        ]);

        return [
            'success' => true,
            'message' => 'Pista modificada correctament'
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en modificar la pista'
        ];
    }
}


// Dona de baixa una pista sense esborrar-la de la base de dades.
function desactivarPista($pdo, $pistaId)
{
    try {
        $sql = "UPDATE pistes SET activa = 0 WHERE id = :id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':id' => $pistaId]);

        if ($stmt->rowCount() === 0 && !existeixPista($pdo, $pistaId)) {
            return [
                'success' => false,
                'message' => 'Pista no trobada'
            ];
        }

        return [
            'success' => true,
            'message' => 'Pista desactivada correctament'
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en desactivar la pista'
        ];
    }
}


// Consulta els horaris d'una pista.
function llistarHorarisPista($pdo, $pistaId)
{
    try {
        if (!existeixPista($pdo, $pistaId)) {
            return [
                'success' => false,
                'message' => 'Pista no trobada'
            ];
        }

        $sql = "
            SELECT id, pista_id, dia_setmana,
                   hora_inici, hora_fi, actiu
            FROM horaris_pista
            WHERE pista_id = :pista_id
            ORDER BY dia_setmana, hora_inici
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([':pista_id' => $pistaId]);

        return [
            'success' => true,
            'horaris' => $stmt->fetchAll(PDO::FETCH_ASSOC)
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en consultar els horaris'
        ];
    }
}


// Comprova les dades bàsiques d'un horari.
function validarDadesHorari($dades)
{
    if (!is_array($dades)) {
        return false;
    }

    $dia = filter_var(
        $dades['dia_setmana'] ?? null,
        FILTER_VALIDATE_INT
    );

    $inici = $dades['hora_inici'] ?? '';
    $fi = $dades['hora_fi'] ?? '';

    $formatHora = '/^(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/';

    if ($dia === false || $dia < 1 || $dia > 7) {
        return false;
    }

    if (
        !is_string($inici) ||
        !is_string($fi) ||
        !preg_match($formatHora, $inici) ||
        !preg_match($formatHora, $fi)
    ) {
        return false;
    }

    // Compara les hores normalitzades en format HH:MM:SS.
    if (strlen($inici) === 5) {
        $inici .= ':00';
    }

    if (strlen($fi) === 5) {
        $fi .= ':00';
    }

    return $inici < $fi;
}


// Crea un horari per a una pista.
function crearHorariPista($pdo, $dades)
{
    if (
        !is_array($dades) ||
        !isset($dades['pista_id']) ||
        !existeixPista($pdo, $dades['pista_id'])
    ) {
        return [
            'success' => false,
            'message' => 'Cal indicar una pista existent'
        ];
    }

    if (!validarDadesHorari($dades)) {
        return [
            'success' => false,
            'message' => 'Les dades de l’horari no són vàlides'
        ];
    }

    try {
        $sql = "
            INSERT INTO horaris_pista
                (pista_id, dia_setmana, hora_inici, hora_fi, actiu)
            VALUES
                (:pista_id, :dia, :inici, :fi, 1)
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':pista_id' => $dades['pista_id'],
            ':dia' => $dades['dia_setmana'],
            ':inici' => $dades['hora_inici'],
            ':fi' => $dades['hora_fi']
        ]);

        return [
            'success' => true,
            'message' => 'Horari creat correctament',
            'id' => $pdo->lastInsertId()
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en crear l’horari'
        ];
    }
}


// Modifica un horari existent.
function modificarHorariPista($pdo, $horariId, $dades)
{
    if (!validarDadesHorari($dades)) {
        return [
            'success' => false,
            'message' => 'Les dades de l’horari no són vàlides'
        ];
    }

    try {
        if (
            !isset($dades['pista_id']) ||
            !existeixPista($pdo, $dades['pista_id'])
        ) {
            return [
                'success' => false,
                'message' => 'Cal indicar una pista existent'
            ];
        }

        $sql = "
            UPDATE horaris_pista
            SET pista_id = :pista_id,
                dia_setmana = :dia,
                hora_inici = :inici,
                hora_fi = :fi
            WHERE id = :id
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':pista_id' => $dades['pista_id'],
            ':dia' => $dades['dia_setmana'],
            ':inici' => $dades['hora_inici'],
            ':fi' => $dades['hora_fi'],
            ':id' => $horariId
        ]);

        if ($stmt->rowCount() === 0) {
            $comprovar = $pdo->prepare(
                "SELECT id FROM horaris_pista WHERE id = :id"
            );
            $comprovar->execute([':id' => $horariId]);

            if (!$comprovar->fetch(PDO::FETCH_ASSOC)) {
                return [
                    'success' => false,
                    'message' => 'Horari no trobat'
                ];
            }
        }

        return [
            'success' => true,
            'message' => 'Horari modificat correctament'
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en modificar l’horari'
        ];
    }
}


// Desactiva un horari sense eliminar-lo.
function desactivarHorariPista($pdo, $horariId)
{
    try {
        $sql = "
            UPDATE horaris_pista
            SET actiu = 0
            WHERE id = :id
        ";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([':id' => $horariId]);

        if ($stmt->rowCount() === 0) {
            $comprovar = $pdo->prepare(
                "SELECT id FROM horaris_pista WHERE id = :id"
            );
            $comprovar->execute([':id' => $horariId]);

            if (!$comprovar->fetch(PDO::FETCH_ASSOC)) {
                return [
                    'success' => false,
                    'message' => 'Horari no trobat'
                ];
            }
        }

        return [
            'success' => true,
            'message' => 'Horari desactivat correctament'
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en desactivar l’horari'
        ];
    }
}