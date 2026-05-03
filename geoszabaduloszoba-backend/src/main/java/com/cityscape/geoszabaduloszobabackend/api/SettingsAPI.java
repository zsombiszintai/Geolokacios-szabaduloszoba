package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.service.AvatarStorageService;
import com.cityscape.geoszabaduloszobabackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsAPI {

    private final AvatarStorageService storageService;
    private final UserService userService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadAvatar(@RequestPart("file") MultipartFile file,
                                            @AuthenticationPrincipal Jwt jwt) throws Exception {

        String sub = jwt.getSubject();
        String ext = "jpg";
        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
            ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1);
        }

        var user = userService.getOrCreateCurrentUser();
        String key = storageService.newKey(user.getId(), ext);

        storageService.uploadRaw(key, file, file.getContentType());

        userService.updateAvatarKey(sub, key);

        return Map.of(
                "objectKey", key,
                "avatarUrl", storageService.publicUrl(key)
        );
    }
}
