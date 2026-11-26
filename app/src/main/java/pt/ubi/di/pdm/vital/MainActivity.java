package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;
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
        View btnJaTomei = findViewById(R.id.btnJaTomeiDashboard);
        View cardProxima = findViewById(R.id.cardProximaToma);
        RecyclerView rvHoje = findViewById(R.id.rvTomasHoje);

        tvData.setText(dataDeHoje());

        // ---------- Lista "Hoje" ----------
        TomasHojeAdapter adaptador = new TomasHojeAdapter();
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
            tvHora.setText(pt.ubi.di.pdm.vital.util.DataUtil
                    .formatarHora(toma.registo.getDataPrevista()));
            tvNome.setText(toma.getNomeCompleto());
            tvInstrucao.setText(toma.getDescricao());
            btnJaTomei.setVisibility(View.VISIBLE);
        });

        btnJaTomei.setOnClickListener(v -> {
            if (proxima == null) {
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

    /** Cabeçalho do dashboard: "Qui, 26 nov". */
    private String dataDeHoje() {
        Locale pt = new Locale("pt", "PT");
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", pt));
    }
}
