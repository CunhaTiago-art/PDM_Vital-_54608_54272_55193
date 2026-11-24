package pt.ubi.di.pdm.vital.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

import pt.ubi.di.pdm.vital.data.model.PapelMembro;

/**
 * Guarda quem está a usar a app neste telemóvel e a que família pertence.
 *
 * São três coisas distintas:
 *  - o perfil escolhido no ecrã de Entrada (Idoso ou Cuidador);
 *  - o membro correspondente na base de dados local, que dá acesso às permissões;
 *  - o uid da autenticação anónima do Firebase, que identifica este telemóvel
 *    perante os outros telemóveis da mesma família.
 *
 * Isto vive em SharedPreferences e não em Room porque é configuração da aplicação,
 * não são dados do utente.
 */
public class SessionManager {

    private static final String FICHEIRO = "VitalPlusPrefs";

    // Chaves já escritas pelo ecrã de Entrada
    private static final String NOME = "utilizador_nome";
    private static final String PERFIL = "utilizador_perfil";

    // Chaves acrescentadas pela camada de dados
    private static final String MEMBRO_ID = "membro_id";
    private static final String UID_REMOTO = "uid_remoto";
    private static final String CODIGO_FAMILIA = "codigo_familia";

    /** Caracteres do código de família: sem I, O, 0 e 1, que se confundem a ler. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(FICHEIRO, Context.MODE_PRIVATE);
    }

    // ---------- Perfil escolhido na Entrada ----------

    public String getNome() {
        return prefs.getString(NOME, null);
    }

    public String getPerfil() {
        return prefs.getString(PERFIL, "Idoso");
    }

    public boolean isIdoso() {
        return "Idoso".equalsIgnoreCase(getPerfil());
    }

    /** Converte o perfil do ecrã de Entrada no papel usado na base de dados. */
    public PapelMembro getPapel() {
        return isIdoso() ? PapelMembro.IDOSO : PapelMembro.CUIDADOR;
    }

    public void guardarPerfil(String nome, String perfil) {
        prefs.edit().putString(NOME, nome).putString(PERFIL, perfil).apply();
    }

    // ---------- Membro correspondente na base de dados ----------

    /** Id do membro que representa quem está a usar o telemóvel; 0 se ainda não houver. */
    public long getMembroId() {
        return prefs.getLong(MEMBRO_ID, 0L);
    }

    public void setMembroId(long id) {
        prefs.edit().putLong(MEMBRO_ID, id).apply();
    }

    // ---------- Identidade na nuvem ----------

    /** Uid da autenticação anónima do Firebase; null enquanto não houver ligação. */
    public String getUidRemoto() {
        return prefs.getString(UID_REMOTO, null);
    }

    public void setUidRemoto(String uid) {
        prefs.edit().putString(UID_REMOTO, uid).apply();
    }

    // ---------- Código de família ----------

    /**
     * Código partilhado que junta os telemóveis da mesma família na mesma conversa.
     * Null enquanto ninguém tiver criado nem introduzido um.
     */
    public String getCodigoFamilia() {
        return prefs.getString(CODIGO_FAMILIA, null);
    }

    public void setCodigoFamilia(String codigo) {
        prefs.edit().putString(CODIGO_FAMILIA, codigo == null ? null
                : codigo.trim().toUpperCase()).apply();
    }

    public boolean temFamilia() {
        String codigo = getCodigoFamilia();
        return codigo != null && !codigo.isEmpty();
    }

    /** Gera um código novo de seis caracteres, para quem cria a família. */
    public static String gerarCodigoFamilia() {
        SecureRandom aleatorio = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Apaga a sessão sem tocar nos dados clínicos guardados no Room. */
    public void terminarSessao() {
        prefs.edit().remove(MEMBRO_ID).remove(NOME).remove(PERFIL).apply();
    }
}
