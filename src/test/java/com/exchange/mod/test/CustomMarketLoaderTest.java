package com.exchange.mod.test;

import com.exchange.mod.core.CustomMarketLoader;
import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
import com.exchange.mod.core.OMSManager;
import com.exchange.mod.db.DatabaseManager;
import com.exchange.mod.db.MarketDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class CustomMarketLoaderTest {

    private MarketManager marketManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        OMSManager omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();
    }

    @Test
    @DisplayName("Should correctly register custom resource into MarketManager and SQLite")
    public void testRegisterCustomResource() throws SQLException {
        MarketResource brass = new MarketResource(
                "create:brass_ingot",
                "Brass Ingot",
                28.0,
                6000.0,
                6000.0,
                0.85,
                10000.0,
                20.0,
                0.02,
                0.20
        );

        marketManager.registerCustomResource(brass);

        MarketResource loaded = marketManager.getResource("create:brass_ingot");
        assertNotNull(loaded, "Custom resource must be retrievable from MarketManager");
        assertEquals("Brass Ingot", loaded.getDisplayName());
        assertEquals(28.0, loaded.getBasePrice(), 0.001);
        assertEquals(6000.0, loaded.getTargetReserve(), 0.001);
        assertEquals(0.85, loaded.getElasticity(), 0.001);

        // Verify it was persisted to SQLite
        MarketResource fromDb = dao.getResource("create:brass_ingot");
        assertNotNull(fromDb, "Custom resource must be saved in database");
        assertEquals(28.0, fromDb.getBasePrice(), 0.001);
    }

    @Test
    @DisplayName("Re-registering existing custom resource should update parameters while preserving existence")
    public void testUpdateCustomResource() throws SQLException {
        MarketResource zinc = new MarketResource(
                "create:zinc_ingot",
                "Zinc Ingot",
                14.0,
                12000.0,
                12000.0,
                0.80,
                18000.0,
                15.0,
                0.02,
                0.10
        );
        marketManager.registerCustomResource(zinc);

        // Update price and targetReserve
        MarketResource zincUpdated = new MarketResource(
                "create:zinc_ingot",
                "Pure Zinc Ingot",
                16.5,
                15000.0,
                12000.0,
                0.82,
                22000.0,
                18.0,
                0.02,
                0.15
        );
        marketManager.registerCustomResource(zincUpdated);

        MarketResource current = marketManager.getResource("create:zinc_ingot");
        assertNotNull(current);
        assertEquals("Pure Zinc Ingot", current.getDisplayName());
        assertEquals(16.5, current.getBasePrice(), 0.001);
        assertEquals(15000.0, current.getTargetReserve(), 0.001);
    }
}
