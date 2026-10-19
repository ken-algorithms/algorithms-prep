package com.prep.onbai.pgconcurrency.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("select c from Customer c order by c.id")
    List<Customer> findAllPlain();

    /** Mot cau select ... left join: nap san wallets, het N+1. */
    @EntityGraph(attributePaths = "wallets")
    @Query("select c from Customer c order by c.id")
    List<Customer> findAllWithWallets();
}
