package com.cityscape.geoszabaduloszobabackend.controller;

import com.cityscape.geoszabaduloszobabackend.model.dto.LoginRequest;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){
        UserEntity user = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserEntity user) {
        try {
            UserEntity savedUser = authService.register(user);
            return ResponseEntity.ok(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
