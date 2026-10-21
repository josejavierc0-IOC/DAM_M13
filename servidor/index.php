<?php

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/src/controllers/usuariController.php';
require_once __DIR__ . '/src/controllers/pistaController.php';
require_once __DIR__ . '/src/controllers/incidenciaController.php';
require_once __DIR__ . '/src/controllers/disponibilitatController.php';
require_once __DIR__ . '/src/controllers/reservaController.php';
require_once __DIR__ . '/src/controllers/tarifaController.php';
require_once __DIR__ . '/src/controllers/notificacioController.php';

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
     * GESTIÓ DE PISTES
     * =====================================================
     */
    case 'pistes':

        $metode = $_SERVER['REQUEST_METHOD'];
        $token = obtenirTokenAuthorization();

        // Per consultar les pistes cal estar autenticat.
        if ($metode === 'GET') {
            $usuari = validarToken($pdo, $token);

            if (!$usuari) {
                http_response_code(401);
                echo json_encode([
                    'success' => false,
                    'message' => 'Token no vàlid o caducat'
                ]);
                break;
            }

            echo json_encode(llistarPistes($pdo));
            break;
        }

        // La resta d'operacions només les pot fer ADMIN.
        $admin = validarAdmin($pdo, $token);

        if (!$admin) {
            http_response_code(403);
            echo json_encode([
                'success' => false,
                'message' => 'Accés denegat. Es requereix el rol ADMIN'
            ]);
            break;
        }

        if (!in_array($metode, ['POST', 'PUT', 'DELETE', 'PATCH'], true)) {
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

        
if ($metode === 'POST') {
    $resultat = crearPista($pdo, $dades);

} elseif ($metode === 'PUT') {
    $id = filter_var(
        $_GET['id'] ?? null,
        FILTER_VALIDATE_INT
    );

    if (!$id || $id < 1) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Cal indicar l’id de la pista a modificar'
        ]);
        break;
    }

    $resultat = modificarPista($pdo, $id, $dades);

} elseif ($metode === 'DELETE') {
    $id = filter_var(
        $_GET['id'] ?? null,
        FILTER_VALIDATE_INT
    );

    if (!$id || $id < 1) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Cal indicar l’id de la pista a desactivar'
        ]);
        break;
    }

    $resultat = desactivarPista($pdo, $id);

} else {
    // PATCH: reactivar una pista desactivada.
    $id = filter_var(
        $_GET['id'] ?? null,
        FILTER_VALIDATE_INT
    );

    if (!$id || $id < 1) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Cal indicar l’id de la pista a activar'
        ]);
        break;
    }

    $resultat = activarPista($pdo, $id);
}


        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat);
        break;


    /*
     * =====================================================
     * GESTIÓ D'HORARIS DE LES PISTES
     * =====================================================
     */
    case 'horaris-pista':

        $metode = $_SERVER['REQUEST_METHOD'];
        $token = obtenirTokenAuthorization();

        // Consulta d'horaris: cal estar autenticat.
        if ($metode === 'GET') {
            $usuari = validarToken($pdo, $token);

            if (!$usuari) {
                http_response_code(401);
                echo json_encode([
                    'success' => false,
                    'message' => 'Token no vàlid o caducat'
                ]);
                break;
            }

            $pistaId = filter_var(
                $_GET['pista_id'] ?? null,
                FILTER_VALIDATE_INT
            );

            if (!$pistaId || $pistaId < 1) {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Cal indicar pista_id'
                ]);
                break;
            }

            $resultat = llistarHorarisPista($pdo, $pistaId);

            if (!$resultat['success']) {
                http_response_code(404);
            }

            echo json_encode($resultat);
            break;
        }

        // Crear, modificar i desactivar horaris: només ADMIN.
        $admin = validarAdmin($pdo, $token);

        if (!$admin) {
            http_response_code(403);
            echo json_encode([
                'success' => false,
                'message' => 'Accés denegat. Es requereix el rol ADMIN'
            ]);
            break;
        }

        if (!in_array($metode, ['POST', 'PUT', 'DELETE'], true)) {
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

        if ($metode === 'POST') {
            $resultat = crearHorariPista($pdo, $dades);
        } elseif ($metode === 'PUT') {
            $id = filter_var(
                $_GET['id'] ?? null,
                FILTER_VALIDATE_INT
            );

            if (!$id || $id < 1) {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Cal indicar l’id de l’horari a modificar'
                ]);
                break;
            }

            $resultat = modificarHorariPista($pdo, $id, $dades);
        } else {
            $id = filter_var(
                $_GET['id'] ?? null,
                FILTER_VALIDATE_INT
            );

            if (!$id || $id < 1) {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Cal indicar l’id de l’horari a desactivar'
                ]);
                break;
            }

            $resultat = desactivarHorariPista($pdo, $id);
        }

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat);
        break;

        
    /*
     * =====================================================
     * GESTIÓ D'INCIDÈNCIES
     * =====================================================
     */
    case 'incidencies':

        $metode = $_SERVER['REQUEST_METHOD'];
        $token = obtenirTokenAuthorization();

        // Totes les operacions d'incidències són per a ADMIN.
        $admin = validarAdmin($pdo, $token);

        if (!$admin) {
            http_response_code(403);
            echo json_encode([
                'success' => false,
                'message' => 'Accés denegat. Es requereix el rol ADMIN'
            ]);
            break;
        }

        // GET: consultar totes les incidències.
        if ($metode === 'GET') {
            echo json_encode(llistarIncidencies($pdo));
            break;
        }

        // Només acceptem POST, PUT i PATCH per modificar dades.
        if (!in_array($metode, ['POST', 'PUT', 'PATCH'], true)) {
            http_response_code(405);
            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);
            break;
        }

        // Llegim les dades JSON del cos de la petició.
        $dades = json_decode(
            file_get_contents('php://input'),
            true
        );

        if ($metode === 'POST') {

            // POST: crear una incidència.
            $resultat = crearIncidencia(
                $pdo,
                $admin['id'],
                $dades
            );

        } else {

            // PUT i PATCH necessiten l'identificador.
            $id = filter_var(
                $_GET['id'] ?? null,
                FILTER_VALIDATE_INT
            );

            if (!$id || $id < 1) {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Cal indicar un id d’incidència vàlid'
                ]);
                break;
            }

            if ($metode === 'PUT') {

                // PUT: modificar una incidència oberta.
                $resultat = modificarIncidencia(
                    $pdo,
                    $id,
                    $dades
                );

            } else {

                // PATCH: marcar la incidència com a resolta.
                $resultat = resoldreIncidencia($pdo, $id);
            }
        }

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat);
        break;

            /*
     * =====================================================
     * CONSULTA DE DISPONIBILITAT
     * =====================================================
     */
    case 'disponibilitat':

        // Només acceptem consultes GET.
        if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
            http_response_code(405);
            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès'
            ]);
            break;
        }

        // Cal iniciar sessió per consultar disponibilitat.
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

        // La data es rep com a paràmetre de l'URL.
        $data = $_GET['data'] ?? '';

        if ($data === '') {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Cal indicar la data de consulta (AAAA-MM-DD)'
            ]);
            break;
        }

        $resultat = consultarDisponibilitat($pdo, $data);

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode(
            $resultat,
            JSON_UNESCAPED_UNICODE
        );

        break;

        
