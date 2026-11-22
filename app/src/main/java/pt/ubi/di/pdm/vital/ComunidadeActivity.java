package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ComunidadeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunidade);

        findViewById(R.id.btnVoltarComunidade).setOnClickListener(v -> finish());

        findViewById(R.id.btnConvidarMembro).setOnClickListener(v ->
                Toast.makeText(this, "Convite pronto a partilhar.", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.cardHistorial).setOnClickListener(v ->
                startActivity(new Intent(this, HistorialActivity.class))
        );

        findViewById(R.id.cardConsultas).setOnClickListener(v ->
                startActivity(new Intent(this, ConsultasActivity.class))
        );

        findViewById(R.id.cardChat).setOnClickListener(v ->
                startActivity(new Intent(this, ChatFamiliaActivity.class))
        );

        findViewById(R.id.cardDuvidas).setOnClickListener(v ->
                startActivity(new Intent(this, DuvidasActivity.class))
        );
    }
}