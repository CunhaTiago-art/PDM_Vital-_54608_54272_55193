package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.data.model.ConsultaComAcompanhante;
import pt.ubi.di.pdm.vital.ui.consultas.ConsultasAdapter;
import pt.ubi.di.pdm.vital.ui.consultas.ConsultasViewModel;
import pt.ubi.di.pdm.vital.util.DataUtil;

/**
 * Consultas: o cartão da próxima, seguido das que estão para vir e das já realizadas.
 * Tudo vem da base de dados; marcar uma consulta nova faz a lista atualizar-se sozinha.
 */
public class ConsultasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultas);

        ConsultasViewModel modelo = new ViewModelProvider(this).get(ConsultasViewModel.class);

        View cardProxima = findViewById(R.id.cardProximaConsulta);
        TextView tvDataHora = findViewById(R.id.tvProxConsultaDataHora);
        TextView tvEspecialidade = findViewById(R.id.tvProxConsultaEspecialidade);
        TextView tvMedicoLocal = findViewById(R.id.tvProxConsultaMedicoLocal);
        TextView tvAcompanhante = findViewById(R.id.tvProxConsultaAcompanhante);
        RecyclerView lista = findViewById(R.id.rvConsultas);

        ConsultasAdapter adaptador = new ConsultasAdapter();
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        // ---------- Cartão da próxima consulta ----------
        modelo.getProxima().observe(this, item -> {
            if (item == null) {
                cardProxima.setVisibility(View.GONE);
                return;
            }
            cardProxima.setVisibility(View.VISIBLE);
            tvDataHora.setText(DataUtil.formatarDiaSemana(item.consulta.getDataHora())
                    + " · " + DataUtil.formatarHora(item.consulta.getDataHora()));
            tvEspecialidade.setText(item.consulta.getEspecialidade());
            tvMedicoLocal.setText(descreverMedicoLocal(item));
            tvAcompanhante.setText(item.acompanhanteNome == null
                    ? "Sem acompanhante definido"
                    : "Acompanha: " + item.acompanhanteNome);
        });

        // ---------- Lista: futuras primeiro, passadas a seguir ----------
        // As duas consultas chegam em momentos diferentes, por isso a lista é
        // recomposta sempre que uma delas muda.
        final List<ConsultaComAcompanhante> futuras = new ArrayList<>();
        final List<ConsultaComAcompanhante> passadas = new ArrayList<>();

        modelo.getFuturas().observe(this, novas -> {
            futuras.clear();
            if (novas != null) {
                futuras.addAll(novas);
            }
            adaptador.submeter(juntar(futuras, passadas));
        });

        modelo.getPassadas().observe(this, novas -> {
            passadas.clear();
            if (novas != null) {
                passadas.addAll(novas);
            }
            adaptador.submeter(juntar(futuras, passadas));
        });

        findViewById(R.id.btnVoltarConsultas).setOnClickListener(v -> finish());

        findViewById(R.id.btnMarcarConsulta).setOnClickListener(v ->
                startActivity(new Intent(this, MarcarConsultaActivity.class)));
    }

    private List<ConsultaComAcompanhante> juntar(List<ConsultaComAcompanhante> futuras,
                                                 List<ConsultaComAcompanhante> passadas) {
        List<ConsultaComAcompanhante> todas = new ArrayList<>(futuras);
        todas.addAll(passadas);
        return todas;
    }

    private String descreverMedicoLocal(ConsultaComAcompanhante item) {
        String medico = item.consulta.getMedico();
        String local = item.consulta.getLocal();
        if (medico != null && !medico.trim().isEmpty()
                && local != null && !local.trim().isEmpty()) {
            return medico + " · " + local;
        }
        if (medico != null && !medico.trim().isEmpty()) {
            return medico;
        }
        return local == null ? "" : local;
    }
}
