package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pt.ubi.di.pdm.vital.data.model.MetricaSaude;
import pt.ubi.di.pdm.vital.data.model.TipoMetrica;
import pt.ubi.di.pdm.vital.ui.metricas.MetricasAdapter;
import pt.ubi.di.pdm.vital.ui.metricas.MetricasViewModel;

/**
 * Saúde: registo e historial de métricas.
 *
 * Batimento e oxigénio mostram a última medição; passos e água mostram o
 * acumulado do dia, porque é assim que fazem sentido. Os valores possíveis
 * de cada métrica são validados antes de gravar — um batimento de 500 ou
 * uma saturação de 150% são erros de escrita, não medições.
 */
public class MetricasActivity extends AppCompatActivity {

    private MetricasViewModel modelo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metricas);

        modelo = new ViewModelProvider(this).get(MetricasViewModel.class);

        TextView tvBatimento = findViewById(R.id.tvValorBatimento);
        TextView tvOxigenio = findViewById(R.id.tvValorOxigenio);
        TextView tvPassos = findViewById(R.id.tvValorPassos);
        TextView tvAgua = findViewById(R.id.tvValorAgua);
        RecyclerView lista = findViewById(R.id.rvMetricas);

        MetricasAdapter adaptador = new MetricasAdapter();
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        modelo.getTodas().observe(this, adaptador::submeter);

        modelo.getUltimoBatimento().observe(this, m -> mostrarUltima(tvBatimento, m));
        modelo.getUltimoOxigenio().observe(this, m -> mostrarUltima(tvOxigenio, m));
        modelo.getPassosDeHoje().observe(this, total -> mostrarTotal(tvPassos, total, ""));
        modelo.getAguaDeHoje().observe(this, total -> mostrarTotal(tvAgua, total, " ml"));

        findViewById(R.id.btnRegistarBatimento).setOnClickListener(v ->
                pedirValor(TipoMetrica.BATIMENTO, 30, 220));
        findViewById(R.id.btnRegistarOxigenio).setOnClickListener(v ->
                pedirValor(TipoMetrica.OXIGENIO, 50, 100));
        findViewById(R.id.btnRegistarPassos).setOnClickListener(v ->
                pedirValor(TipoMetrica.PASSOS, 1, 100000));
        findViewById(R.id.btnRegistarAgua).setOnClickListener(v ->
                pedirValor(TipoMetrica.AGUA, 10, 3000));

        findViewById(R.id.btnVoltarMetricas).setOnClickListener(v -> finish());
    }

    private void mostrarUltima(TextView alvo, MetricaSaude metrica) {
        alvo.setText(metrica == null
                ? "—"
                : Math.round(metrica.getValor()) + " " + metrica.getTipo().getUnidade());
    }

    private void mostrarTotal(TextView alvo, Double total, String unidade) {
        long valor = total == null ? 0 : Math.round(total);
        alvo.setText(valor == 0 ? "—" : valor + unidade);
    }

    /**
     * Pede o valor e só aceita o que for plausível para aquela métrica.
     * Sem limites, um engano no teclado entrava no historial clínico como
     * se fosse uma medição verdadeira.
     */
    private void pedirValor(TipoMetrica tipo, int minimo, int maximo) {
        EditText campo = new EditText(this);
        campo.setInputType(InputType.TYPE_CLASS_NUMBER);
        campo.setHint("Entre " + minimo + " e " + maximo + " " + tipo.getUnidade());
        campo.setTextSize(22f);

        int margem = (int) (24 * getResources().getDisplayMetrics().density);
        campo.setPadding(margem, margem, margem, margem);

        new AlertDialog.Builder(this)
                .setTitle(tipo.getEtiqueta())
                .setView(campo)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, w) -> guardar(tipo, campo.getText().toString(),
                        minimo, maximo))
                .show();
    }

    private void guardar(TipoMetrica tipo, String texto, int minimo, int maximo) {
        double valor;
        try {
            valor = Double.parseDouble(texto.trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Escreva um número.", Toast.LENGTH_LONG).show();
            return;
        }
        if (valor < minimo || valor > maximo) {
            Toast.makeText(this,
                    "Valor fora do esperado para " + tipo.getEtiqueta().toLowerCase()
                            + " (entre " + minimo + " e " + maximo + ").",
                    Toast.LENGTH_LONG).show();
            return;
        }
        modelo.registar(tipo, valor);
        Toast.makeText(this, tipo.getEtiqueta() + " registado.", Toast.LENGTH_SHORT).show();
    }
}
