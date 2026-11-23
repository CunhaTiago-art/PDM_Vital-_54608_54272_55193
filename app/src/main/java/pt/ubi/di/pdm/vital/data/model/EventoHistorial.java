package pt.ubi.di.pdm.vital.data.model;

/**
 * Linha da lista do Historial. Junta numa só lista ordenada no tempo as tomas,
 * as consultas e as notas da rede de apoio (separadores Tudo / Tomas / Consultas).
 */
public class EventoHistorial {

    /** "TOMA", "CONSULTA" ou "NOTA" - decide o ícone a mostrar. */
    public String tipo;

    public String titulo;
    public String subtitulo;
    public long dataHora;
}
