package com.example.customers;

import org.jdbscript.IDBSchema;
import org.jdbscript.IDBSchema.IDBRecord;
import org.jdbscript.JDBMigrationEngine;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CustomerStatusMigrationTest {

    private interface IBeforeSchema extends IDBSchema {
        ICustomerRecord customer();

        interface ICustomerRecord extends IDBRecord {
            ICustomerRecord id(int value);
            ICustomerRecord name(String value);
            ICustomerRecord status(String value);
        }
    }

    private interface IAfterSchema extends IDBSchema {
        ICustomerStatusRecord customer_status();
        ICustomerRecord customer();

        interface ICustomerStatusRecord extends IDBRecord {
            ICustomerStatusRecord id(int value);
            ICustomerStatusRecord code(String value);
        }

        interface ICustomerRecord extends IDBRecord {
            ICustomerRecord id(int value);
            ICustomerRecord name(String value);
            ICustomerRecord status_id(int value);
        }
    }

    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
    private JDBMigrationEngine<IBeforeSchema, IAfterSchema> migration;

    @BeforeClass
    public void startDatabase() {
        postgres.start();
        migration = JDBMigrationEngine.builder(IBeforeSchema.class, IAfterSchema.class)
                .dataSource(dataSource())
                .migrations("classpath:db/migration")
                .build();
    }

    @BeforeMethod
    public void startFromBlankDatabase() {
        migration.reset();
    }

    @Test
    public void every_customer_keeps_its_status() {
        migration.migrateBefore("2");
        migration.before().insertDB(db -> {
            db.customer().id(1).name("Acme").status("ACTIVE");
            db.customer().id(2).name("Globex").status("INACTIVE");
            db.customer().id(3).name("Initech").status("active");
            db.customer().id(4).name("Umbrella").status("Active "); // trailing space
        });

        migration.migrateTo("4");

        migration.after().assertDBHas(db -> {
            db.customer_status().id(1).code("ACTIVE");
            db.customer_status().id(2).code("INACTIVE");
            db.customer().id(1).status_id(1);
            db.customer().id(2).status_id(2);
            db.customer().id(3).status_id(1);
            db.customer().id(4).status_id(1);
        });
    }

    @Test
    public void customer_without_known_status_becomes_inactive() {
        migration.migrateBefore("2");
        migration.before().insertDB(db -> {
            db.customer().id(1).name("Acme").status("ACTIVE");
            db.customer().id(2).name("Hooli").status(null);
            db.customer().id(3).name("Wayne").status("SUSPENDED");
        });

        migration.migrateTo("4");

        migration.after().assertDBHas(db -> {
            db.customer().id(1).status_id(1);
            db.customer().id(2).status_id(2);
            db.customer().id(3).status_id(2);
        });
    }

    @AfterClass(alwaysRun = true)
    public void stopDatabase() {
        postgres.stop();
    }

    private PGSimpleDataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }
}
