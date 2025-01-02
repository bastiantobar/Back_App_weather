package com.back.tfm.weatherapp.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class FirebaseUserService {

    public String registerUser(String email, String password) throws FirebaseAuthException {
        // Crear un usuario en Firebase
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setEmailVerified(false); // Por defecto, no verificado

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        return userRecord.getUid(); // Retorna el UID del usuario creado
    }
}
