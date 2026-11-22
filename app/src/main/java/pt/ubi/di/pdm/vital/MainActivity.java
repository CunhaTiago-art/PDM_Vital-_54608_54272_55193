package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnJaTomeiDashboard).setOnClickListener(v ->
                Toast.makeText(this, "Toma registada!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnAbrirCaixa).setOnClickListener(v ->
                startActivity(new Intent(this, CaixaRemediosActivity.class))
        );

        findViewById(R.id.btnSos).setOnClickListener(v ->
                startActivity(new Intent(this, EmergenciaActivity.class))
        );

        findViewById(R.id.btnComunidade).setOnClickListener(v ->
                startActivity(new Intent(this, ComunidadeActivity.class))
        );

        // Atalho de teste: premir sem largar o cartão abre o ecrã de Alarme Ativo
        findViewById(R.id.cardProximaToma).setOnLongClickListener(v -> {
            startActivity(new Intent(this, AlertaAlarmeActivity.class));
            return true;
        });
    }
}