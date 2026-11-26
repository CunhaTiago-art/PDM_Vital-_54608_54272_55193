package pt.ubi.di.pdm.vital;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;

import pt.ubi.di.pdm.vital.util.DiasSemana;

/**
 * Testes à máscara de bits dos dias da semana.
 *
 * Esta é a decisão de modelação mais arriscada da base de dados: em vez de uma
 * tabela com sete linhas por medicamento, os dias são guardados num só inteiro.
 * Poupa uma junção em todas as consultas do gerador de tomas, mas só funciona
 * se a conversão entre dia do calendário e bit estiver certa — um erro de um
 * bit faria o idoso tomar medicação no dia errado.
 */
public class DiasSemanaTest {

    @Test
    public void segundaEOPrimeiroBit() {
        assertEquals(1, DiasSemana.mascaraDe(DayOfWeek.MONDAY));
    }

    @Test
    public void domingoEOSetimoBit() {
        assertEquals(1 << 6, DiasSemana.mascaraDe(DayOfWeek.SUNDAY));
    }

    @Test
    public void todosOsDiasIncluiOsSete() {
        for (DayOfWeek dia : DayOfWeek.values()) {
            assertTrue("faltou " + dia, DiasSemana.contem(DiasSemana.TODOS, dia));
        }
    }

    @Test
    public void mascaraDeUmDiaNaoIncluiOsOutros() {
        int soSegunda = DiasSemana.SEGUNDA;
        assertTrue(DiasSemana.contem(soSegunda, DayOfWeek.MONDAY));
        assertFalse(DiasSemana.contem(soSegunda, DayOfWeek.TUESDAY));
        assertFalse(DiasSemana.contem(soSegunda, DayOfWeek.SUNDAY));
    }

    @Test
    public void cadaDiaTemUmBitDiferente() {
        // Se dois dias partilhassem bit, ligar um ligava o outro sem se perceber.
        int acumulado = 0;
        for (DayOfWeek dia : DayOfWeek.values()) {
            int bit = DiasSemana.mascaraDe(dia);
            assertEquals("bit repetido em " + dia, 0, acumulado & bit);
            acumulado |= bit;
        }
        assertEquals(DiasSemana.TODOS, acumulado);
    }

    @Test
    public void alternarLigaEDesliga() {
        int mascara = 0;
        mascara = DiasSemana.alternar(mascara, DayOfWeek.WEDNESDAY);
        assertTrue(DiasSemana.contem(mascara, DayOfWeek.WEDNESDAY));

        mascara = DiasSemana.alternar(mascara, DayOfWeek.WEDNESDAY);
        assertFalse(DiasSemana.contem(mascara, DayOfWeek.WEDNESDAY));
        assertEquals(0, mascara);
    }

    @Test
    public void descreverTodosOsDias() {
        assertEquals("Todos os dias", DiasSemana.descrever(DiasSemana.TODOS));
    }

    @Test
    public void descreverAlgunsDiasPelaOrdemCerta() {
        int mascara = DiasSemana.SEGUNDA | DiasSemana.QUARTA | DiasSemana.SEXTA;
        assertEquals("Seg, Qua, Sex", DiasSemana.descrever(mascara));
    }

    @Test
    public void descreverNenhumDia() {
        assertEquals("Nenhum dia", DiasSemana.descrever(0));
    }
}
