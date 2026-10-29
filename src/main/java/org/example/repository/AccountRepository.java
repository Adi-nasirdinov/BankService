package org.example.repository;

import org.example.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Методы findById(Long id) и save(Account account) уже присутствуют в JpaRepository.
    // Объявлять их вручную не нужно.
}