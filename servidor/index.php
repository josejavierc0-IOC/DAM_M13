<?php

/*
 * API IOC Padel Pass
 *
 * Punt d'entrada principal de l'API.
 *
 * Aquí rebem les peticions del client i decidim
 * quina funció del servidor hem d'executar.
 */


/*
 * Carreguem la connexió amb la base de dades.
 */
require_once __DIR__ . '/config/database.php';


/*
 * Carreguem el controlador d'usuaris.
 */
require_once __DIR__ . '/src/controllers/usuariController.php';


/*
 * Indiquem que totes les respostes de l'API
 * seran en format JSON.
 */
header('Content-Type: application/json; charset=utf-8');


/*
 * Llegim el paràmetre "resource" de la URL.
 *
 * Exemple:
 * http://localhost/DAM_M13/servidor/?resource=usuaris
 */
$resource = $_GET['resource'] ?? '';


/*
 * Segons el recurs sol·licitat,
 * decidim quina operació executar.
 */
switch ($resource) {


    /*
     * =====================================================
     * RECURS: USUARIS
     * =====================================================
     */
    case 'usuaris':


        /*
         * -------------------------------------------------
         * GET / usuaris
         * -------------------------------------------------
         *
         * Retorna tots els usuaris.
         *
         * Exemple:
         * http://localhost/DAM_M13/servidor/?resource=usuaris
         */
        if ($_SERVER['REQUEST_METHOD'] === 'GET') {

            $usuaris = obtenirUsuaris($pdo);

            echo json_encode([
                'success' => true,
                'data' => $usuaris
            ]);

            break;
        }


        /*
         * -------------------------------------------------
         * POST / usuaris
         * -------------------------------------------------
         *
         * Registra un usuari nou.
         *
         * El client envia les dades en format JSON.
         */
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {


            /*
             * Llegim el JSON que envia el client.
             */
            $json = file_get_contents('php://input');


            /*
             * Convertim el JSON en un array PHP.
             */
            $dades = json_decode($json, true);


            /*
             * Comprovem que el JSON sigui correcte.
             */
            if ($dades === null) {

                echo json_encode([
                    'success' => false,
                    'message' => 'El JSON enviat no és correcte'
                ]);

                break;
            }


            /*
             * Cridem la funció que registra l'usuari
             * a la base de dades.
             */
            $resultat = registrarUsuari($pdo, $dades);


            /*
             * Retornem el resultat al client
             * en format JSON.
             */
            echo json_encode($resultat);

            break;
        }


        /*
         * Si arribem aquí vol dir que hem utilitzat
         * un mètode HTTP que encara no suportem.
         */
        echo json_encode([
            'success' => false,
            'message' => 'Mètode no permès'
        ]);

        break;



    /*
     * =====================================================
     * RECURS: UN USUARI
     * =====================================================
     */
    case 'usuari':


        /*
         * Comprovem si ens han passat un ID.
         *
         * Exemple:
         * ?resource=usuari&id=1
         */
        $id = $_GET['id'] ?? null;


        /*
         * Si no hi ha ID, retornem un error.
         */
        if ($id === null) {

            echo json_encode([
                'success' => false,
                'message' => 'Cal indicar l\'ID de l\'usuari'
            ]);

            break;
        }


        /*
         * Busquem l'usuari a la base de dades.
         */
        $usuari = obtenirUsuari($pdo, $id);


        /*
         * Si no existeix l'usuari,
         * retornem un missatge d'error.
         */
        if ($usuari === false) {

            echo json_encode([
                'success' => false,
                'message' => 'Usuari no trobat'
            ]);

            break;
        }


        /*
         * Retornem les dades de l'usuari.
         */
        echo json_encode([
            'success' => true,
            'data' => $usuari
        ]);

        break;



    /*
     * =====================================================
     * RECURS PER DEFECTE
     * =====================================================
     *
     * Si no indiquem cap resource,
     * mostrem informació bàsica de l'API.
     */
    default:

        echo json_encode([
            'success' => true,
            'message' => 'API IOC Padel Pass funcionant',
            'available_resources' => [
                'usuaris',
                'usuari'
            ]
        ]);

        break;
}