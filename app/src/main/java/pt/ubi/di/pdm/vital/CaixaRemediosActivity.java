package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CaixaRemediosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caixa_remedios);

        findViewById(R.id.btnVoltarCaixa).setOnClickListener(v -> finish());

        findViewById(R.id.btnAdicionarMed).setOnClickListener(v ->
                startActivity(new Intent(this, AdicionarRemedioActivity.class))
        );
    }
}