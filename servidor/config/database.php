<?php

/*
 * Connexió amb la base de dades MySQL.
 *
 * Utilitzem PDO perquè PHP pugui comunicar-se
 * amb la nostra base de dades.
 */

// Dades de connexió a MySQL.
// En XAMPP, per defecte l'usuari és "root"
// i normalment no té contrasenya.
$host = 'localhost';
$dbname = 'ioc_padel_pass';
$username = 'root';
$password = '';

try {

    // Creem la connexió amb MySQL mitjançant PDO.
    $pdo = new PDO(
        "mysql:host=$host;dbname=$dbname;charset=utf8mb4",
        $username,
        $password
    );

    // Fem que PDO ens mostri els errors
    // de la base de dades com a excepcions.
    $pdo->setAttribute(
        PDO::ATTR_ERRMODE,
        PDO::ERRMODE_EXCEPTION
    );

} catch (PDOException $e) {

    // Si no podem connectar amb la BBDD,
    // retornem un missatge en format JSON.
    header('Content-Type: application/json; charset=utf-8');

    echo json_encode([
        'success' => false,
        'message' => 'Error de connexió amb la base de dades'
    ]);

    // Aturem l'execució del programa.
    exit;
}