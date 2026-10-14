<?php

/*
 * CONTROLADOR DE DISPONIBILITAT
 *
 * Calcula les franges disponibles d'una data concreta.
 *
 * Té en compte:
 * - Pistes actives.
 * - Horaris actius.
 * - Tarifes actives i aplicables a la data.
 * - Reserves confirmades.
 * - Incidències obertes.
 *
 * Les durades vàlides són de 60 minuts o més,
 * en increments de 30 minuts.
 *
 * El preu de la tarifa correspon a una hora.
 */


/**
 * Consulta les franges disponibles d'una data.
 */
function consultarDisponibilitat($pdo, $data)
{
    // Validem el format de la data.
    $dataObjecte = DateTime::createFromFormat('!Y-m-d', (string) $data);

    if (
        !$dataObjecte ||
        $dataObjecte->format('Y-m-d') !== $data
    ) {
        return [
            'success' => false,
            'message' => 'La data ha de tenir el format AAAA-MM-DD'
        ];
    }

    // No permetem consultar dates passades.
    if ($data < date('Y-m-d')) {
        return [
            'success' => false,
            'message' => 'No es pot consultar una data passada'
        ];
    }

    // PHP considera dilluns com a dia 1 i diumenge com a dia 7.
    $diaSetmana = (int) $dataObjecte->format('N');

    try {

        /*
         * 1. Obtenim les pistes amb horaris actius
         * per al dia de la setmana seleccionat.
         */
        $sqlHoraris = "
            SELECT
                p.id AS pista_id,
                p.nom AS pista,
                hp.hora_inici,
                hp.hora_fi
            FROM pistes p
            INNER JOIN horaris_pista hp
                ON hp.pista_id = p.id
            WHERE p.activa = 1
              AND hp.actiu = 1
              AND hp.dia_setmana = :dia_setmana
            ORDER BY p.id, hp.hora_inici
        ";

        $stmtHoraris = $pdo->prepare($sqlHoraris);

        $stmtHoraris->execute([
            ':dia_setmana' => $diaSetmana
        ]);

        $horaris = $stmtHoraris->fetchAll(PDO::FETCH_ASSOC);


        /*
         * 2. Obtenim les tarifes actives aplicables
         * a la data seleccionada.
         *
         * El preu guardat correspon a una hora.
         */
        $sqlTarifes = "
            SELECT
                id,
                nom,
                preu,
                durada_minuts
            FROM tarifes
            WHERE activa = 1
              AND (
                    data_inici IS NULL
                    OR data_inici <= :data_inici
              )
              AND (
                    data_fi IS NULL
                    OR data_fi >= :data_fi
              )
              AND durada_minuts >= 60
              AND MOD(durada_minuts, 30) = 0
            ORDER BY durada_minuts, id
        ";

        $stmtTarifes = $pdo->prepare($sqlTarifes);

        $stmtTarifes->execute([
            ':data_inici' => $data,
            ':data_fi' => $data
        ]);

        $tarifes = $stmtTarifes->fetchAll(PDO::FETCH_ASSOC);


        /*
         * 3. Obtenim les reserves confirmades del dia.
         * Les reserves cancel·lades no bloquegen la pista.
         */
        $sqlReserves = "
            SELECT
                pista_id,
                hora_inici,
                hora_fi
            FROM reserves
            WHERE data_reserva = :data
              AND estat = 'CONFIRMADA'
        ";

        $stmtReserves = $pdo->prepare($sqlReserves);

        $stmtReserves->execute([
            ':data' => $data
        ]);

        $reserves = $stmtReserves->fetchAll(PDO::FETCH_ASSOC);


        /*
         * 4. Obtenim les incidències obertes que afecten
         * el dia consultat.
         *
         * Si data_fi és NULL, la incidència continua oberta.
         */
        $sqlIncidencies = "
            SELECT
                pista_id,
                data_inici,
                data_fi
            FROM incidencies
            WHERE estat = 'OBERTA'
              AND data_inici < :final_dia
              AND (
                    data_fi IS NULL
                    OR data_fi > :inici_dia
              )
        ";

        $stmtIncidencies = $pdo->prepare($sqlIncidencies);

        $stmtIncidencies->execute([
            ':final_dia' => $data . ' 23:59:59',
            ':inici_dia' => $data . ' 00:00:00'
        ]);

        $incidencies = $stmtIncidencies->fetchAll(PDO::FETCH_ASSOC);


        /*
         * 5. Calculem les franges disponibles.
         */
        $frangesDisponibles = [];
        $ara = time();

        foreach ($horaris as $horari) {

            $pistaId = (int) $horari['pista_id'];

            $iniciHorari = strtotime(
                $data . ' ' . $horari['hora_inici']
            );

            $fiHorari = strtotime(
                $data . ' ' . $horari['hora_fi']
            );

            // Ignorem horaris incorrectes.
            if (
                $iniciHorari === false ||
                $fiHorari === false ||
                $fiHorari <= $iniciHorari
            ) {
                continue;
            }

            // Provem les durades definides a les tarifes actives.
            foreach ($tarifes as $tarifa) {

                $durada = (int) $tarifa['durada_minuts'];

                // Comprovació addicional de la durada.
                if ($durada < 60 || $durada % 30 !== 0) {
                    continue;
                }

                // Els inicis possibles avancen en intervals de 30 minuts.
                for (
                    $inici = $iniciHorari;
                    $inici + ($durada * 60) <= $fiHorari;
                    $inici += 30 * 60
                ) {

                    $fi = $inici + ($durada * 60);

                    // No oferim hores que ja han passat.
                    if ($inici <= $ara) {
                        continue;
                    }


                    /*
                     * 6. Comprovem si se solapa amb una reserva.
                     */
                    $ocupadaPerReserva = false;

                    foreach ($reserves as $reserva) {

                        if (
                            (int) $reserva['pista_id'] !== $pistaId
                        ) {
                            continue;
                        }

                        $iniciReserva = strtotime(
                            $data . ' ' . $reserva['hora_inici']
                        );

                        $fiReserva = strtotime(
                            $data . ' ' . $reserva['hora_fi']
                        );

                        // Hi ha solapament si els intervals coincideixen.
                        if (
                            $iniciReserva < $fi &&
                            $fiReserva > $inici
                        ) {
                            $ocupadaPerReserva = true;
                            break;
                        }
                    }

                    if ($ocupadaPerReserva) {
                        continue;
                    }


                    /*
                     * 7. Comprovem si se solapa amb una incidència oberta.
                     */
                    $ocupadaPerIncidencia = false;

                    foreach ($incidencies as $incidencia) {

                        if (
                            (int) $incidencia['pista_id'] !== $pistaId
                        ) {
                            continue;
                        }

                        $iniciIncidencia = strtotime(
                            $incidencia['data_inici']
                        );

                        $fiIncidencia = $incidencia['data_fi'] !== null
                            ? strtotime($incidencia['data_fi'])
                            : null;

                        if (
                            $iniciIncidencia < $fi &&
                            (
                                $fiIncidencia === null ||
                                $fiIncidencia > $inici
                            )
                        ) {
                            $ocupadaPerIncidencia = true;
                            break;
                        }
                    }

                    if ($ocupadaPerIncidencia) {
                        continue;
                    }


                    /*
                     * 8. Calculem el preu total.
                     *
                     * Exemple:
                     * 20 euros/hora durant 90 minuts = 30 euros.
                     */
                    $preuHora = (float) $tarifa['preu'];

                    $preuTotal = round(
                        $preuHora * $durada / 60,
                        2
                    );


                    /*
                     * 9. Afegim la franja a la resposta.
                     */
                    $frangesDisponibles[] = [
                        'pista_id' => $pistaId,
                        'pista' => $horari['pista'],
                        'data' => $data,
                        'hora_inici' => date('H:i:s', $inici),
                        'hora_fi' => date('H:i:s', $fi),
                        'tarifa_id' => (int) $tarifa['id'],
                        'tarifa' => $tarifa['nom'],
                        'durada_minuts' => $durada,
                        'preu_hora' => number_format(
                            $preuHora,
                            2,
                            '.',
                            ''
                        ),
                        'preu_total' => number_format(
                            $preuTotal,
                            2,
                            '.',
                            ''
                        )
                    ];
                }
            }
        }


        /*
         * 10. Retornem les franges en format JSON.
         */
        return [
            'success' => true,
            'data' => $data,
            'total' => count($frangesDisponibles),
            'franges_disponibles' => $frangesDisponibles
        ];

    } catch (PDOException $e) {

        // No exposem els detalls interns de la base de dades.
        return [
            'success' => false,
            'message' => 'Error en consultar la disponibilitat'
        ];
    }
}