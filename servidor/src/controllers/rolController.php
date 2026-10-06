<?php

/*
 * Controlador dels rols.
 *
 * Aquest fitxer s'encarrega de consultar
 * els rols que tenim guardats a la BBDD.
 */

// Carreguem la connexió amb la base de dades.
require_once __DIR__ . '/../../config/database.php';

/*
 * Fem una consulta per obtenir tots els rols.
 *
 * ORDER BY id ASC fa que apareguin
 * en l'ordre en què estan guardats.
 */
$sql = "SELECT id, nom FROM rols ORDER BY id ASC";

try {

    // Preparem la consulta.
    $stmt = $pdo->prepare($sql);

    // Executem la consulta.
    $stmt->execute();

    // Recuperem tots els resultats com arrays associatius.
    $rols = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Indiquem que la resposta serà JSON.
    header('Content-Type: application/json; charset=utf-8');

    // Retornem els rols.
    echo json_encode([
        'success' => true,
        'data' => $rols
    ]);

} catch (PDOException $e) {

    // Si hi ha un error, retornem un missatge senzill.
    header('Content-Type: application/json; charset=utf-8');

    echo json_encode([
        'success' => false,
        'message' => 'No s\'han pogut consultar els rols'
    ]);
}