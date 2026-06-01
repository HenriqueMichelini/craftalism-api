package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.dto.PlayerRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.PlayerUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.PlayerMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.player.application.PlayerService;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private PlayerService service;

    @Mock
    private PlayerMapper mapper;

    @InjectMocks
    private PlayerController controller;

    @Test
    void getPlayerByUuid_returnsOkAndMappedDto() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        PlayerResponseDTO dto = mock(PlayerResponseDTO.class);

        when(service.getPlayerByUuid(uuid)).thenReturn(player);
        when(mapper.toDto(player)).thenReturn(dto);

        ResponseEntity<PlayerResponseDTO> resp = controller.getPlayerByUuid(
            uuid
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service, times(1)).getPlayerByUuid(uuid);
        verify(mapper, times(1)).toDto(player);
    }

    @Test
    void getPlayerByName_returnsOkAndMappedDto() {
        String name = "KOLONY_9";
        Player player = mock(Player.class);
        PlayerResponseDTO dto = mock(PlayerResponseDTO.class);

        when(service.getPlayerByName(name)).thenReturn(player);
        when(mapper.toDto(player)).thenReturn(dto);

        ResponseEntity<PlayerResponseDTO> resp = controller.getPlayerByName(
            name
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service, times(1)).getPlayerByName(name);
        verify(mapper, times(1)).toDto(player);
    }

    @Test
    void createPlayer_returnsCreatedWithLocationAndBody() {
        UUID uuid = UUID.randomUUID();
        String name = "NEW_PLAYER";

        PlayerRequestDTO req = mock(PlayerRequestDTO.class);
        Player created = mock(Player.class);
        PlayerResponseDTO dto = mock(PlayerResponseDTO.class);

        when(req.uuid()).thenReturn(uuid);
        when(req.name()).thenReturn(name);

        when(service.createPlayer(uuid, name)).thenReturn(created);
        when(created.getUuid()).thenReturn(uuid);
        when(mapper.toDto(created)).thenReturn(dto);

        ResponseEntity<PlayerResponseDTO> resp = controller.createPlayer(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        URI location = resp.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals("/api/players/" + uuid.toString(), location.toString());

        verify(service, times(1)).createPlayer(uuid, name);
        verify(mapper, times(1)).toDto(created);
    }

    @Test
    void updatePlayer_returnsOkAndMappedDto() {
        UUID uuid = UUID.randomUUID();
        String name = "UPDATED";

        PlayerUpdateRequestDTO req = mock(PlayerUpdateRequestDTO.class);
        Player updated = mock(Player.class);
        PlayerResponseDTO dto = mock(PlayerResponseDTO.class);

        when(req.name()).thenReturn(name);
        when(service.updatePlayer(uuid, name)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<PlayerResponseDTO> resp = controller.updatePlayer(
            uuid,
            req
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(service).updatePlayer(uuid, name);
        verify(mapper).toDto(updated);
    }

    @Test
    void deletePlayer_returnsNoContent() {
        UUID uuid = UUID.randomUUID();

        ResponseEntity<Void> resp = controller.deletePlayer(uuid);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        assertNull(resp.getBody());
        verify(service).deletePlayer(uuid);
    }
}
