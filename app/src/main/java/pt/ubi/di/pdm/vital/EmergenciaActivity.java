package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class EmergenciaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergencia);

        findViewById(R.id.btnVoltarEmergencia).setOnClickListener(v -> finish());

        findViewById(R.id.btnLigarPrincipal).setOnClickListener(v -> abrirMarcador("912345678"));
        findViewById(R.id.btnLigarSecundario).setOnClickListener(v -> abrirMarcador("919876543"));
        findViewById(R.id.btnLigar112).setOnClickListener(v -> abrirMarcador("112"));
    }

    private void abrirMarcador(String numero) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero));
        startActivity(intent);
    }
}