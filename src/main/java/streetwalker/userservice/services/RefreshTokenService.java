package streetwalker.userservice.services;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.RefreshTokenRepository;
import streetwalker.userservice.security.JwtCore;

import java.util.Date;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtCore jwtCore;
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtCore jwtCore) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtCore = jwtCore;
    }
    public RefreshToken create(User refreshTokenUser) throws DataAccessException {
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUsername(refreshTokenUser.getUsername());
        refreshTokenEntity.setToken(jwtCore.generateRefreshToken(refreshTokenUser.getUsername()));
        refreshTokenEntity.setExpiryDate(new Date(System.currentTimeMillis() + jwtCore.getRefreshTokenLifetime()));
        return refreshTokenRepository.save(refreshTokenEntity);
    }
    public String check(String refreshToken) {
        // Проверяем, существует ли refresh token в базе данных
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Проверяем, не истек ли refresh token
        if (storedToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token expired");
        }
        return jwtCore.getUserNameFromJwt(refreshToken);
    }
    public void delete(String refreshToken) throws DataAccessException {
        if (refreshToken != null) {
            refreshTokenRepository.deleteByUsername(jwtCore.getUserNameFromJwt(refreshToken));
        }
    }
    public String getUsernameFromToken(String token) {
        return jwtCore.getUserNameFromJwt(token);
    }
}
