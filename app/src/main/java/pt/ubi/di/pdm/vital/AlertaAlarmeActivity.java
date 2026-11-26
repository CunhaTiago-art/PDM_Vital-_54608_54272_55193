package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;
import pt.ubi.di.pdm.vital.ui.dashboard.DashboardViewModel;
import pt.ubi.di.pdm.vital.util.DataUtil;

/**
 * Ecrã de alarme: "HORA DA MEDICAÇÃO".
 *
 * Aceita no Intent o id da toma a confirmar, que é o que o AlarmManager vai
 * passar quando o Elemento 3 ligar os alarmes. Sem esse id — por exemplo
 * quando se chega aqui pelo atalho do dashboard — usa a próxima toma pendente.
 */
public class AlertaAlarmeActivity extends AppCompatActivity {

    /** Chave do Intent com o id do registo de toma. */
    public static final String EXTRA_REGISTO_ID = "registoId";

    private static final int MINUTOS_ADIAR = 10;

    private DashboardViewModel modelo;
    private TomaDeHoje toma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerta_alarme);

        modelo = new ViewModelProvider(this).get(DashboardViewModel.class);

        TextView tvHora = findViewById(R.id.tvAlertaHora);
        TextView tvNome = findViewById(R.id.tvAlertaNomeMed);
        TextView tvDose = findViewById(R.id.tvAlertaDoseMed);

        long pedido = getIntent().getLongExtra(EXTRA_REGISTO_ID, 0L);

        modelo.getProximaToma().observe(this, proxima -> {
            // Enquanto o alarme não existir, o id pedido é 0 e vale a próxima toma.
            if (pedido != 0 && proxima != null && proxima.registo.getId() != pedido) {
                return;
            }
            toma = proxima;
            if (proxima == null) {
                tvHora.setText("--:--");
                tvNome.setText("Sem tomas por confirmar");
                tvDose.setText("");
                return;
            }
            tvHora.setText(DataUtil.formatarHora(proxima.registo.getDataPrevista()));
            tvNome.setText(proxima.getNomeCompleto());
            tvDose.setText(proxima.getDescricao());
        });

        findViewById(R.id.btnConfirmarToma).setOnClickListener(v -> {
            if (toma == null) {
                finish();
                return;
            }
            if (!modelo.podeConfirmar(toma)) {
                Toast.makeText(this, "Ainda não são horas desta toma.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            modelo.confirmar(toma.registo.getId());
            Toast.makeText(this, "Toma registada.", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnAdiarToma).setOnClickListener(v -> {
            if (toma == null) {
                finish();
                return;
            }
            modelo.adiar(toma.registo.getId(), MINUTOS_ADIAR);
            Toast.makeText(this, "Adiado " + MINUTOS_ADIAR + " minutos.",
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
