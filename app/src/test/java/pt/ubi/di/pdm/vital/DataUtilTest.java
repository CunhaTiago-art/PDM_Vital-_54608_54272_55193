package pt.ubi.di.pdm.vital;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pt.ubi.di.pdm.vital.util.DataUtil;

/**
 * Testes às conversões de datas.
 *
 * Todas as datas da base de dados são milissegundos desde 1970. Os limites dos
 * dias e das semanas decidem o que entra em "as tomas de hoje" e no resumo
 * semanal, por isso um erro de um milissegundo faz uma toma aparecer no dia errado.
 */
public class DataUtilTest {

    @Test
    public void formatarMinutosDoDia() {
        assertEquals("00:00", DataUtil.formatarMinutos(0));
        assertEquals("08:00", DataUtil.formatarMinutos(8 * 60));
        assertEquals("14:30", DataUtil.formatarMinutos(14 * 60 + 30));
        assertEquals("23:59", DataUtil.formatarMinutos(23 * 60 + 59));
    }

    @Test
    public void converterParaMillisEDeVolta() {
        LocalDateTime momento = LocalDateTime.of(2026, 11, 26, 14, 30);
        long millis = DataUtil.paraMillis(momento);
        assertEquals(momento, DataUtil.paraDataHora(millis));
    }

    @Test
    public void inicioDoDiaEMeiaNoite() {
        long millis = DataUtil.paraMillis(LocalDateTime.of(2026, 11, 26, 17, 45));
        LocalDateTime inicio = DataUtil.paraDataHora(DataUtil.inicioDoDia(millis));

        assertEquals(0, inicio.getHour());
        assertEquals(0, inicio.getMinute());
        assertEquals(26, inicio.getDayOfMonth());
    }

    @Test
    public void fimDoDiaEOUltimoMilissegundo() {
        long millis = DataUtil.paraMillis(LocalDateTime.of(2026, 11, 26, 17, 45));
        long inicio = DataUtil.inicioDoDia(millis);
        long fim = DataUtil.fimDoDia(millis);

        // Um dia completo menos um milissegundo: sem isto, a meia-noite
        // seguinte entrava nas tomas de hoje e também nas de amanhã.
        assertEquals(86_400_000L - 1, fim - inicio);
        assertEquals(26, DataUtil.paraDataHora(fim).getDayOfMonth());
    }

    @Test
    public void umMomentoDoDiaEstaEntreOInicioEOFim() {
        long millis = DataUtil.paraMillis(LocalDateTime.of(2026, 11, 26, 8, 0));
        assertTrue(millis >= DataUtil.inicioDoDia(millis));
        assertTrue(millis <= DataUtil.fimDoDia(millis));
    }

    @Test
    public void aSemanaComecaASegundaFeira() {
        LocalDateTime inicio = DataUtil.paraDataHora(DataUtil.inicioDaSemana());
        assertEquals(DayOfWeek.MONDAY, inicio.getDayOfWeek());
        assertEquals(0, inicio.getHour());
    }

    @Test
    public void aSemanaDuraSeteDias() {
        long duracao = DataUtil.fimDaSemana() - DataUtil.inicioDaSemana();
        assertEquals(7 * 86_400_000L - 1, duracao);
    }

    @Test
    public void semanaAnteriorFicaSeteDiasAtras() {
        long diferenca = DataUtil.inicioDaSemana(0) - DataUtil.inicioDaSemana(-1);
        assertEquals(7 * 86_400_000L, diferenca);
    }

    @Test
    public void hojeEstaDentroDaSemanaAtual() {
        long agora = System.currentTimeMillis();
        assertTrue(agora >= DataUtil.inicioDaSemana());
        assertTrue(agora <= DataUtil.fimDaSemana());
    }

    @Test
    public void combinarDataComHoraEmMinutos() {
        LocalDate dia = LocalDate.of(2026, 11, 26);
        LocalDateTime resultado = DataUtil.paraDataHora(DataUtil.combinar(dia, 14 * 60 + 30));

        assertEquals(dia, resultado.toLocalDate());
        assertEquals(14, resultado.getHour());
        assertEquals(30, resultado.getMinute());
    }
}
