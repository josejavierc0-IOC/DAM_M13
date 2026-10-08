<?php

/*
 * Controlador d'usuaris
 *
 * Aquí tenim les funcions relacionades amb:
 * - Registre d'usuaris
 * - Login
 * - Tokens de sessió
 * - Logout
 * - Control de rol ADMIN
 * - Canvi de contrasenya
 * - Recuperació de contrasenya
 */


/**
 * Registrar un nou usuari
 */
function registrarUsuari($pdo, $dades)
{
    // Comprovem que s'hagin enviat les dades obligatòries
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

        // Comprovem si l'email ja existeix
        $sql = "SELECT id FROM usuaris WHERE email = :email";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':email' => $dades['email']
        ]);

        if ($stmt->fetch()) {
            return [
                'success' => false,
                'message' => 'L\'email ja està registrat'
            ];
        }

        // La contrasenya no es guarda directament.
        // Es guarda un hash segur.
        $passwordHash = password_hash(
            $dades['password'],
            PASSWORD_DEFAULT
        );

        // Els usuaris que es registren públicament sempre són USER.
        // No permetem que algú es registri com ADMIN.
        $rolId = 1;

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

        $stmt->execute([
            ':rol_id' => $rolId,
            ':nom' => $dades['nom'],
            ':cognoms' => $dades['cognoms'],
            ':email' => $dades['email'],
            ':password_hash' => $passwordHash,
            ':telefon' => $dades['telefon'] ?? null
        ]);

        return [
            'success' => true,
            'message' => 'Usuari registrat correctament',
            'id' => $pdo->lastInsertId()
        ];

    } catch (PDOException $e) {

        return [
            'success' => false,
            'message' => 'Error en registrar l\'usuari'
        ];
    }
}


/**
 * Iniciar sessió
 */
function loginUsuari($pdo, $dades)
{
    if (
        empty($dades['email']) ||
        empty($dades['password'])
    ) {
        return [
            'success' => false,
            'message' => 'Email i contrasenya són obligatoris'
        ];
    }

    try {

        // Busquem l'usuari i el seu rol
        $sql = "
            SELECT
                u.id,
                u.nom,
                u.cognoms,
                u.email,
                u.password_hash,
                u.actiu,
                r.nom AS rol
            FROM usuaris u
            INNER JOIN rols r ON u.rol_id = r.id
            WHERE u.email = :email
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':email' => $dades['email']
        ]);

        $usuari = $stmt->fetch(PDO::FETCH_ASSOC);

        // Comprovem que l'usuari existeix
        if (!$usuari) {
            return [
                'success' => false,
                'message' => 'Email o contrasenya incorrectes'
            ];
        }

        // Comprovem que l'usuari està actiu
        if ((int)$usuari['actiu'] !== 1) {
            return [
                'success' => false,
                'message' => 'L\'usuari està desactivat'
            ];
        }

        // Comprovem la contrasenya
        if (!password_verify(
            $dades['password'],
            $usuari['password_hash']
        )) {
            return [
                'success' => false,
                'message' => 'Email o contrasenya incorrectes'
            ];
        }

        // Generem un token de sessió
        $token = bin2hex(random_bytes(32));

        // Guardem només el hash del token a la base de dades
        $tokenHash = hash('sha256', $token);

        // El token serà vàlid durant 24 hores
        $expiraAt = date(
            'Y-m-d H:i:s',
            time() + (24 * 60 * 60)
        );

        $sql = "
            INSERT INTO tokens_acces
            (
                usuari_id,
                token_hash,
                expira_at
            )
            VALUES
            (
                :usuari_id,
                :token_hash,
                :expira_at
            )
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':usuari_id' => $usuari['id'],
            ':token_hash' => $tokenHash,
            ':expira_at' => $expiraAt
        ]);

        // No enviem mai el password_hash al client
        unset($usuari['password_hash']);

        return [
            'success' => true,
            'message' => 'Login correcte',
            'token' => $token,
            'expira_at' => $expiraAt,
            'usuari' => $usuari
        ];

    } catch (PDOException $e) {

        return [
            'success' => false,
            'message' => 'Error en iniciar sessió'
        ];
    }
}


/**
 * Validar un token de sessió
 */
function validarToken($pdo, $token)
{
    if (empty($token)) {
        return false;
    }

    try {

        // Transformem el token rebut en hash
        $tokenHash = hash('sha256', $token);

        $sql = "
            SELECT
                u.id,
                u.nom,
                u.cognoms,
                u.email,
                u.actiu,
                r.nom AS rol
            FROM tokens_acces t
            INNER JOIN usuaris u ON t.usuari_id = u.id
            INNER JOIN rols r ON u.rol_id = r.id
            WHERE t.token_hash = :token_hash
            AND t.revocat_at IS NULL
            AND t.expira_at > NOW()
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':token_hash' => $tokenHash
        ]);

        $usuari = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$usuari) {
            return false;
        }

        // Comprovem que l'usuari estigui actiu
        if ((int)$usuari['actiu'] !== 1) {
            return false;
        }

        return $usuari;

    } catch (PDOException $e) {

        return false;
    }
}


/**
 * Tancar sessió
 */
function logoutUsuari($pdo, $token)
{
    if (empty($token)) {
        return [
            'success' => false,
            'message' => 'No s\'ha enviat cap token'
        ];
    }

    try {

        $tokenHash = hash('sha256', $token);

        $sql = "
            UPDATE tokens_acces
            SET revocat_at = NOW()
            WHERE token_hash = :token_hash
            AND revocat_at IS NULL
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':token_hash' => $tokenHash
        ]);

        if ($stmt->rowCount() === 0) {
            return [
                'success' => false,
                'message' => 'Token no vàlid o sessió ja tancada'
            ];
        }

        return [
            'success' => true,
            'message' => 'Sessió tancada correctament'
        ];

    } catch (PDOException $e) {

        return [
            'success' => false,
            'message' => 'Error en tancar la sessió'
        ];
    }
}


