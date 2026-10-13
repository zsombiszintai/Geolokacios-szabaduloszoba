package com.cityscape.geoszabaduloszobabackend.service;
import com.cityscape.geoszabaduloszobabackend.model.domain.AvatarUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final UserService userService;
    private final AvatarStorageService storageService;

    public AvatarUploadResult uploadAvatar(MultipartFile file) throws Exception {
        String sub = userService.getCurrentUserId();
        String extension = "jpg";
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.') + 1);
        }
        var user = userService.getOrCreateCurrentUser();
        String key = storageService.newKey(user.getId(), extension);
        storageService.uploadRaw(key, file, file.getContentType());
        userService.updateAvatarKey(sub, key);
        return new AvatarUploadResult(key, storageService.publicUrl(key));
    }
}
