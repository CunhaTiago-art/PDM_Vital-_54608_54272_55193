package pt.ubi.di.pdm.vital;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class AdicionarRemedioActivity extends AppCompatActivity {

    private int quantidade = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_remedio);

        TextView tvQuantidade = findViewById(R.id.tvQuantidadeDose);

        findViewById(R.id.btnVoltarNovo).setOnClickListener(v -> finish());

        findViewById(R.id.btnMenosDose).setOnClickListener(v -> {
            if (quantidade > 1) {
                quantidade--;
                atualizarTextoDose(tvQuantidade);
            }
        });

        findViewById(R.id.btnMaisDose).setOnClickListener(v -> {
            quantidade++;
            atualizarTextoDose(tvQuantidade);
        });

        findViewById(R.id.btnAdicionarHora).setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(this, (view, hour, minute) -> {
                String hora = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                Toast.makeText(this, "Hora selecionada: " + hora, Toast.LENGTH_SHORT).show();
            }, 8, 0, true);
            picker.show();
        });

        findViewById(R.id.btnGuardarMed).setOnClickListener(v -> {
            Toast.makeText(this, "Medicamento guardado!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void atualizarTextoDose(TextView tv) {
        tv.setText(quantidade == 1 ? "1 comprimido" : quantidade + " comprimidos");
    }
}