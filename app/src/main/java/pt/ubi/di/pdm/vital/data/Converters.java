package pt.ubi.di.pdm.vital.data;

import androidx.room.TypeConverter;

import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.EstadoConsulta;
import pt.ubi.di.pdm.vital.data.model.EstadoToma;
import pt.ubi.di.pdm.vital.data.model.PapelMembro;
import pt.ubi.di.pdm.vital.data.model.TipoMensagem;
import pt.ubi.di.pdm.vital.data.model.TipoMetrica;

/**
 * O SQLite não conhece enums, por isso cada um é guardado como texto (o seu nome).
 * Guardar o nome em vez do número evita que a base de dados fique corrompida
 * se a ordem das constantes do enum mudar.
 */
public class Converters {

    @TypeConverter
    public static String dePapel(PapelMembro valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static PapelMembro paraPapel(String valor) {
        return valor == null ? null : PapelMembro.valueOf(valor);
    }

    @TypeConverter
    public static String deEstadoToma(EstadoToma valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static EstadoToma paraEstadoToma(String valor) {
        return valor == null ? null : EstadoToma.valueOf(valor);
    }

    @TypeConverter
    public static String deTipoMetrica(TipoMetrica valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static TipoMetrica paraTipoMetrica(String valor) {
        return valor == null ? null : TipoMetrica.valueOf(valor);
    }

    @TypeConverter
    public static String deTipoMensagem(TipoMensagem valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static TipoMensagem paraTipoMensagem(String valor) {
        return valor == null ? null : TipoMensagem.valueOf(valor);
    }

    @TypeConverter
    public static String deCategoria(CategoriaDuvida valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static CategoriaDuvida paraCategoria(String valor) {
        return valor == null ? null : CategoriaDuvida.valueOf(valor);
    }

    @TypeConverter
    public static String deEstadoConsulta(EstadoConsulta valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static EstadoConsulta paraEstadoConsulta(String valor) {
        return valor == null ? null : EstadoConsulta.valueOf(valor);
    }
}
