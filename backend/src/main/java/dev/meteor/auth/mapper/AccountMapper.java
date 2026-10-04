package dev.meteor.auth.mapper;

import dev.meteor.auth.mapper.projection.AccountCredentialsProjection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

public interface AccountMapper {

    Optional<AccountCredentialsProjection> findByEmail(@Param("email") String email);

    List<String> findRolesByUserId(@Param("userId") long userId);
}
