package dojo.security.l3.notes;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Reference solution: turns on @PreAuthorize/@PostAuthorize processing (a proxy around NoteService). */
@Configuration
@EnableMethodSecurity
public class NoteSecurityConfig {

}
