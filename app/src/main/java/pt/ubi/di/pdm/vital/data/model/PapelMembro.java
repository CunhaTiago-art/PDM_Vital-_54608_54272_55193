package pt.ubi.di.pdm.vital.data.model;

/** Papel de cada pessoa dentro da rede de apoio do idoso. */
public enum PapelMembro {
    IDOSO("Idoso"),
    FILHA("Filha"),
    FILHO("Filho"),
    NETO("Neto"),
    NETA("Neta"),
    ENFERMEIRO("Enfermeiro"),
    MEDICO("Médico"),
    CUIDADOR("Cuidador");

    private final String etiqueta;

    PapelMembro(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto a mostrar na interface (ex.: chips da Comunidade). */
    public String getEtiqueta() {
        return etiqueta;
    }
}