/**
 * Comprovar si l'usuari és ADMIN
 */
function validarAdmin($pdo, $token)
{
    $usuari = validarToken($pdo, $token);

    if (!$usuari) {
        return false;
    }

    if ($usuari['rol'] !== 'ADMIN') {
        return false;
    }

    return $usuari;
}


/**
 * Canviar la contrasenya d'un usuari autenticat
 */
function canviarContrasenya($pdo, $usuariId, $dades)
{
    if (
        empty($dades['password_actual']) ||
        empty($dades['password_nova'])
    ) {
        return [
            'success' => false,
            'message' => 'Cal indicar la contrasenya actual i la nova'
        ];
    }

    try {

        // Busquem la contrasenya actual de l'usuari
        $sql = "
            SELECT password_hash
            FROM usuaris
            WHERE id = :id
            AND actiu = 1
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':id' => $usuariId
        ]);

        $usuari = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$usuari) {
            return [
                'success' => false,
                'message' => 'Usuari no trobat'
            ];
        }

        // Comprovem la contrasenya actual
        if (!password_verify(
            $dades['password_actual'],
            $usuari['password_hash']
        )) {
            return [
                'success' => false,
                'message' => 'La contrasenya actual no és correcta'
            ];
        }

        // Creem el hash de la nova contrasenya
        $novaPasswordHash = password_hash(
            $dades['password_nova'],
            PASSWORD_DEFAULT
        );

        // Actualitzem la contrasenya
        $sql = "
            UPDATE usuaris
            SET password_hash = :password_hash
            WHERE id = :id
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':password_hash' => $novaPasswordHash,
            ':id' => $usuariId
        ]);

        return [
            'success' => true,
            'message' => 'Contrasenya canviada correctament'
        ];

    } catch (PDOException $e) {

        return [
            'success' => false,
            'message' => 'Error en canviar la contrasenya'
        ];
    }
}


/**
 * Sol·licitar recuperació de contrasenya
 */
function sollicitarRecuperacio($pdo, $email)
{
    if (empty($email)) {
        return [
            'success' => false,
            'message' => 'Cal indicar l\'email'
        ];
    }

    try {

        // Busquem l'usuari
        $sql = "
            SELECT id
            FROM usuaris
            WHERE email = :email
            AND actiu = 1
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':email' => $email
        ]);

        $usuari = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$usuari) {
            return [
                'success' => false,
                'message' => 'No existeix cap usuari amb aquest email'
            ];
        }

        // Generem un token de recuperació
        $token = bin2hex(random_bytes(32));

        // Guardem el hash del token
        $tokenHash = hash('sha256', $token);

        // El token serà vàlid durant 1 hora
        $expiraAt = date(
            'Y-m-d H:i:s',
            time() + (60 * 60)
        );

        $sql = "
            INSERT INTO tokens_recuperacio
            (
                usuari_id,
                token_hash,
                expira_at
            )
            VALUES
            (
                :usuari_id,
                :token_hash,
                :expira_at
            )
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':usuari_id' => $usuari['id'],
            ':token_hash' => $tokenHash,
            ':expira_at' => $expiraAt
        ]);

        /*
         * En una aplicació real enviaríem el token per email.
         *
         * Com que encara no tenim servei de correu,
         * el retornem per poder provar la funcionalitat.
         */
        return [
            'success' => true,
            'message' => 'Token de recuperació generat',
            'token' => $token,
            'expira_at' => $expiraAt
        ];

    } catch (PDOException $e) {

        return [
            'success' => false,
            'message' => 'Error en generar el token de recuperació'
        ];
    }
}


/**
 * Restablir una contrasenya mitjançant un token
 */
function restablirContrasenya($pdo, $dades)
{
    if (
        empty($dades['token']) ||
        empty($dades['password_nova'])
    ) {
        return [
            'success' => false,
            'message' => 'Cal indicar el token i la nova contrasenya'
        ];
    }

    try {

        // Hash del token rebut
        $tokenHash = hash(
            'sha256',
            $dades['token']
        );

        // Busquem un token vàlid
        $sql = "
            SELECT
                id,
                usuari_id
            FROM tokens_recuperacio
            WHERE token_hash = :token_hash
            AND utilitzat_at IS NULL
            AND expira_at > NOW()
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':token_hash' => $tokenHash
        ]);

        $token = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$token) {
            return [
                'success' => false,
                'message' => 'Token no vàlid o caducat'
            ];
        }

        // Generem el hash de la nova contrasenya
        $passwordHash = password_hash(
            $dades['password_nova'],
            PASSWORD_DEFAULT
        );

        // Actualitzem la contrasenya
        $sql = "
            UPDATE usuaris
            SET password_hash = :password_hash
            WHERE id = :usuari_id
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':password_hash' => $passwordHash,
            ':usuari_id' => $token['usuari_id']
        ]);

        // Marquem el token com a utilitzat
        $sql = "
            UPDATE tokens_recuperacio
            SET utilitzat_at = NOW()
            WHERE id = :id
        ";

        $stmt = $pdo->prepare($sql);

        $stmt->execute([
            ':id' => $token['id']
        ]);

        return [
            'success' => true,
            'message' => 'Contrasenya restablerta correctament'
        ];

    } catch (PDOException $e) {

        return [
            'success' => false,
            'message' => 'Error en restablir la contrasenya'
        ];
    }
}