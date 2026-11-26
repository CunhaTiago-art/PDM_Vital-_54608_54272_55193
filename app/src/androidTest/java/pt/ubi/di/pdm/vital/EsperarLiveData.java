package pt.ubi.di.pdm.vital;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Lê o valor de um LiveData dentro de um teste.
 *
 * As consultas do Room que devolvem LiveData correm numa thread de fundo e só
 * produzem valor quando alguém as observa. Num teste não há ciclo de vida de
 * ecrã, por isso observa-se à força a partir da thread principal e espera-se
 * pelo primeiro resultado.
 */
final class EsperarLiveData {

    private static final int SEGUNDOS_DE_ESPERA = 5;

    private EsperarLiveData() {
    }

    static <T> T valorDe(final LiveData<T> fonte) throws InterruptedException {
        final Object[] resultado = new Object[1];
        final CountDownLatch espera = new CountDownLatch(1);

        final Observer<T> observador = new Observer<T>() {
            @Override
            public void onChanged(T valor) {
                resultado[0] = valor;
                fonte.removeObserver(this);
                espera.countDown();
            }
        };

        new Handler(Looper.getMainLooper()).post(() -> fonte.observeForever(observador));

        if (!espera.await(SEGUNDOS_DE_ESPERA, TimeUnit.SECONDS)) {
            throw new IllegalStateException("O LiveData não produziu valor a tempo.");
        }

        @SuppressWarnings("unchecked")
        T valor = (T) resultado[0];
        return valor;
    }
}
