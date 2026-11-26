package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.ui.dashboard.DashboardViewModel;
import pt.ubi.di.pdm.vital.ui.dashboard.TomasHojeAdapter;

/**
 * Dashboard. Tudo o que aqui se vê vem da base de dados local:
 * a próxima toma por confirmar, a lista das tomas de hoje e o estado de cada uma.
 * O botão "JÁ TOMEI" grava a confirmação no Room, e a lista reage sozinha.
 */
public class MainActivity extends AppCompatActivity {

    private DashboardViewModel modelo;
    private TomaDeHoje proxima;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modelo = new ViewModelProvider(this).get(DashboardViewModel.class);

        TextView tvData = findViewById(R.id.tvDataHoje);
        TextView tvHora = findViewById(R.id.tvHoraToma);
        TextView tvNome = findViewById(R.id.tvNomeRemedio);
        TextView tvInstrucao = findViewById(R.id.tvInstrucaoRemedio);
        MaterialButton btnJaTomei = findViewById(R.id.btnJaTomeiDashboard);
        View cardProxima = findViewById(R.id.cardProximaToma);
        RecyclerView rvHoje = findViewById(R.id.rvTomasHoje);

        tvData.setText(dataDeHoje());

        // ---------- Lista "Hoje" ----------
        TomasHojeAdapter adaptador = new TomasHojeAdapter(this::perguntarSeConfirma);
        rvHoje.setLayoutManager(new LinearLayoutManager(this));
        rvHoje.setAdapter(adaptador);
        modelo.getTomasDeHoje().observe(this, adaptador::submeter);

        // ---------- Cartão "Próxima toma" ----------
        modelo.getProximaToma().observe(this, toma -> {
            proxima = toma;
            if (toma == null) {
                // Nada por tomar: não faz sentido mostrar um botão de confirmação.
                tvHora.setText("--:--");
                tvNome.setText("Sem tomas por confirmar");
                tvInstrucao.setText("Está tudo em dia");
                btnJaTomei.setVisibility(View.GONE);
                return;
            }
            tvHora.setText(DataUtil.formatarHora(toma.registo.getDataPrevista()));
            tvNome.setText(toma.getNomeCompleto());
            tvInstrucao.setText(toma.getDescricao());
            btnJaTomei.setVisibility(View.VISIBLE);
            atualizarBotao(btnJaTomei, toma);
        });

        btnJaTomei.setOnClickListener(v -> {
            if (proxima == null) {
                return;
            }
            // Segunda barreira, além do botão desativado: uma toma nunca é
            // confirmada fora da sua janela, aconteça o que acontecer na interface.
            if (!modelo.podeConfirmar(proxima)) {
                Toast.makeText(this,
                        "Ainda não são horas. A próxima toma é às "
                                + DataUtil.formatarHora(proxima.registo.getDataPrevista()) + ".",
                        Toast.LENGTH_LONG).show();
                return;
            }
            modelo.confirmar(proxima.registo.getId());
            Toast.makeText(this,
                    proxima.getNomeCompleto() + " registado.",
                    Toast.LENGTH_SHORT).show();
        });

        // ---------- Navegação ----------
        findViewById(R.id.btnAbrirCaixa).setOnClickListener(v ->
                startActivity(new Intent(this, CaixaRemediosActivity.class)));

        findViewById(R.id.btnSos).setOnClickListener(v ->
                startActivity(new Intent(this, EmergenciaActivity.class)));

        findViewById(R.id.btnComunidade).setOnClickListener(v ->
                startActivity(new Intent(this, ComunidadeActivity.class)));

        // Atalho de teste: premir sem largar o cartão abre o ecrã de Alarme Ativo
        cardProxima.setOnLongClickListener(v -> {
            startActivity(new Intent(this, AlertaAlarmeActivity.class));
            return true;
        });
    }

    /**
     * Tocar numa toma da lista "Hoje" permite confirmá-la mais tarde,
     * incluindo uma que já tenha sido dada como falhada. Pede confirmação
     * primeiro, porque é um toque fácil de dar sem querer.
     */
    private void perguntarSeConfirma(TomaDeHoje toma) {
        if (!modelo.podeConfirmar(toma)) {
            Toast.makeText(this,
                    "Ainda não são horas desta toma.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String atraso = pt.ubi.di.pdm.vital.data.VitalRepository.descreverAtraso(
                toma.registo.getDataPrevista(), System.currentTimeMillis());

        String mensagem = "Marcar " + toma.getNomeCompleto() + " das "
                + DataUtil.formatarHora(toma.registo.getDataPrevista()) + " como tomado?";
        if (atraso != null) {
            mensagem += "\n\nFicará registado " + atraso + ".";
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmar toma")
                .setMessage(mensagem)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sim, tomei", (d, w) -> modelo.confirmar(toma.registo.getId()))
                .show();
    }

    /**
     * O botão só fica disponível quando a toma está mesmo a chegar.
     * Fora de horas mostra porquê, em vez de ficar apagado sem explicação —
     * um botão cinzento e mudo não diz nada a quem o está a usar.
     */
    private void atualizarBotao(MaterialButton botao, TomaDeHoje toma) {
        if (modelo.podeConfirmar(toma)) {
            botao.setEnabled(true);
            botao.setAlpha(1f);
            botao.setText("✓  JÁ TOMEI");
        } else {
            botao.setEnabled(false);
            botao.setAlpha(0.45f);
            botao.setText("Ainda não são horas");
        }
    }

    /** Cabeçalho do dashboard: "Qui, 26 nov". */
    private String dataDeHoje() {
        Locale pt = new Locale("pt", "PT");
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", pt));
    }
}
