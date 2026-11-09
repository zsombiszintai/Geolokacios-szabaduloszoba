package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.service.SettingsService;
import com.cityscape.geoszabaduloszobabackend.service.AccountService;
import com.cityscape.geoszabaduloszobabackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsAPI {

    private final SettingsService settingsService;
    private final UserService userService;
    private final AccountService accountService;

    @GetMapping("/account")
    public AccountService.AccountInfo account() {
        return accountService.info();
    }

    @PostMapping("/account/verify-email")
    public ResponseEntity<Void> verifyEmail() {
        accountService.verifyEmail();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestBody Map<String, String> body) {
        accountService.deleteAccount(body.get("confirmation"));
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadAvatar(@RequestPart("file") MultipartFile file) throws Exception {
        var result = settingsService.uploadAvatar(file);
        return Map.of("objectKey", result.objectKey(), "avatarUrl", result.avatarUrl());
    }

    @PostMapping("/description")
    public void updateDescription(@RequestBody Map<String, String> body) {
        userService.updateDescription(body.get("description"));
    }
}
