package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DuvidasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duvidas);

        findViewById(R.id.btnVoltarDuvidas).setOnClickListener(v -> finish());

        findViewById(R.id.btnFazerPergunta).setOnClickListener(v ->
                Toast.makeText(this, "Nova pergunta (a integrar).", Toast.LENGTH_SHORT).show()
        );
    }
}