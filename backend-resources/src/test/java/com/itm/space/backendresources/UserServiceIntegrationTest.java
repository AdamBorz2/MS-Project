package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.mapper.UserMapper;
import com.itm.space.backendresources.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private Keycloak keycloakClient;

    @MockBean
    private UserMapper userMapper;

    @Value("${keycloak.realm}")
    private String realm;

    @Test
    public void testCreateUser() throws Exception {
        UserRequest userRequest = new UserRequest(
                "johndoe",
                "john.doe@example.com",
                "password123",
                "John",
                "Doe"
        );

        // Настройка mock-объектов
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(userRequest.getPassword());

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userRequest.getUsername());
        userRepresentation.setEmail(userRequest.getEmail());
        userRepresentation.setCredentials(List.of(credentialRepresentation));
        userRepresentation.setEnabled(true);
        userRepresentation.setFirstName(userRequest.getFirstName());
        userRepresentation.setLastName(userRequest.getLastName());

        // Настройка mock-объектов для Keycloak
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(Response.Status.CREATED).build());

        // Вызов метода сервиса
        userService.createUser(userRequest);

        // Проверка вызова метода Keycloak
        verify(usersResource, times(1)).create(any(UserRepresentation.class));
    }

    @Test
    public void testGetUserById() throws Exception {
        UUID userId = UUID.randomUUID();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(userId.toString());
        userRepresentation.setUsername("johndoe");
        userRepresentation.setEmail("john.doe@example.com");
        userRepresentation.setFirstName("John");
        userRepresentation.setLastName("Doe");

        List<RoleRepresentation> userRoles = List.of(new RoleRepresentation("ROLE_USER", null, false));
        GroupRepresentation group = new GroupRepresentation();
        group.setName("GROUP_USER");
        List<GroupRepresentation> userGroups = List.of(group);

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        MappingsRepresentation mappingsRepresentation = mock(MappingsRepresentation.class);
        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);
        when(mappingsRepresentation.getRealmMappings()).thenReturn(userRoles);
        when(userResource.groups()).thenReturn(userGroups);

        UserResponse userResponse = new UserResponse(
                userRepresentation.getFirstName(),
                userRepresentation.getLastName(),
                userRepresentation.getEmail(),
                userRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toList()),
                userGroups.stream().map(GroupRepresentation::getName).collect(Collectors.toList())
        );

        when(userMapper.userRepresentationToUserResponse(userRepresentation, userRoles, userGroups))
                .thenReturn(userResponse);

        // Вызов метода сервиса
        UserResponse response = userService.getUserById(userId);

        // Проверка результата
        assertNotNull(response);
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("john.doe@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertTrue(response.getGroups().contains("GROUP_USER"));
    }
}