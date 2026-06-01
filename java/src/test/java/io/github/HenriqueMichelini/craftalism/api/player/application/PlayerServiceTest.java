package io.github.HenriqueMichelini.craftalism.api.player.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerInUseException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNameAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository repository;

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private PlayerService service;

    @Test
    void getPlayerByUuid_returnsPlayer() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);

        when(repository.findById(uuid)).thenReturn(Optional.of(player));

        Player result = service.getPlayerByUuid(uuid);

        assertSame(player, result);
        verify(repository).findById(uuid);
    }

    @Test
    void getPlayerByUuid_notFound_throwsException() {
        UUID uuid = UUID.randomUUID();

        when(repository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () ->
            service.getPlayerByUuid(uuid)
        );

        verify(repository).findById(uuid);
    }

    @Test
    void getPlayerByName_returnsPlayer() {
        String name = "KOLONY_9";
        Player player = mock(Player.class);

        when(repository.findByName(name)).thenReturn(Optional.of(player));

        Player result = service.getPlayerByName(name);

        assertSame(player, result);
        verify(repository).findByName(name.trim());
    }

    @Test
    void getPlayerByName_notFound_throwsException() {
        String name = "GhostPlayer";

        when(repository.findByName(name.trim())).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () ->
            service.getPlayerByName(name)
        );

        verify(repository).findByName(name.trim());
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

        verify(repository).existsById(uuid);
    }

    @Test
    void createPlayer_existingUUID_throwsException() {
        UUID uuid = UUID.randomUUID();
        String name = "Existing";

        when(repository.existsById(uuid)).thenReturn(true);

        assertThrows(PlayerAlreadyExistsException.class, () ->
            service.createPlayer(uuid, name)
        );

        verify(repository).existsById(uuid);
        verify(repository, never()).save(any());
    }

    @Test
    void createPlayer_existingName_throwsException() {
        UUID uuid = UUID.randomUUID();
        String rawName = " Existing ";
        String trimmedName = "Existing";

        when(repository.existsById(uuid)).thenReturn(false);
        when(repository.findByName(trimmedName)).thenReturn(
            Optional.of(new Player(UUID.randomUUID(), trimmedName))
        );

        assertThrows(PlayerNameAlreadyExistsException.class, () ->
            service.createPlayer(uuid, rawName)
        );

        verify(repository).existsById(uuid);
        verify(repository).findByName(trimmedName);
        verify(repository, never()).save(any());
    }

    @Test
    void updatePlayer_success_updatesNameOnly() {
        UUID uuid = UUID.randomUUID();
        Player player = new Player(uuid, "OldName");

        when(repository.findById(uuid)).thenReturn(Optional.of(player));
        when(repository.findByName("NewName")).thenReturn(Optional.empty());
        when(repository.save(player)).thenReturn(player);

        Player result = service.updatePlayer(uuid, " NewName ");

        assertSame(player, result);
        assertEquals(uuid, result.getUuid());
        assertEquals("NewName", result.getName());
        verify(repository).save(player);
    }

    @Test
    void updatePlayer_sameNameOnSamePlayer_succeeds() {
        UUID uuid = UUID.randomUUID();
        Player player = new Player(uuid, "SameName");

        when(repository.findById(uuid)).thenReturn(Optional.of(player));
        when(repository.findByName("SameName")).thenReturn(Optional.of(player));
        when(repository.save(player)).thenReturn(player);

        Player result = service.updatePlayer(uuid, "SameName");

        assertEquals("SameName", result.getName());
        verify(repository).save(player);
    }

    @Test
    void updatePlayer_existingNameOnDifferentPlayer_throwsException() {
        UUID uuid = UUID.randomUUID();
        Player player = new Player(uuid, "OldName");
        Player other = new Player(UUID.randomUUID(), "TakenName");

        when(repository.findById(uuid)).thenReturn(Optional.of(player));
        when(repository.findByName("TakenName")).thenReturn(Optional.of(other));

        assertThrows(PlayerNameAlreadyExistsException.class, () ->
            service.updatePlayer(uuid, "TakenName")
        );

        verify(repository, never()).save(any());
    }

    @Test
    void deletePlayer_success_deletesAndFlushes() {
        UUID uuid = UUID.randomUUID();
        Player player = new Player(uuid, "DeleteMe");

        when(repository.findById(uuid)).thenReturn(Optional.of(player));
        when(balanceRepository.existsById(uuid)).thenReturn(false);

        service.deletePlayer(uuid);

        verify(balanceRepository).existsById(uuid);
        verify(repository).delete(player);
        verify(repository).flush();
    }

    @Test
    void deletePlayer_referencedByBalance_throwsConflictException() {
        UUID uuid = UUID.randomUUID();
        Player player = new Player(uuid, "InUse");

        when(repository.findById(uuid)).thenReturn(Optional.of(player));
        when(balanceRepository.existsById(uuid)).thenReturn(true);

        assertThrows(PlayerInUseException.class, () ->
            service.deletePlayer(uuid)
        );

        verify(balanceRepository).existsById(uuid);
        verify(repository, never()).delete(any());
        verify(repository, never()).flush();
    }

    @Test
    void deletePlayer_otherReferenceViolation_throwsConflictException() {
        UUID uuid = UUID.randomUUID();
        Player player = new Player(uuid, "InUse");

        when(repository.findById(uuid)).thenReturn(Optional.of(player));
        when(balanceRepository.existsById(uuid)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("fk"))
            .when(repository)
            .flush();

        assertThrows(PlayerInUseException.class, () ->
            service.deletePlayer(uuid)
        );

        verify(repository).delete(player);
        verify(repository).flush();
    }
}
