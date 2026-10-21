
package dam.clubpadel.escriptori;

public class Sessio {

    private static String token;
    private static String nomUsuari;
    private static String email;
    private static String rol;

    private Sessio() {
    }

    public static void iniciar(
            String tokenNou,
            String nomNou,
            String emailNou,
            String rolNou
    ) {
        token = tokenNou;
        nomUsuari = nomNou;
        email = emailNou;
        rol = rolNou;
    }

    public static String getToken() {
        return token;
    }

    public static String getNomUsuari() {
        return nomUsuari;
    }

    public static String getEmail() {
        return email;
    }

    public static String getRol() {
        return rol;
    }

    public static boolean esAdmin() {
        return rol != null && rol.equalsIgnoreCase("ADMIN");
    }

    public static void tancar() {
        token = null;
        nomUsuari = null;
        email = null;
        rol = null;
    }
}
