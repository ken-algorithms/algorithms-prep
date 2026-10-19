package com.prep.onbai.pgconcurrency.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jpa_customer")
public class Customer {

    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "owner") // LAZY mac dinh: cham vao moi nap -> nguon goc cua N+1
    @OrderBy("id")
    private List<Wallet> wallets = new ArrayList<>();

    protected Customer() {}

    public Customer(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Wallet> getWallets() {
        return wallets;
    }
}
