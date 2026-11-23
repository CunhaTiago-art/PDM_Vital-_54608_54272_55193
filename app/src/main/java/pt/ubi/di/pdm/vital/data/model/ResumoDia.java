package pt.ubi.di.pdm.vital.data.model;

/**
 * Contagem de tomas de um dia, usada nos círculos da semana no ecrã Historial
 * ("Esta semana 18 de 21 tomas").
 */
public class ResumoDia {

    /** Data no formato aaaa-MM-dd. */
    public String dia;

    public int total;
    public int tomadas;

    /** Verdadeiro quando o dia está completo (círculo preenchido no wireframe). */
    public boolean isCompleto() {
        return total > 0 && tomadas == total;
    }
}
