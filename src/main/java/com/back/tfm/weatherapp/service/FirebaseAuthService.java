package com.back.tfm.weatherapp.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {

    // Registrar un usuario
    public String registerUser(String email, String password) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setEmailVerified(false); // Por defecto, no verificado

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        return userRecord.getUid(); // Retorna el UID del usuario registrado
    }

    // Validar token JWT
    public String validateToken(String idToken) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().verifyIdToken(idToken).getUid(); // Retorna el UID del token validado
    }
}
