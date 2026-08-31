package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {

    Optional<Module> findByCodeModule(String codeModule);

    boolean existsByCodeModule(String codeModule);
}
