package com.back.tfm.weatherapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public DatabaseReference firebaseDatabase() throws IOException {
        // Verifica si FirebaseApp ya está inicializado
        if (FirebaseApp.getApps().isEmpty()) {
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebase-service-account.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://base-app-weather-default-rtdb.firebaseio.com/") // Reemplaza con tu URL
                    .build();

            FirebaseApp.initializeApp(options);
        }

        // Retorna la referencia a la base de datos
        return FirebaseDatabase.getInstance().getReference();
    }
}
