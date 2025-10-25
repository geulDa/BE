package com.CUK.geulDa.global.auth.redis;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TempTokenRepository extends CrudRepository<TempToken, String> {
}
