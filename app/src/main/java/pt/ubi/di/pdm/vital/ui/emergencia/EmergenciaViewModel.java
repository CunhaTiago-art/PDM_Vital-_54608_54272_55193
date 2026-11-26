package pt.ubi.di.pdm.vital.ui.emergencia;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.Membro;

/** Contactos de emergência, já ordenados pela base de dados. */
public class EmergenciaViewModel extends AndroidViewModel {

    private final LiveData<List<Membro>> contactos;

    public EmergenciaViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        contactos = new VitalRepository(aplicacao).observarContactosEmergencia();
    }

    public LiveData<List<Membro>> getContactos() {
        return contactos;
    }
}
