package com.example.mr_backend_wh.security;

import com.example.mr_backend_wh.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  //Método que a partir de un usuario genere un token
  public String generateToken(Usuario usuario) {
    // Asegurarnos de que rolid no sea null
    String rol = (usuario.getRolid() != null && usuario.getRolid().getRol() != null)
      ? usuario.getRolid().getRol()
      : "USER";

    Map<String, Object> claims = new HashMap<>();
    claims.put("rol", rol);
    claims.put("username", usuario.getNomusuario());

    long now = System.currentTimeMillis();
    long expiration = now + 1000L * 60 * 60 * 3; // 3 horas

    Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

    String token = Jwts.builder()
      .setClaims(claims)
      .setSubject(usuario.getNomusuario())
      .setIssuedAt(new Date(now))
      .setExpiration(new Date(expiration))
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();

    System.out.println("✅ Token generado correctamente: " + token);
    return token;
  }


  //Metodo que sabe la clave de encriptación
  private Key getSignInKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes());
  }

  private Claims extractDatosToken(String token){
    return Jwts
      .parserBuilder()
      .setSigningKey(getSignInKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
  }

  public String extractUsername(String token) {
    try {
      if (token == null) return null;
      Claims claims = extractDatosToken(token);
      // Preferir subject estándar
      if (claims.getSubject() != null) return claims.getSubject();
      // fallback a claim username si existe
      Object username = claims.get("username");
      return username != null ? username.toString() : null;
    } catch (Exception e) {
      System.err.println("❌ Error al extraer username del JWT: " + e.getMessage());
      return null;
    }
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
    final Claims claims = extractDatosToken(token);
    return claimsResolver.apply(claims);
  }

  //Este metodo solo saca la información que va dentro del json, es decir los datos del DTO
  public TokenDataDTO extractTokenData(String token) {
    Claims claims = extractDatosToken(token);

    if (claims == null) {
      throw new RuntimeException("El token es inválido o no contiene claims");
    }

    String username = claims.getSubject() != null ? claims.getSubject() : (String) claims.get("username");
    String rol = claims.get("rol") != null ? claims.get("rol").toString() : null;
    Long fecha_creacion = claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : null;
    Long fecha_expiracion = claims.getExpiration() != null ? claims.getExpiration().getTime() : null;

    return TokenDataDTO.builder()
      .username(username)
      .rol(rol)
      .fecha_creacion(fecha_creacion)
      .fecha_expiracion(fecha_expiracion)
      .build();
  }


  private boolean isTokenExpired(String token){
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token){
    return extractClaim(token, Claims::getExpiration);
  }

  public boolean isTokenValid(String token, Usuario usuario) {
    try {
      final String username = extractUsername(token);
      return (username != null && username.equals(usuario.getUsername())) && !isTokenExpired(token);
    } catch (Exception e) {
      System.err.println("❌ Error en isTokenValid: " + e.getMessage());
      return false;
    }
  }


}
