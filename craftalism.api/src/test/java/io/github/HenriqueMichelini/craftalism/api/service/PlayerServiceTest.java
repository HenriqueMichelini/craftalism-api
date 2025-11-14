package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Player Service Tests")
class PlayerServiceTest {

    @Mock
    private PlayerRepository repository;

    @InjectMocks
    private PlayerService service;

    private UUID testUuid;
    private Player testPlayer;

    @BeforeEach
    void setUp() {
        testUuid = UUID.randomUUID();
        testPlayer = new Player();
        testPlayer.setUuid(testUuid);
        testPlayer.setName("TestPlayer");
    }

    @Test
    @DisplayName("Should return player when UUID exists")
    void shouldReturnPlayerWhenUuidExists() {
        when(repository.findById(testUuid)).thenReturn(Optional.of(testPlayer));

        Player result = service.getPlayerByUuid(testUuid);

        assertNotNull(result);
        assertEquals(testUuid, result.getUuid());
        assertEquals("TestPlayer", result.getName());
        verify(repository, times(1)).findById(testUuid);
    }

    @Test
    @DisplayName("Should throw exception when player not found by UUID")
    void shouldThrowExceptionWhenPlayerNotFoundByUuid() {
        UUID nonExistentUuid = UUID.randomUUID();
        when(repository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPlayerByUuid(nonExistentUuid)
        );

        assertTrue(exception.getMessage().contains("Player not found for UUID"));
        assertTrue(exception.getMessage().contains(nonExistentUuid.toString()));
        verify(repository, times(1)).findById(nonExistentUuid);
    }

    @Test
    @DisplayName("Should return player when name exists")
    void shouldReturnPlayerWhenNameExists() {
        String playerName = "TestPlayer";
        when(repository.findByName(playerName)).thenReturn(Optional.of(testPlayer));

        Player result = service.getPlayerByName(playerName);

        assertNotNull(result);
        assertEquals(testUuid, result.getUuid());
        assertEquals("TestPlayer", result.getName());
        verify(repository, times(1)).findByName(playerName);
    }

    @Test
    @DisplayName("Should trim name and return player")
    void shouldTrimNameAndReturnPlayer() {
        String nameWithSpaces = "  TestPlayer  ";
        String trimmedName = "TestPlayer";
        when(repository.findByName(trimmedName)).thenReturn(Optional.of(testPlayer));

        Player result = service.getPlayerByName(nameWithSpaces);

        assertNotNull(result);
        assertEquals("TestPlayer", result.getName());
        verify(repository, times(1)).findByName(trimmedName);
    }

    @Test
    @DisplayName("Should throw exception when player not found by name")
    void shouldThrowExceptionWhenPlayerNotFoundByName() {
        String nonExistentName = "NonExistentPlayer";
        when(repository.findByName(nonExistentName)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPlayerByName(nonExistentName)
        );

        assertTrue(exception.getMessage().contains("Player not found for name"));
        assertTrue(exception.getMessage().contains(nonExistentName));
        verify(repository, times(1)).findByName(nonExistentName);
    }

    @Test
    @DisplayName("Should handle empty name after trim")
    void shouldHandleEmptyNameAfterTrim() {
        String emptyName = "   ";
        String trimmedEmpty = "";
        when(repository.findByName(trimmedEmpty)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPlayerByName(emptyName)
        );

        assertTrue(exception.getMessage().contains("Player not found for name"));
        verify(repository, times(1)).findByName(trimmedEmpty);
    }

    @Test
    @DisplayName("Should create player successfully")
    void shouldCreatePlayerSuccessfully() {
        UUID newUuid = UUID.randomUUID();
        String playerName = "NewPlayer";
        Player savedPlayer = new Player();
        savedPlayer.setUuid(newUuid);
        savedPlayer.setName(playerName);

        when(repository.existsById(newUuid)).thenReturn(false);
        when(repository.save(any(Player.class))).thenReturn(savedPlayer);

        Player result = service.createPlayer(newUuid, playerName);

        assertNotNull(result);
        assertEquals(newUuid, result.getUuid());
        assertEquals(playerName, result.getName());
        verify(repository, times(1)).existsById(newUuid);
        verify(repository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Should trim name when creating player")
    void shouldTrimNameWhenCreatingPlayer() {
        UUID newUuid = UUID.randomUUID();
        String nameWithSpaces = "  NewPlayer  ";
        String trimmedName = "NewPlayer";
        Player savedPlayer = new Player();
        savedPlayer.setUuid(newUuid);
        savedPlayer.setName(trimmedName);

        when(repository.existsById(newUuid)).thenReturn(false);
        when(repository.save(any(Player.class))).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            assertEquals(trimmedName, player.getName());
            return savedPlayer;
        });

        Player result = service.createPlayer(newUuid, nameWithSpaces);

        assertNotNull(result);
        assertEquals(trimmedName, result.getName());
        verify(repository, times(1)).existsById(newUuid);
        verify(repository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Should throw exception when player already exists")
    void shouldThrowExceptionWhenPlayerAlreadyExists() {
        String playerName = "ExistingPlayer";
        when(repository.existsById(testUuid)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPlayer(testUuid, playerName)
        );

        assertTrue(exception.getMessage().contains("Player already exists for UUID"));
        assertTrue(exception.getMessage().contains(testUuid.toString()));
        verify(repository, times(1)).existsById(testUuid);
        verify(repository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should create player with special characters in name")
    void shouldCreatePlayerWithSpecialCharactersInName() {
        UUID newUuid = UUID.randomUUID();
        String specialName = "Player_123!@#";
        Player savedPlayer = new Player();
        savedPlayer.setUuid(newUuid);
        savedPlayer.setName(specialName);

        when(repository.existsById(newUuid)).thenReturn(false);
        when(repository.save(any(Player.class))).thenReturn(savedPlayer);

        Player result = service.createPlayer(newUuid, specialName);

        assertNotNull(result);
        assertEquals(specialName, result.getName());
        verify(repository, times(1)).existsById(newUuid);
        verify(repository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Should create player with very long name")
    void shouldCreatePlayerWithVeryLongName() {
        UUID newUuid = UUID.randomUUID();
        String longName = "A".repeat(100);
        Player savedPlayer = new Player();
        savedPlayer.setUuid(newUuid);
        savedPlayer.setName(longName);

        when(repository.existsById(newUuid)).thenReturn(false);
        when(repository.save(any(Player.class))).thenReturn(savedPlayer);

        Player result = service.createPlayer(newUuid, longName);

        assertNotNull(result);
        assertEquals(longName, result.getName());
        verify(repository, times(1)).existsById(newUuid);
        verify(repository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Should handle name with only spaces being trimmed to empty")
    void shouldHandleNameWithOnlySpacesTrimmedToEmpty() {
        UUID newUuid = UUID.randomUUID();
        String spacesOnlyName = "     ";
        String trimmedName = "";
        Player savedPlayer = new Player();
        savedPlayer.setUuid(newUuid);
        savedPlayer.setName(trimmedName);

        when(repository.existsById(newUuid)).thenReturn(false);
        when(repository.save(any(Player.class))).thenReturn(savedPlayer);

        Player result = service.createPlayer(newUuid, spacesOnlyName);

        assertNotNull(result);
        assertEquals(trimmedName, result.getName());
        verify(repository, times(1)).existsById(newUuid);
        verify(repository, times(1)).save(any(Player.class));
    }
}