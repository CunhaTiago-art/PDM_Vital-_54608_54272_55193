package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HistorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        findViewById(R.id.btnVoltarHistorial).setOnClickListener(v -> finish());

        findViewById(R.id.btnAdicionarNota).setOnClickListener(v ->
                Toast.makeText(this, "Adicionar nota ao historial.", Toast.LENGTH_SHORT).show()
        );
    }
}