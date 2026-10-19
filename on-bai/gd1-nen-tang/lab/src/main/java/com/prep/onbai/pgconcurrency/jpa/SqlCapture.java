package com.prep.onbai.pgconcurrency.jpa;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Ghi lai moi cau SQL Hibernate sinh ra: test dem duoc N+1 va thay "for update" that. */
public class SqlCapture implements StatementInspector {

    public static final List<String> SQL = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        SQL.add(sql);
        return sql;
    }
}
