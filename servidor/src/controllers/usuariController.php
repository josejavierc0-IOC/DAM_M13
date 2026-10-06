<?php

/*
 * Funció per registrar un nou usuari.
 *
 * Rep les dades que envia el client i les guarda
 * a la base de dades.
 */
function registrarUsuari($pdo, $dades)
{
    // Comprovem que les dades obligatòries existeixen.
    if (
        empty($dades['nom']) ||
        empty($dades['cognoms']) ||
        empty($dades['email']) ||
        empty($dades['password'])
    ) {
        return [
            'success' => false,
            'message' => 'Falten dades obligatòries'
        ];
    }

    try {

        /*
         * Comprovem si ja existeix un usuari
         * amb aquest correu electrònic.
         */
        $sql = "SELECT id FROM usuaris WHERE email = :email";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':email' => $dades['email']
        ]);

        if ($stmt->fetch()) {
            return [
                'success' => false,
                'message' => 'Aquest email ja està registrat'
            ];
        }

        /*
         * Convertim la contrasenya en un hash segur.
         *
         * IMPORTANT:
         * Mai guardem la contrasenya directament a la BBDD.
         */
        $passwordHash = password_hash(
            $dades['password'],
            PASSWORD_DEFAULT
        );

        /*
         * Els usuaris que es registren des de l'aplicació
         * seran sempre USER.
         *
         * El rol ADMIN no es podrà escollir des del registre.
         */
        $rolId = 1;

        /*
         * Preparem la consulta INSERT.
         */
        $sql = "
            INSERT INTO usuaris
            (
                rol_id,
                nom,
                cognoms,
                email,
                password_hash,
                telefon
            )
            VALUES
            (
                :rol_id,
                :nom,
                :cognoms,
                :email,
                :password_hash,
                :telefon
            )
        ";

        $stmt = $pdo->prepare($sql);

        /*
         * Executem l'INSERT amb les dades de l'usuari.
         */
        $stmt->execute([
            ':rol_id' => $rolId,
            ':nom' => $dades['nom'],
            ':cognoms' => $dades['cognoms'],
            ':email' => $dades['email'],
            ':password_hash' => $passwordHash,
            ':telefon' => $dades['telefon'] ?? null
        ]);

        /*
         * Si arribem aquí, l'usuari s'ha creat correctament.
         */
        return [
            'success' => true,
            'message' => 'Usuari registrat correctament',
            'id' => $pdo->lastInsertId()
        ];

    } catch (PDOException $e) {

        /*
         * Si MySQL dona algun error, el retornem en JSON.
         *
         * Això ens permet saber què està fallant
         * sense que l'API quedi en blanc.
         */
        return [
            'success' => false,
            'message' => 'Error en registrar l\'usuari',
            'error' => $e->getMessage()
        ];
    }
}