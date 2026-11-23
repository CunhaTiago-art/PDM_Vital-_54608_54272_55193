package pt.ubi.di.pdm.vital.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Os dias da semana de um medicamento são guardados num único inteiro (máscara de bits),
 * para evitar uma tabela extra só com sete valores.
 *
 * bit 0 = Segunda ... bit 6 = Domingo
 */
public final class DiasSemana {

    public static final int SEGUNDA = 1;        // 0000001
    public static final int TERCA   = 1 << 1;   // 0000010
    public static final int QUARTA  = 1 << 2;
    public static final int QUINTA  = 1 << 3;
    public static final int SEXTA   = 1 << 4;
    public static final int SABADO  = 1 << 5;
    public static final int DOMINGO = 1 << 6;   // 1000000

    /** Todos os dias - valor por defeito ao criar um medicamento. */
    public static final int TODOS = 0b1111111;

    private DiasSemana() {
        // classe utilitária, não se instancia
    }

    /** Converte um dia do calendário na respetiva máscara. */
    public static int mascaraDe(DayOfWeek dia) {
        return 1 << (dia.getValue() - 1);
    }

    /** Verdadeiro se a máscara inclui esse dia da semana. */
    public static boolean contem(int mascara, DayOfWeek dia) {
        return (mascara & mascaraDe(dia)) != 0;
    }

    /** Verdadeiro se o medicamento deve ser tomado no instante indicado. */
    public static boolean contem(int mascara, long dataHora) {
        DayOfWeek dia = Instant.ofEpochMilli(dataHora)
                .atZone(ZoneId.systemDefault())
                .getDayOfWeek();
        return contem(mascara, dia);
    }

    /** Liga ou desliga um dia na máscara e devolve o resultado. */
    public static int alternar(int mascara, DayOfWeek dia) {
        return mascara ^ mascaraDe(dia);
    }

    /** Texto para a interface: "Todos os dias" ou "Seg, Qua, Sex". */
    public static String descrever(int mascara) {
        if ((mascara & TODOS) == TODOS) {
            return "Todos os dias";
        }
        String[] nomes = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((mascara & (1 << i)) != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(nomes[i]);
            }
        }
        return sb.length() == 0 ? "Nenhum dia" : sb.toString();
    }
}
