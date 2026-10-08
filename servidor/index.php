<?php

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/src/controllers/usuariController.php';

header('Content-Type: application/json; charset=utf-8');


// Recollim el recurs de l'URL
$resource = $_GET['resource'] ?? '';


// Funció per obtenir el token Authorization
function obtenirTokenAuthorization()
{
    $headers = [];

    // Intentem obtenir les capçaleres
    if (function_exists('getallheaders')) {
        $headers = getallheaders();
    }

    $authorization = null;

    // Busquem Authorization sense importar majúscules/minúscules
    foreach ($headers as $nom => $valor) {
        if (strtolower($nom) === 'authorization') {
            $authorization = $valor;
            break;
        }
    }

    // En alguns servidors Apache pot arribar per aquesta variable
    if (!$authorization) {
        $authorization = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? null;
    }

    // Comprovem que sigui Bearer TOKEN
    if (!$authorization) {
        return null;
    }

    if (stripos($authorization, 'Bearer ') !== 0) {
        return null;
    }

    return trim(substr($authorization, 7));
}


switch ($resource) {


    /*
     * =====================================================
     * REGISTRE I LLISTAT D'USUARIS
     * =====================================================
     */
    case 'usuaris':

        // GET -> consultar usuaris
        if ($_SERVER['REQUEST_METHOD'] === 'GET') {

            try {

                $sql = "
                    SELECT
                        u.id,
                        u.nom,
                        u.cognoms,
                        u.email,
                        u.telefon,
                        u.actiu,
                        r.nom AS rol
                    FROM usuaris u
                    INNER JOIN rols r ON u.rol_id = r.id
                    ORDER BY u.id
                ";

                $stmt = $pdo->query($sql);

                echo json_encode([
                    'success' => true,
                    'usuaris' => $stmt->fetchAll(PDO::FETCH_ASSOC)
                ]);

            } catch (PDOException $e) {

                http_response_code(500);

                echo json_encode([
                    'success' => false,
                    'message' => 'Error en consultar els usuaris'
                ]);
            }

            break;
        }


        // POST -> registrar usuari
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {

            $dades = json_decode(
                file_get_contents('php://input'),
                true
            );

            $resultat = registrarUsuari($pdo, $dades);

            echo json_encode($resultat);

            break;
        }


        http_response_code(405);

        echo json_encode([
            'success' => false,
            'message' => 'Mètode no permès'
        ]);

        break;


    /*
     * =====================================================
     * LOGIN
     * =====================================================
     */
    case 'login':

        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $dades = json_decode(
            file_get_contents('php://input'),
            true
        );

        $resultat = loginUsuari($pdo, $dades);

        echo json_encode($resultat);

        break;


    /*
     * =====================================================
     * PERFIL
     * =====================================================
     */
    case 'perfil':

        if ($_SERVER['REQUEST_METHOD'] !== 'GET') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $token = obtenirTokenAuthorization();

        $usuari = validarToken($pdo, $token);

        if (!$usuari) {

            http_response_code(401);

            echo json_encode([
                'success' => false,
                'message' => 'Token no vàlid o caducat'
            ]);

            break;
        }

        echo json_encode([
            'success' => true,
            'usuari' => $usuari
        ]);

        break;


    /*
     * =====================================================
     * CONSULTAR UN USUARI
     * =====================================================
     */
    case 'usuari':

        if ($_SERVER['REQUEST_METHOD'] !== 'GET') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $id = $_GET['id'] ?? null;

        if (!$id) {

            http_response_code(400);

            echo json_encode([
                'success' => false,
                'message' => 'Cal indicar l\'id de l\'usuari'
            ]);

            break;
        }

        try {

            $sql = "
                SELECT
                    u.id,
                    u.nom,
                    u.cognoms,
                    u.email,
                    u.telefon,
                    u.actiu,
                    r.nom AS rol
                FROM usuaris u
                INNER JOIN rols r ON u.rol_id = r.id
                WHERE u.id = :id
            ";

            $stmt = $pdo->prepare($sql);

            $stmt->execute([
                ':id' => $id
            ]);

            $usuari = $stmt->fetch(PDO::FETCH_ASSOC);

            if (!$usuari) {

                http_response_code(404);

                echo json_encode([
                    'success' => false,
                    'message' => 'Usuari no trobat'
                ]);

                break;
            }

            echo json_encode([
                'success' => true,
                'usuari' => $usuari
            ]);

        } catch (PDOException $e) {

            http_response_code(500);

            echo json_encode([
                'success' => false,
                'message' => 'Error en consultar l\'usuari'
            ]);
        }

        break;


    /*
     * =====================================================
     * LOGOUT
     * =====================================================
     */
    case 'logout':

        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $token = obtenirTokenAuthorization();

        if (!$token) {

            http_response_code(401);

            echo json_encode([
                'success' => false,
                'message' => 'Cal enviar el token'
            ]);

            break;
        }

        $resultat = logoutUsuari($pdo, $token);

        if (!$resultat['success']) {
            http_response_code(401);
        }

        echo json_encode($resultat);

        break;


    /*
     * =====================================================
     * PROVA D'ACCÉS ADMIN
     * =====================================================
     */
    case 'admin':

        if ($_SERVER['REQUEST_METHOD'] !== 'GET') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $token = obtenirTokenAuthorization();

        $usuari = validarAdmin($pdo, $token);

        if (!$usuari) {

            http_response_code(403);

            echo json_encode([
                'success' => false,
                'message' => 'Accés denegat. Es requereix el rol ADMIN'
            ]);

            break;
        }

        echo json_encode([
            'success' => true,
            'message' => 'Accés d\'administrador autoritzat',
            'usuari' => $usuari
        ]);

        break;


    /*
     * =====================================================
     * CANVIAR CONTRASENYA
     * =====================================================
     */
    case 'canviar-contrasenya':

        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        // Necessitem un usuari autenticat
        $token = obtenirTokenAuthorization();

        $usuari = validarToken($pdo, $token);

        if (!$usuari) {

            http_response_code(401);

            echo json_encode([
                'success' => false,
                'message' => 'Token no vàlid o caducat'
            ]);

            break;
        }

        $dades = json_decode(
            file_get_contents('php://input'),
            true
        );

        $resultat = canviarContrasenya(
            $pdo,
            $usuari['id'],
            $dades
        );

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat);

        break;


    /*
     * =====================================================
     * SOL·LICITAR RECUPERACIÓ
     * =====================================================
     */
    case 'recuperar-contrasenya':

        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $dades = json_decode(
            file_get_contents('php://input'),
            true
        );

        $email = $dades['email'] ?? '';

        $resultat = sollicitarRecuperacio(
            $pdo,
            $email
        );

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat);

        break;


    /*
     * =====================================================
     * RESTABLIR CONTRASENYA
     * =====================================================
     */
    case 'restablir-contrasenya':

        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {

            http_response_code(405);

            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);

            break;
        }

        $dades = json_decode(
            file_get_contents('php://input'),
            true
        );

        $resultat = restablirContrasenya(
            $pdo,
            $dades
        );

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat);

        break;


    /*
     * =====================================================
     * RECURS NO TROBAT
     * =====================================================
     */
    default:

        echo json_encode([
            'success' => true,
            'message' => 'API IOC Padel Pass',
            'resources' => [
                'usuaris',
                'login',
                'perfil',
                'usuari',
                'logout',
                'admin',
                'canviar-contrasenya',
                'recuperar-contrasenya',
                'restablir-contrasenya'
            ]
        ]);

        break;
}