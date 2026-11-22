package pt.ubi.di.pdm.vital;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

public class MarcarConsultaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marcar_consulta);

        EditText etData = findViewById(R.id.etDataConsulta);
        EditText etHora = findViewById(R.id.etHoraConsulta);

        findViewById(R.id.btnVoltarMarcarConsulta).setOnClickListener(v -> finish());

        etData.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etData.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        etHora.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) ->
                    etHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                    10, 30, true
            ).show();
        });

        findViewById(R.id.btnGuardarConsulta).setOnClickListener(v -> {
            Toast.makeText(this, "Consulta guardada e comunidade avisada!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}