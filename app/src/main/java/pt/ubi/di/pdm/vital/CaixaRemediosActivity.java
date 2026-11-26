package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pt.ubi.di.pdm.vital.ui.caixa.CaixaViewModel;
import pt.ubi.di.pdm.vital.ui.caixa.MedicamentosAdapter;

/**
 * Caixa de Remédios: a lista das prescrições guardadas na base de dados,
 * cada uma com os seus horários. Acrescentar ou alterar um medicamento
 * no ecrã seguinte reflete-se aqui sem ser preciso recarregar nada.
 */
public class CaixaRemediosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caixa_remedios);

        CaixaViewModel modelo = new ViewModelProvider(this).get(CaixaViewModel.class);

        MedicamentosAdapter adaptador = new MedicamentosAdapter(item -> {
            // O ecrã de edição ainda não recebe um medicamento existente.
            // Fica preparado: o id já é passado no Intent.
            Intent intent = new Intent(this, AdicionarRemedioActivity.class);
            intent.putExtra("medicamentoId", item.medicamento.getId());
            startActivity(intent);
        });

        RecyclerView lista = findViewById(R.id.rvMedicamentos);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        modelo.getMedicamentos().observe(this, medicamentos -> {
            adaptador.submeter(medicamentos);
            if (medicamentos != null && medicamentos.isEmpty()) {
                Toast.makeText(this,
                        "Ainda não há medicamentos. Toque em Adicionar.",
                        Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnVoltarCaixa).setOnClickListener(v -> finish());

        findViewById(R.id.btnAdicionarMed).setOnClickListener(v ->
                startActivity(new Intent(this, AdicionarRemedioActivity.class)));
    }
}
