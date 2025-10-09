package com.prolizwebservices.repository;

import com.prolizwebservices.entity.NotificationToken;
import com.prolizwebservices.entity.NotificationToken.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationToken entity
 */
@Repository
public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {
    
    Optional<NotificationToken> findByToken(String token);
    
    Optional<NotificationToken> findByUserIdAndUserType(String userId, UserType userType);
    
    List<NotificationToken> findByUserIdIn(List<String> userIds);
    
    /**
     * Find all tokens for given user IDs and user type
     * OPTIMIZED: Single query instead of N queries
     */
    List<NotificationToken> findByUserIdInAndUserType(List<String> userIds, UserType userType);
    
    void deleteByToken(String token);
    
    List<NotificationToken> findByUserType(UserType userType);
}
