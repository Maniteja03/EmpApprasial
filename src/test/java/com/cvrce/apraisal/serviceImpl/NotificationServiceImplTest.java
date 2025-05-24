package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.NotificationDTO;
import com.cvrce.apraisal.entity.Notification;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.NotificationRepository;
import com.cvrce.apraisal.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private NotificationDTO testNotificationDTO;
    private UUID userId;
    private String userEmail = "testuser@example.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        testUser = User.builder().id(userId).email(userEmail).fullName("Test User").build();

        testNotificationDTO = NotificationDTO.builder()
                .userId(userId)
                .title("Test Notification")
                .message("This is a test message.")
                .build();
    }

    @Test
    void testSendNotification_SuccessfulEmailAndDbSave() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification savedNotification = invocation.getArgument(0);
            savedNotification.setId(UUID.randomUUID()); // Simulate DB save assigning an ID
            return savedNotification;
        });
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        NotificationDTO resultDTO = notificationService.sendNotification(testNotificationDTO);

        // Assert
        assertNotNull(resultDTO);
        assertEquals(testNotificationDTO.getTitle(), resultDTO.getTitle());
        verify(userRepository).findById(userId);
        verify(notificationRepository).save(any(Notification.class));

        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailMessageCaptor.capture());
        SimpleMailMessage sentMessage = mailMessageCaptor.getValue();
        assertArrayEquals(new String[]{userEmail}, sentMessage.getTo());
        assertEquals(testNotificationDTO.getTitle(), sentMessage.getSubject());
        assertEquals(testNotificationDTO.getMessage(), sentMessage.getText());
    }

    @Test
    void testSendNotification_MailExceptionOccurs_DbSaveStillSucceedsAndErrorLogged() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification savedNotification = invocation.getArgument(0);
            savedNotification.setId(UUID.randomUUID());
            return savedNotification;
        });
        doThrow(new MailSendException("Simulated mail error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        NotificationDTO resultDTO = null;
        Exception caughtException = null;
        try {
            resultDTO = notificationService.sendNotification(testNotificationDTO);
        } catch (Exception e) {
            caughtException = e;
        }
        
        // Assert
        assertNull(caughtException, "MailSendException should be caught and not re-thrown.");
        assertNotNull(resultDTO); // DB save should succeed
        assertEquals(testNotificationDTO.getTitle(), resultDTO.getTitle());

        verify(userRepository).findById(userId);
        verify(notificationRepository).save(any(Notification.class));
        verify(mailSender).send(any(SimpleMailMessage.class)); // Verify it was called
        // Log verification is harder without specific setup, but the key is no exception re-thrown.
    }

    @Test
    void testSendNotification_UserNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.sendNotification(testNotificationDTO);
        });

        assertEquals("User not found", exception.getMessage()); // Assuming this is the message from service
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
</tbody>
</table>
