package com.example.project.controller;

import com.example.project.dto.ApiResponse;
import com.example.project.dto.UpdateUserDTO;
import com.example.project.dto.UserDTO;
import com.example.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author "ISMOIL NIGMATOV"
 * @created 3:34 PM on 11/5/2022
 * @project Project
 */
@RestController
@RequestMapping("/u/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize(value = "hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllUsers(){
       ApiResponse response = userService.getAll();
       return ResponseEntity.ok(response);
    }

    @PreAuthorize(value = "hasAnyAuthority('ADMIN','USER')")
    @PutMapping("/{email}")
    public ResponseEntity<?> updateUser(@PathVariable String email ,@RequestBody UpdateUserDTO updateUserDTO){
       ApiResponse response = userService.update(email,updateUserDTO);
       return ResponseEntity.status(response.isSuccess()? HttpStatus.OK:HttpStatus.CONFLICT).body(response);
    }

    @PreAuthorize(value = "hasAnyAuthority('ADMIN','USER')")
    @PostMapping("/profile/photo/{email}")
    public ResponseEntity<?> profilePhoto(@PathVariable String email,@RequestPart MultipartFile multipartFile) throws IOException {
        ApiResponse response= userService.photo(email,multipartFile);
        return ResponseEntity.status(response.isSuccess()?HttpStatus.OK:HttpStatus.CONFLICT).body(response);
    }
}