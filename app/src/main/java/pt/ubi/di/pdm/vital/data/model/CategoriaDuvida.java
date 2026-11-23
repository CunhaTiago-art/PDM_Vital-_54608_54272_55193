package pt.ubi.di.pdm.vital.data.model;

/** Categorias dos chips do ecrã "Dúvidas e ajuda". */
public enum CategoriaDuvida {
    ALARMES("Alarmes"),
    MEDICACAO("Medicação"),
    USAR_APP("Usar a app"),
    OUTRO("Outro");

    private final String etiqueta;

    CategoriaDuvida(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
