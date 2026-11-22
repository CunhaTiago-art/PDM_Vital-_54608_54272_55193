package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ChatFamiliaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_familia);

        EditText etMensagem = findViewById(R.id.etMensagemChat);

        findViewById(R.id.btnVoltarChat).setOnClickListener(v -> finish());

        findViewById(R.id.btnEnviarChat).setOnClickListener(v -> {
            String texto = etMensagem.getText().toString().trim();
            if (!texto.isEmpty()) {
                Toast.makeText(this, "Mensagem enviada!", Toast.LENGTH_SHORT).show();
                etMensagem.setText("");
            }
        });

        findViewById(R.id.btnVozChat).setOnClickListener(v ->
                Toast.makeText(this, "Gravação de voz (a integrar).", Toast.LENGTH_SHORT).show()
        );
    }
}