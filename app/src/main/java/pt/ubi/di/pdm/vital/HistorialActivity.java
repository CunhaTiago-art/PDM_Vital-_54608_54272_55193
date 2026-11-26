package pt.ubi.di.pdm.vital;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.util.List;

import pt.ubi.di.pdm.vital.data.model.ResumoDia;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.ui.historial.HistorialAdapter;
import pt.ubi.di.pdm.vital.ui.historial.HistorialViewModel;

/**
 * Historial.
 *
 * Mostra três coisas vindas da base de dados: o resumo da semana em números,
 * os sete círculos com as tomas de cada dia, e a lista de acontecimentos —
 * tomas, consultas e notas — numa só sequência ordenada no tempo.
 */
public class HistorialActivity extends AppCompatActivity {

    private HistorialViewModel modelo;
    private MaterialButton btnTudo;
    private MaterialButton btnTomas;
    private MaterialButton btnConsultas;
    private TextView[] circulos;
    private TextView tvEtiquetaSemana;
    private MaterialButton btnSemanaSeguinte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        modelo = new ViewModelProvider(this).get(HistorialViewModel.class);

        TextView tvTotal = findViewById(R.id.tvTotalTomasSemana);
        RecyclerView lista = findViewById(R.id.rvHistorial);

        tvEtiquetaSemana = findViewById(R.id.tvEtiquetaSemana);
        btnSemanaSeguinte = findViewById(R.id.btnSemanaSeguinte);
        MaterialButton btnSemanaAnterior = findViewById(R.id.btnSemanaAnterior);

        btnTudo = findViewById(R.id.btnFiltroTudo);
        btnTomas = findViewById(R.id.btnFiltroTomas);
        btnConsultas = findViewById(R.id.btnFiltroConsultas);

        // Segunda a domingo, pela mesma ordem dos círculos no wireframe
        circulos = new TextView[]{
                findViewById(R.id.tvDiaSeg), findViewById(R.id.tvDiaTer),
                findViewById(R.id.tvDiaQua), findViewById(R.id.tvDiaQui),
                findViewById(R.id.tvDiaSex), findViewById(R.id.tvDiaSab),
                findViewById(R.id.tvDiaDom)};

        // ---------- Lista ----------
        HistorialAdapter adaptador = new HistorialAdapter();
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        modelo.getEventos().observe(this, adaptador::submeter);

        // ---------- Resumo da semana ----------
        modelo.getTomadas().observe(this, t -> atualizarResumo(tvTotal));
        modelo.getTotal().observe(this, t -> atualizarResumo(tvTotal));
        modelo.getSemana().observe(this, this::desenharCirculos);

        // ---------- Navegação entre semanas ----------
        btnSemanaAnterior.setOnClickListener(v -> {
            modelo.semanaAnterior();
            atualizarEtiquetaSemana();
        });
        btnSemanaSeguinte.setOnClickListener(v -> {
            modelo.semanaSeguinte();
            atualizarEtiquetaSemana();
        });
        atualizarEtiquetaSemana();

        // ---------- Separadores ----------
        btnTudo.setOnClickListener(v -> escolherFiltro(HistorialViewModel.TUDO));
        btnTomas.setOnClickListener(v -> escolherFiltro(HistorialViewModel.TOMAS));
        btnConsultas.setOnClickListener(v -> escolherFiltro(HistorialViewModel.CONSULTAS));
        pintarFiltros();

        findViewById(R.id.btnVoltarHistorial).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdicionarNota).setOnClickListener(v -> pedirNota());
    }

    /**
     * Mostra de que semana se está a falar e desliga a seta de avançar quando
     * já se está na semana atual — um botão que não faz nada confunde mais do
     * que um botão que se vê estar indisponível.
     */
    private void atualizarEtiquetaSemana() {
        tvEtiquetaSemana.setText(DataUtil.descreverSemana(modelo.getDeslocamento()));
        boolean pode = modelo.podeAvancar();
        btnSemanaSeguinte.setEnabled(pode);
        btnSemanaSeguinte.setAlpha(pode ? 1f : 0.3f);
    }

    /** "18 de 21 tomas" — os dois números vêm de consultas separadas. */
    private void atualizarResumo(TextView alvo) {
        Integer tomadas = modelo.getTomadas().getValue();
        Integer total = modelo.getTotal().getValue();
        if (tomadas == null || total == null) {
            return;
        }
        alvo.setText(tomadas + " de " + total + (total == 1 ? " toma" : " tomas"));
    }

    /**
     * Preenchido = todas as tomas do dia foram confirmadas.
     * Contorno = falhou alguma. Vazio = não havia tomas nesse dia.
     */
    private void desenharCirculos(List<ResumoDia> dias) {
        for (TextView circulo : circulos) {
            circulo.setText("–");
            circulo.setBackgroundResource(R.drawable.bg_dia_circulo);
            circulo.setTextColor(ContextCompat.getColor(this, R.color.vital_gray));
        }
        if (dias == null) {
            return;
        }
        for (ResumoDia dia : dias) {
            int indice;
            try {
                indice = LocalDate.parse(dia.dia).getDayOfWeek().getValue() - 1;
            } catch (Exception e) {
                continue; // data inesperada: ignora em vez de rebentar o ecrã
            }
            if (indice < 0 || indice >= circulos.length) {
                continue;
            }
            TextView circulo = circulos[indice];
            circulo.setText(String.valueOf(dia.tomadas));
            if (dia.isCompleto()) {
                circulo.setBackgroundResource(R.drawable.bg_dia_completo);
                circulo.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                circulo.setBackgroundResource(R.drawable.bg_dia_falhado);
                circulo.setTextColor(ContextCompat.getColor(this, R.color.vital_navy));
            }
        }
    }

    private void escolherFiltro(String filtro) {
        modelo.filtrar(filtro);
        pintarFiltros();
    }

    private void pintarFiltros() {
        String atual = modelo.getFiltroAtual();
        pintar(btnTudo, HistorialViewModel.TUDO.equals(atual));
        pintar(btnTomas, HistorialViewModel.TOMAS.equals(atual));
        pintar(btnConsultas, HistorialViewModel.CONSULTAS.equals(atual));
    }

    private void pintar(MaterialButton botao, boolean ativo) {
        int verde = ContextCompat.getColor(this, R.color.vital_green);
        int navy = ContextCompat.getColor(this, R.color.vital_navy);
        int branco = ContextCompat.getColor(this, R.color.white);

        botao.setBackgroundTintList(ColorStateList.valueOf(ativo ? verde : branco));
        botao.setTextColor(ativo ? branco : navy);
        botao.setStrokeColor(ColorStateList.valueOf(navy));
        botao.setStrokeWidth(ativo ? 0 : 3);
    }

    /** "+ Adicionar nota": um recado da rede de apoio, guardado no historial. */
    private void pedirNota() {
        if (!modelo.temAutor()) {
            Toast.makeText(this,
                    "Volte ao ecrã inicial e indique o seu nome.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        EditText campo = new EditText(this);
        campo.setHint("Ex.: lembrar de beber água");
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setTextSize(18f);

        int margem = (int) (24 * getResources().getDisplayMetrics().density);
        campo.setPadding(margem, margem, margem, margem);

        new AlertDialog.Builder(this)
                .setTitle("Adicionar nota")
                .setView(campo)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, w) ->
                        modelo.adicionarNota(campo.getText().toString()))
                .show();
    }
}
