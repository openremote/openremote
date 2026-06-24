package org.openremote.model.security;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserSessionTest {

    @Test
    public void jackson2DeserializesUserSessionArray() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        UserSession[] sessions = objectMapper.readValue("""
                [{
                    "ID": "session-1",
                    "username": "testuser",
                    "startTimeMillis": 1234,
                    "remoteAddress": "127.0.0.1"
                }]
                """, UserSession[].class);

        assertEquals(1, sessions.length);
        assertEquals("session-1", sessions[0].getID());
        assertEquals("testuser", sessions[0].getUsername());
        assertEquals(1234L, sessions[0].getStartTimeMillis());
        assertEquals("127.0.0.1", sessions[0].getRemoteAddress());
    }
}
