package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.service.AvatarStorageService;
import com.cityscape.geoszabaduloszobabackend.service.SettingsService;
import com.cityscape.geoszabaduloszobabackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {
    @Mock UserService users;
    @Mock AvatarStorageService storage;
    @InjectMocks SettingsService settings;

    @Test void uploadsBeforeSavingAvatarKeyAndPreservesResponse() throws Exception {
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1});
        var user = new UserEntity();
        user.setId(7L);
        when(users.getCurrentUserId()).thenReturn("subject");
        when(users.getOrCreateCurrentUser()).thenReturn(user);
        when(storage.newKey(7L, "png")).thenReturn("key");
        when(storage.publicUrl("key")).thenReturn("url");
        var result = settings.uploadAvatar(file);
        assertThat(result.objectKey()).isEqualTo("key");
        assertThat(result.avatarUrl()).isEqualTo("url");
        var order = inOrder(users, storage);
        order.verify(storage).uploadRaw("key", file, "image/png");
        order.verify(users).updateAvatarKey("subject", "key");
    }

    @Test void failedUploadDoesNotChangeStoredAvatarAndKeepsDefaultExtension() throws Exception {
        var file = new MockMultipartFile("file", "avatar", "image/jpeg", new byte[]{1});
        var user = new UserEntity();
        user.setId(7L);
        when(users.getCurrentUserId()).thenReturn("subject");
        when(users.getOrCreateCurrentUser()).thenReturn(user);
        when(storage.newKey(7L, "jpg")).thenReturn("key");
        doThrow(new Exception("storage unavailable")).when(storage).uploadRaw("key", file, "image/jpeg");
        assertThatThrownBy(() -> settings.uploadAvatar(file)).hasMessage("storage unavailable");
        verify(users, never()).updateAvatarKey(anyString(), anyString());
    }
}
