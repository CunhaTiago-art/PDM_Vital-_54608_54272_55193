package pt.ubi.di.pdm.vital.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pt.ubi.di.pdm.vital.data.dao.ChatDao;
import pt.ubi.di.pdm.vital.data.dao.ComunidadeDao;
import pt.ubi.di.pdm.vital.data.dao.ConsultaDao;
import pt.ubi.di.pdm.vital.data.dao.MedicamentoDao;
import pt.ubi.di.pdm.vital.data.dao.MembroDao;
import pt.ubi.di.pdm.vital.data.dao.MetricaSaudeDao;
import pt.ubi.di.pdm.vital.data.dao.RegistoTomaDao;
import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.Duvida;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.MensagemChat;
import pt.ubi.di.pdm.vital.data.model.MetricaSaude;
import pt.ubi.di.pdm.vital.data.model.Nota;
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.data.model.Resposta;

/**
 * Base de dados local da aplicação (SQLite através do Room).
 * Toda a app funciona offline: não há servidor nem ligação à Internet.
 */
@Database(
        entities = {
                Membro.class,
                Medicamento.class,
                Horario.class,
                RegistoToma.class,
                MetricaSaude.class,
                Consulta.class,
                MensagemChat.class,
                Duvida.class,
                Resposta.class,
                Nota.class
        },
        version = 2,
        exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String NOME_BD = "vital.db";

    public abstract MembroDao membroDao();

    public abstract MedicamentoDao medicamentoDao();

    public abstract RegistoTomaDao registoTomaDao();

    public abstract MetricaSaudeDao metricaSaudeDao();

    public abstract ConsultaDao consultaDao();

    public abstract ChatDao chatDao();

    public abstract ComunidadeDao comunidadeDao();

    /**
     * v1 -> v2: prepara a sincronização do chat e das dúvidas com a nuvem.
     * Acrescenta as colunas novas sem apagar nada do que já está no telemóvel —
     * quem tiver a app instalada mantém o historial de tomas e as consultas.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Identidade global de cada pessoa (uid da autenticação anónima do Firebase)
            db.execSQL("ALTER TABLE `membros` ADD COLUMN `uidRemoto` TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_membros_uidRemoto` "
                    + "ON `membros` (`uidRemoto`)");

            // Mensagens de chat
            db.execSQL("ALTER TABLE `mensagens_chat` ADD COLUMN `idRemoto` TEXT");
            db.execSQL("ALTER TABLE `mensagens_chat` ADD COLUMN `sincronizado` "
                    + "INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mensagens_chat_idRemoto` "
                    + "ON `mensagens_chat` (`idRemoto`)");

            // Dúvidas
            db.execSQL("ALTER TABLE `duvidas` ADD COLUMN `idRemoto` TEXT");
            db.execSQL("ALTER TABLE `duvidas` ADD COLUMN `sincronizado` "
                    + "INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_duvidas_idRemoto` "
                    + "ON `duvidas` (`idRemoto`)");

            // Respostas
            db.execSQL("ALTER TABLE `respostas` ADD COLUMN `idRemoto` TEXT");
            db.execSQL("ALTER TABLE `respostas` ADD COLUMN `sincronizado` "
                    + "INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_respostas_idRemoto` "
                    + "ON `respostas` (`idRemoto`)");

            // Tudo o que já estava no telemóvel é historial local anterior à nuvem:
            // fica marcado como sincronizado para não ser enviado retroativamente.
            db.execSQL("UPDATE `mensagens_chat` SET `sincronizado` = 1");
            db.execSQL("UPDATE `duvidas` SET `sincronizado` = 1");
            db.execSQL("UPDATE `respostas` SET `sincronizado` = 1");
        }
    };

    /** Executor partilhado por toda a app para as escritas fora da thread principal. */
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase instancia;

    public static AppDatabase getInstance(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    NOME_BD)
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(SEED)
                            .build();
                }
            }
        }
        return instancia;
    }

    /**
     * Corre apenas uma vez, quando a base de dados é criada pela primeira vez.
     * Insere os dados de demonstração para a app arrancar com conteúdo.
     */
    private static final Callback SEED = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            EXECUTOR.execute(() -> {
                try {
                    if (instancia != null) {
                        SeedData.popular(instancia);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Falha ao inserir os dados de demonstração", e);
                }
            });
        }
    };
}
