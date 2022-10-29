package com.example.project.controller;

import cn.apiclub.captcha.Captcha;
import cn.apiclub.captcha.backgrounds.FlatColorBackgroundProducer;
import cn.apiclub.captcha.text.producer.DefaultTextProducer;
import cn.apiclub.captcha.text.renderer.DefaultWordRenderer;
import com.example.project.dto.ApiResponse;
import com.example.project.dto.LoginDTO;
import com.example.project.dto.UserDto;
import com.example.project.entity.User;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.JwtProvider;
import com.example.project.util.CaptchaUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;


import javax.validation.Valid;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author "ISMOIL NIGMATOV"
 * @created 3:22 PM on 10/6/2022
 * @project Project
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;
    @PostMapping("/login")
    @Operation(summary = "Return token after login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword()));

            String token = jwtProvider.generateToken(loginDTO.getEmail());
            return ResponseEntity.ok(token);
        }

    @Operation(summary = "Captcha")
    @GetMapping("/signup")
    public ResponseEntity<?> registration(){
        UserDto userDto=new UserDto();
        getCaptcha(userDto);
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Saving user")
    @PostMapping("/signup")
    ResponseEntity<?> registration(@Valid @RequestBody UserDto userDto){
        if (userDto.getCaptcha().equals(userDto.getHiddenCaptcha())) {
            if(userRepository.existsByEmail(userDto.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already in use");
            }
            if(!userDto.getPassword().equals(userDto.getSecondPassword())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Passwords are not the same");
            }
            User user=new User();
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            user.setEmail(userDto.getEmail());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setPhoneNumber(userDto.getPhoneNumber());
            if(Objects.nonNull(userDto.getCompanyName())) user.setCompanyName(userDto.getCompanyName());
            user.setRole(roleRepository.findById(2L).orElseThrow(() -> new RuntimeException("Role not Found")));
            userRepository.save(user);
            return ResponseEntity.ok("User succesfully registered");
        } else {
            getCaptcha(userDto);
            ApiResponse response=new ApiResponse<>();
            response.setData(userDto);
            response.setMessage("Enter the captcha correctly!");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    private void getCaptcha(UserDto userDto) {
        java.util.List<Color> textColors = Arrays.asList(Color.BLACK, Color.BLUE, Color.RED,Color.MAGENTA);
        List<Font> textFonts = Arrays.asList(new Font("Arial", Font.BOLD, 40), new Font("Courier", Font.BOLD, 40));

        Color backgroundColor = Color.GREEN;

        Captcha captcha = new Captcha.Builder(200,50)
                .addText(
                        new DefaultTextProducer(),
                        new DefaultWordRenderer(textColors, textFonts))
                .addBackground(new FlatColorBackgroundProducer(backgroundColor))
                .build();

        userDto.setHiddenCaptcha(captcha.getAnswer());
        userDto.setCaptcha("");
        userDto.setRealCaptcha(CaptchaUtil.encodeCaptcha(captcha));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> e = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            e.put(fieldName, errorMessage);
        });
        return e;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> unauthorized(){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email not found");
    }
}
