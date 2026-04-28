package com.example.mr_backend_wh.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuario", schema = "manuel_romero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Usuario implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rolid", nullable = false)
    private Rol rolid;

    @Column(name = "nomusuario", nullable = false, length = 50, unique = true)
    @EqualsAndHashCode.Include
    private String nomusuario;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "contrasenia", nullable = false, length = 500)
    private String contrasenia;

    @Column(name = "nomcompleto", length = 100)
    private String nomcompleto;

    @ColumnDefault("now()")
    @Column(name = "fechacreacion")
    private OffsetDateTime fechacreacion;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (rolid == null || rolid.getRol() == null) return List.of();
        return List.of(new SimpleGrantedAuthority(rolid.getRol()));
    }

    @Override
    public String getPassword() {
        return this.contrasenia;
    }

    @Override
    public String getUsername() {
        return this.nomusuario;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}