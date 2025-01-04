package com.back.tfm.weatherapp.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String FIREBASE_API_KEY = "AIzaSyBQ4F2VK9t0dza3J9YX5qvx2DXtinW8u5U";
    private static final String FIREBASE_AUTH_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_API_KEY;

    public String registerUser(String email, String password) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password);

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        logger.info("Usuario registrado con UID: {}", userRecord.getUid());
        return userRecord.getUid();
    }

    public String authenticateUser(String email, String password) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> request = Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", "true"
        );

        try {
            HashMap<String, Object> response = restTemplate.postForObject(FIREBASE_AUTH_URL, request, HashMap.class);
            logger.info("Token obtenido de Firebase para el usuario {}: {}", email, response.get("idToken"));
            return (String) response.get("idToken");
        } catch (Exception e) {
            logger.error("Error al autenticar usuario: {}", e.getMessage());
            throw new Exception("Firebase authentication failed: " + e.getMessage());
        }
    }
}
