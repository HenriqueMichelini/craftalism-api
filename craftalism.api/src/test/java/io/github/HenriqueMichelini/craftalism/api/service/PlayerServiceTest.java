package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository repository;

    @InjectMocks
    private PlayerService service;

    @Test
    void getPlayerByUuid_returnsPlayer() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);

        when(repository.findById(uuid)).thenReturn(Optional.of(player));

        Player result = service.getPlayerByUuid(uuid);

        assertSame(player, result);
        verify(repository, times(1)).findById(uuid);
    }

    @Test
    void getPlayerByUuid_notFound_throwsException() {
        UUID uuid = UUID.randomUUID();

        when(repository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class,
                () -> service.getPlayerByUuid(uuid));

        verify(repository, times(1)).findById(uuid);
    }

    @Test
    void getPlayerByName_returnsPlayer() {
        String name = "KOLONY_9";
        Player player = mock(Player.class);

        when(repository.findByName(name)).thenReturn(Optional.of(player));

        Player result = service.getPlayerByName(name);

        assertSame(player, result);
        verify(repository, times(1)).findByName(name.trim());
    }

    @Test
    void getPlayerByName_notFound_throwsException() {
        String name = "GhostPlayer";

        when(repository.findByName(name.trim())).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class,
                () -> service.getPlayerByName(name));

        verify(repository, times(1)).findByName(name.trim());
    }

    @Test
    void createPlayer_success_savesPlayerAndReturnsIt() {
        UUID uuid = UUID.randomUUID();
        String rawName = "   Steve   ";
        String trimmedName = "Steve";

        when(repository.existsById(uuid)).thenReturn(false);

        Player saved = new Player();
        saved.setUuid(uuid);
        saved.setName(trimmedName);

        when(repository.save(any(Player.class))).thenReturn(saved);

        Player result = service.createPlayer(uuid, rawName);

        // returned player is repo return value
        assertSame(saved, result);

        // capture player passed to repo.save()
        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(repository).save(captor.capture());

        Player captured = captor.getValue();
        assertNotNull(captured);
        assertEquals(uuid, captured.getUuid());
        assertEquals(trimmedName, captured.getName());

        verify(repository, times(1)).existsById(uuid);
    }

    @Test
    void createPlayer_existingUUID_throwsException() {
        UUID uuid = UUID.randomUUID();
        String name = "Existing";

        when(repository.existsById(uuid)).thenReturn(true);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> service.createPlayer(uuid, name));

        assertTrue(ex.getMessage().contains("Player already exists"));

        verify(repository, times(1)).existsById(uuid);
        verify(repository, never()).save(any());
    }
}
