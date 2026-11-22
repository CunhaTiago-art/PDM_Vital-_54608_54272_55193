package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AlertaAlarmeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerta_alarme);

        findViewById(R.id.btnConfirmarToma).setOnClickListener(v -> {
            Toast.makeText(this, "Toma confirmada!", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnAdiarToma).setOnClickListener(v -> {
            Toast.makeText(this, "Alarme adiado 10 minutos.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}