package com.example.BasicCRM_FWF.Repository;
import com.example.BasicCRM_FWF.Token.Token;
import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {

    @Query("""
    SELECT T FROM Token T INNER JOIN User U ON U.id = T.user.id
    WHERE U.id = :userId AND (T.expired = FALSE OR T.revoked = FALSE)
    """)
    List<Token> findAllTokenByUser(Integer userId);

    Optional<Token> findByToken(String token);

    @Query("""
    SELECT t FROM Token t LEFT JOIN FETCH t.user WHERE t.token = :token
    """)
    Optional<Token> findTokenByToken(@Param("token") String token);

    @Query("""
    SELECT t FROM Token t WHERE t.user.id = :id and t.expired = FALSE
""")
    Optional<Token> findTokenByUserId(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM Token T
    WHERE T.user.id = :userId AND T.expired = FALSE AND T.revoked = FALSE
    """)
    void deleteExpiredAndRevokedTokensByUserId(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.expired = true OR t.revoked = true")
    int deleteExpiredOrRevokedTokens();
}
