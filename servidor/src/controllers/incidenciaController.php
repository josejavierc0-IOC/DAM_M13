
<?php

// Comprova que la pista existeix.
function existeixPistaIncidencia($pdo, $pistaId)
{
    $sql = "SELECT id FROM pistes WHERE id = :id";
    $stmt = $pdo->prepare($sql);
    $stmt->execute(['id' => $pistaId]);

    return $stmt->fetch(PDO::FETCH_ASSOC) !== false;
}

// Comprova que un usuari existeix.
function existeixUsuariIncidencia($pdo, $usuariId)
{
    $sql = "SELECT id FROM usuaris WHERE id = :id";
    $stmt = $pdo->prepare($sql);
    $stmt->execute(['id' => $usuariId]);

    return $stmt->fetch(PDO::FETCH_ASSOC) !== false;
}

// Consulta totes les incidències, amb el nom de la pista.
function llistarIncidencies($pdo)
{
    try {
        $sql = "SELECT
                    i.id,
                    i.pista_id,
                    p.nom AS pista,
                    i.creat_per,
                    i.descripcio,
                    i.data_inici,
                    i.data_fi,
                    i.estat,
                    i.creat_at
                FROM incidencies i
                INNER JOIN pistes p ON p.id = i.pista_id
                ORDER BY i.data_inici DESC";

        $stmt = $pdo->query($sql);

        return [
            'success' => true,
            'incidencies' => $stmt->fetchAll(PDO::FETCH_ASSOC)
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en consultar les incidències'
        ];
    }
}

// Registra una incidència.
function crearIncidencia($pdo, $usuariId, $dades)
{
    $pistaId = $dades['pista_id'] ?? null;
    $descripcio = trim($dades['descripcio'] ?? '');
    $dataInici = $dades['data_inici'] ?? '';

    if (
        !$pistaId ||
        $descripcio === '' ||
        $dataInici === ''
    ) {
        return [
            'success' => false,
            'message' => 'Cal indicar pista, descripció i data d’inici'
        ];
    }

    if (!existeixPistaIncidencia($pdo, $pistaId)) {
        return [
            'success' => false,
            'message' => 'La pista indicada no existeix'
        ];
    }

    // Validem el format de la data.
    $data = DateTime::createFromFormat('Y-m-d H:i:s', $dataInici)
        ?: DateTime::createFromFormat('Y-m-d\TH:i', $dataInici);

    if (!$data) {
        return [
            'success' => false,
            'message' => 'Format de data d’inici incorrecte'
        ];
    }

    $dataIniciSQL = $data->format('Y-m-d H:i:s');

    // La data final és opcional.
    $dataFi = $dades['data_fi'] ?? null;
    $dataFiSQL = null;

    if ($dataFi !== null && $dataFi !== '') {
        $dataFinal = DateTime::createFromFormat('Y-m-d H:i:s', $dataFi)
            ?: DateTime::createFromFormat('Y-m-d\TH:i', $dataFi);

        if (!$dataFinal) {
            return [
                'success' => false,
                'message' => 'Format de data final incorrecte'
            ];
        }

        $dataFiSQL = $dataFinal->format('Y-m-d H:i:s');

        if ($dataFiSQL <= $dataIniciSQL) {
            return [
                'success' => false,
                'message' => 'La data final ha de ser posterior a la inicial'
            ];
        }
    }

    try {
        $sql = "INSERT INTO incidencies
                    (pista_id, creat_per, descripcio, data_inici, data_fi, estat)
                VALUES
                    (:pista_id, :creat_per, :descripcio, :data_inici, :data_fi, 'OBERTA')";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            'pista_id' => $pistaId,
            'creat_per' => $usuariId,
            'descripcio' => $descripcio,
            'data_inici' => $dataIniciSQL,
            'data_fi' => $dataFiSQL
        ]);

        return [
            'success' => true,
            'message' => 'Incidència creada correctament',
            'id' => $pdo->lastInsertId()
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en crear la incidència'
        ];
    }
}

// Modifica la descripció i les dates d'una incidència.
function modificarIncidencia($pdo, $incidenciaId, $dades)
{
    $descripcio = trim($dades['descripcio'] ?? '');
    $dataInici = $dades['data_inici'] ?? '';
    $dataFi = $dades['data_fi'] ?? null;

    if ($descripcio === '' || $dataInici === '') {
        return [
            'success' => false,
            'message' => 'Cal indicar descripció i data d’inici'
        ];
    }

    $inici = DateTime::createFromFormat('Y-m-d H:i:s', $dataInici)
        ?: DateTime::createFromFormat('Y-m-d\TH:i', $dataInici);

    if (!$inici) {
        return [
            'success' => false,
            'message' => 'Format de data d’inici incorrecte'
        ];
    }

    $iniciSQL = $inici->format('Y-m-d H:i:s');
    $fiSQL = null;

    if ($dataFi !== null && $dataFi !== '') {
        $fi = DateTime::createFromFormat('Y-m-d H:i:s', $dataFi)
            ?: DateTime::createFromFormat('Y-m-d\TH:i', $dataFi);

        if (!$fi) {
            return [
                'success' => false,
                'message' => 'Format de data final incorrecte'
            ];
        }

        $fiSQL = $fi->format('Y-m-d H:i:s');

        if ($fiSQL <= $iniciSQL) {
            return [
                'success' => false,
                'message' => 'La data final ha de ser posterior a la inicial'
            ];
        }
    }

    try {
        $sql = "UPDATE incidencies
                SET descripcio = :descripcio,
                    data_inici = :data_inici,
                    data_fi = :data_fi
                WHERE id = :id AND estat = 'OBERTA'";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            'descripcio' => $descripcio,
            'data_inici' => $iniciSQL,
            'data_fi' => $fiSQL,
            'id' => $incidenciaId
        ]);

        if ($stmt->rowCount() === 0) {
            $comprovar = $pdo->prepare(
                "SELECT id FROM incidencies WHERE id = :id AND estat = 'OBERTA'"
            );
            $comprovar->execute(['id' => $incidenciaId]);

            if (!$comprovar->fetch(PDO::FETCH_ASSOC)) {
                return [
                    'success' => false,
                    'message' => 'Incidència inexistent o ja resolta'
                ];
            }
        }

        return [
            'success' => true,
            'message' => 'Incidència modificada correctament'
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en modificar la incidència'
        ];
    }
}

// Marca una incidència com a resolta.
function resoldreIncidencia($pdo, $incidenciaId)
{
    try {
        $sql = "UPDATE incidencies
                SET estat = 'RESOLTA',
                    data_fi = COALESCE(data_fi, NOW())
                WHERE id = :id AND estat = 'OBERTA'";

        $stmt = $pdo->prepare($sql);
        $stmt->execute(['id' => $incidenciaId]);

        if ($stmt->rowCount() === 0) {
            return [
                'success' => false,
                'message' => 'Incidència inexistent o ja resolta'
            ];
        }

        return [
            'success' => true,
            'message' => 'Incidència resolta correctament'
        ];
    } catch (PDOException $e) {
        return [
            'success' => false,
            'message' => 'Error en resoldre la incidència'
        ];
    }
}