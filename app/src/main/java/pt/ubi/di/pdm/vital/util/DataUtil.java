package pt.ubi.di.pdm.vital.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Conversões entre milissegundos (como as datas são guardadas na base de dados)
 * e as datas/horas usadas na interface.
 */
public final class DataUtil {

    private static final Locale PT = new Locale("pt", "PT");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", PT);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT);
    private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("d MMM", PT);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEE, d MMM", PT);

    private DataUtil() {
        // classe utilitária, não se instancia
    }

    public static LocalDateTime paraDataHora(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static long paraMillis(LocalDateTime dataHora) {
        return dataHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** "14:00" */
    public static String formatarHora(long millis) {
        return paraDataHora(millis).format(HORA);
    }

    /** "27/11/2026" */
    public static String formatarData(long millis) {
        return paraDataHora(millis).format(DATA);
    }

    /** "8 dez" */
    public static String formatarDataCurta(long millis) {
        return paraDataHora(millis).format(DATA_CURTA);
    }

    /** "Sex, 27 nov" */
    public static String formatarDiaSemana(long millis) {
        return paraDataHora(millis).format(DIA_SEMANA);
    }

    /** Converte minutos desde a meia-noite em "08:00". */
    public static String formatarMinutos(int minutosDoDia) {
        return String.format(PT, "%02d:%02d", minutosDoDia / 60, minutosDoDia % 60);
    }

    /** Primeiro milissegundo de hoje. */
    public static long inicioDeHoje() {
        return inicioDoDia(System.currentTimeMillis());
    }

    /** Último milissegundo de hoje. */
    public static long fimDeHoje() {
        return fimDoDia(System.currentTimeMillis());
    }

    public static long inicioDoDia(long millis) {
        return paraMillis(paraDataHora(millis).toLocalDate().atStartOfDay());
    }

    public static long fimDoDia(long millis) {
        return inicioDoDia(millis) + 86_400_000L - 1;
    }

    /** Primeiro milissegundo da segunda-feira da semana em curso. */
    public static long inicioDaSemana() {
        LocalDate hoje = LocalDate.now();
        LocalDate segunda = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        return paraMillis(segunda.atStartOfDay());
    }

    /** Último milissegundo do domingo da semana em curso. */
    public static long fimDaSemana() {
        return inicioDaSemana() + 7 * 86_400_000L - 1;
    }

    /** Junta uma data e uma hora em minutos desde a meia-noite. */
    public static long combinar(LocalDate dia, int minutosDoDia) {
        return paraMillis(dia.atStartOfDay().plusMinutes(minutosDoDia));
    }
}
