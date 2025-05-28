package com.back.tfm.weatherapp.service;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct; // Importa PostConstruct

@Service
public class TranslationService {

    @Value("${google.cloud.translation.api.key}")
    private String googleTranslationApiKey;

    private Translate translate;

    // @PostConstruct se ejecuta después de que Spring ha inyectado las dependencias y los valores de @Value
    @PostConstruct
    public void init() {
        // Inicializa el cliente de Google Translate con tu clave de API
        // Para entornos de producción, considera usar Service Accounts en lugar de API Keys directamente.
        // https://cloud.google.com/docs/authentication/production
        this.translate = TranslateOptions.newBuilder().setApiKey(googleTranslationApiKey).build().getService();
        System.out.println("--- [TranslationService] Google Cloud Translation API cliente inicializado.");
    }

    /**
     * Traduce un texto de un idioma de origen a un idioma de destino.
     * @param text El texto a traducir.
     * @param sourceLanguage El código del idioma de origen (ej. "en" para inglés).
     * @param targetLanguage El código del idioma de destino (ej. "es" para español).
     * @return El texto traducido, o el texto original si la traducción falla.
     */
    public String translateText(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            Translation translation = translate.translate(
                    text,
                    Translate.TranslateOption.sourceLanguage(sourceLanguage),
                    Translate.TranslateOption.targetLanguage(targetLanguage)
            );
            System.out.println("--- [TranslationService] Texto traducido: '" + text.substring(0, Math.min(text.length(), 50)) + "...' a '" + translation.getTranslatedText().substring(0, Math.min(translation.getTranslatedText().length(), 50)) + "...'");
            return translation.getTranslatedText();
        } catch (Exception e) {
            System.err.println("!!! [TranslationService] Error al traducir el texto: " + e.getMessage());
            e.printStackTrace(); // Imprime la pila de llamadas para depuración
            return text; // Retorna el texto original en caso de error
        }
    }
}
