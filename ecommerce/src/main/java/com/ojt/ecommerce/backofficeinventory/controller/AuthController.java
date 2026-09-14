package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.AuthRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.AuthResponseDto;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.backofficeinventory.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody AuthRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();


        String token = jwtUtil.generateToken(userDetails);


        AuthResponseDto response = AuthResponseDto.builder()
                .token(token)
                .username(userDetails.getUser().getUserName())
                .role(userDetails.getUser().getUserRole().getRoleName())
                .build();

        return ResponseEntity.ok(response);
    }
}