case 'reserves':
    $token = obtenirTokenAuthorization();
    $usuari = validarToken($pdo, $token);

    if (!$usuari) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Cal iniciar sessió.'
        ]);
        break;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $resultat = llistarReserves($pdo, $usuari);
        echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
        break;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $dades = json_decode(file_get_contents('php://input'), true) ?? [];
        $resultat = crearReserva($pdo, $usuari, $dades);

        if (!$resultat['success']) {
            http_response_code(400);
        }

        echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
        break;
    }

    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Mètode no permès.'
    ]);
    break;


case 'reserva':
    $token = obtenirTokenAuthorization();
    $usuari = validarToken($pdo, $token);

    if (!$usuari) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Cal iniciar sessió.'
        ]);
        break;
    }

    $reservaId = filter_input(INPUT_GET, 'id', FILTER_VALIDATE_INT);

    if (!$reservaId) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Cal indicar un id de reserva vàlid.'
        ]);
        break;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $resultat = consultarReserva($pdo, $reservaId, $usuari);

        if (!$resultat['success']) {
            http_response_code(
                str_contains($resultat['message'], 'permís') ? 403 : 404
            );
        }

        echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
        break;
    }

  

if ($_SERVER['REQUEST_METHOD'] === 'PATCH') {
    $accio = $_GET['accio'] ?? '';

    if ($accio === 'pagament') {
        $resultat = marcarReservaPagada(
            $pdo,
            $reservaId,
            $usuari
        );
    } elseif ($accio === 'modificar') {
        $dades = json_decode(
            file_get_contents('php://input'),
            true
        ) ?? [];

        $resultat = modificarReserva(
            $pdo,
            $reservaId,
            $usuari,
            $dades
        );
    } else {
        $resultat = cancelLarReserva(
            $pdo,
            $reservaId,
            $usuari
        );
    }

    if (!$resultat['success']) {
        http_response_code(
            str_contains($resultat['message'], 'permís') ? 403 : 400
        );
    }

    echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
    break;
}

    /*
     * =====================================================
     * GESTIÓ DE TARIFES
     * =====================================================
     */
    case 'tarifes':

        $metode = $_SERVER['REQUEST_METHOD'];
        $token = obtenirTokenAuthorization();

        // Cal estar autenticat per consultar les tarifes.
        $usuari = validarToken($pdo, $token);

        if (!$usuari) {
            http_response_code(401);
            echo json_encode([
                'success' => false,
                'message' => 'Cal iniciar sessió.'
            ]);
            break;
        }

        // GET: qualsevol usuari autenticat pot consultar tarifes.
        if ($metode === 'GET') {
            $resultat = llistarTarifes($pdo, $usuari);
            echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
            break;
        }

        // La resta d'operacions són exclusives de l'administrador.
        $admin = validarAdmin($pdo, $token);

        if (!$admin) {
            http_response_code(403);
            echo json_encode([
                'success' => false,
                'message' => 'Accés denegat. Es requereix el rol ADMIN.'
            ]);
            break;
        }

        // Només acceptem POST, PUT i DELETE.
        if (!in_array($metode, ['POST', 'PUT', 'DELETE'], true)) {
            http_response_code(405);
            echo json_encode([
                'success' => false,
                'message' => 'Mètode no permès.'
            ]);
            break;
        }

        // PUT i DELETE necessiten un id de tarifa vàlid.
        $id = null;

        if ($metode === 'PUT' || $metode === 'DELETE') {
            $id = filter_var(
                $_GET['id'] ?? null,
                FILTER_VALIDATE_INT
            );

            if (!$id || $id < 1) {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Cal indicar un id de tarifa vàlid.'
                ]);
                break;
            }
        }

        if ($metode === 'DELETE') {
            $resultat = desactivarTarifa($pdo, $id);
        } else {
            $dades = json_decode(
                file_get_contents('php://input'),
                true
            ) ?? [];

            if ($metode === 'POST') {
                $resultat = crearTarifa($pdo, $dades);
            } else {
                $resultat = modificarTarifa($pdo, $id, $dades);
            }
        }

        if (!$resultat['success']) {
            $missatge = $resultat['message'];

            if ($missatge === 'Tarifa no trobada.') {
                http_response_code(404);
            } else {
                http_response_code(400);
            }
        }

        echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
        break;

    case 'notificacions':
        $token = obtenirTokenAuthorization();
        $usuari = validarToken($pdo, $token);

        if (!$usuari) {
            http_response_code(401);
            echo json_encode([
                'success' => false,
                'message' => 'Cal iniciar sessió.'
            ], JSON_UNESCAPED_UNICODE);
            break;
        }

        $metode = $_SERVER['REQUEST_METHOD'];
        $accio = $_GET['accio'] ?? '';

        if ($metode === 'GET') {
            $resultat = llistarNotificacions($pdo, $usuari, $_GET);

            if (!$resultat['success']) {
                http_response_code(400);
            }

            echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
            break;
        }

        if ($metode === 'PATCH') {
            if ($accio === 'llegir_totes') {
                $resultat = marcarTotesNotificacionsLlegides(
                    $pdo,
                    $usuari
                );
            } elseif ($accio === 'llegir') {
                $notificacioId = filter_input(
                    INPUT_GET,
                    'id',
                    FILTER_VALIDATE_INT
                );

                $resultat = marcarNotificacioLlegida(
                    $pdo,
                    $notificacioId,
                    $usuari
                );
            } else {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Acció de notificacions no vàlida.'
                ], JSON_UNESCAPED_UNICODE);
                break;
            }

            if (!$resultat['success']) {
                http_response_code(
                    str_contains($resultat['message'], 'permisos') ? 403 : 400
                );
            }

            echo json_encode($resultat, JSON_UNESCAPED_UNICODE);
            break;
        }

        http_response_code(405);
        echo json_encode([
            'success' => false,
            'message' => 'Mètode no permès.'
        ], JSON_UNESCAPED_UNICODE);
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
                'restablir-contrasenya',
                'pistes',
                'horaris-pista',
                'incidencies',
                'reserves',
                'reserva',
                'tarifes'
            ]
        ]);

        break;
}