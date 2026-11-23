package pt.ubi.di.pdm.vital.data.model;

/** Métricas de saúde registadas no historial. */
public enum TipoMetrica {
    BATIMENTO("Batimento cardíaco", "bpm"),
    OXIGENIO("Oxigénio no sangue", "%"),
    PASSOS("Passos", "passos"),
    AGUA("Água", "ml");

    private final String etiqueta;
    private final String unidade;

    TipoMetrica(String etiqueta, String unidade) {
        this.etiqueta = etiqueta;
        this.unidade = unidade;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getUnidade() {
        return unidade;
    }
}
