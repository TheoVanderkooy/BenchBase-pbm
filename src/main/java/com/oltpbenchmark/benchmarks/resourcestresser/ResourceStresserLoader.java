package com.oltpbenchmark.benchmarks.resourcestresser;

import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.catalog.Table;
import com.oltpbenchmark.util.SQLUtil;
import com.oltpbenchmark.util.TextGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResourceStresserLoader extends Loader<ResourceStresserBenchmark> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceStresserLoader.class);
    private final int numEmployees;

    public ResourceStresserLoader(ResourceStresserBenchmark benchmark, Connection conn) {
        super(benchmark, conn);
        this.numEmployees = (int) (this.scaleFactor * ResourceStresserConstants.RECORD_COUNT);
        if (LOG.isDebugEnabled()) {
            LOG.debug("# of EMPLOYEES:  {}", this.numEmployees);
        }
    }

    @Override
    public List<LoaderThread> createLoaderThreads() throws SQLException {
        List<LoaderThread> threads = new ArrayList<LoaderThread>();
        threads.add(new LoaderThread() {
            @Override
            public void load(Connection conn) throws SQLException {
                loadTable(conn, ResourceStresserConstants.TABLENAME_CPUTABLE);
            }
        });
        threads.add(new LoaderThread() {
            @Override
            public void load(Connection conn) throws SQLException {
                loadTable(conn, ResourceStresserConstants.TABLENAME_IOTABLE);
            }
        });
        threads.add(new LoaderThread() {
            @Override
            public void load(Connection conn) throws SQLException {
                loadTable(conn, ResourceStresserConstants.TABLENAME_IOTABLESMALLROW);
            }
        });
        threads.add(new LoaderThread() {
            @Override
            public void load(Connection conn) throws SQLException {
                loadTable(conn, ResourceStresserConstants.TABLENAME_LOCKTABLE);
            }
        });
        return (threads);
    }

    private void loadTable(Connection conn, String tableName) throws SQLException {
        Table catalog_tbl = this.benchmark.getTableCatalog(tableName);


        if (LOG.isDebugEnabled()) {
            LOG.debug("Start loading {}", tableName);
        }
        String sql = SQLUtil.getInsertSQL(catalog_tbl, this.getDatabaseType());
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int batch = 0;
            int i;
            for (i = 0; i < this.numEmployees; ++i) {
                stmt.setInt(1, i);
                if (tableName.equals(ResourceStresserConstants.TABLENAME_CPUTABLE)) {
                    stmt.setString(2, TextGenerator.randomStr(rng(), ResourceStresserConstants.STRING_LENGTH));
                } else if (tableName.equals(ResourceStresserConstants.TABLENAME_IOTABLE)) {
                    for (int j = 2; j <= catalog_tbl.getColumnCount(); ++j) {
                        stmt.setString(j, TextGenerator.randomStr(rng(), ResourceStresserConstants.STRING_LENGTH));
                    }
                } else {
                    stmt.setInt(2, rng().nextInt());
                }

                stmt.addBatch();
                if (++batch >= ResourceStresserConstants.COMMIT_BATCH_SIZE) {
                    int result[] = stmt.executeBatch();

                    batch = 0;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Records Loaded %d / %d", i + 1, this.numEmployees));
                    }
                }
            } // FOR
            if (batch > 0) {
                stmt.executeBatch();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Records Loaded %d / %d", i, this.numEmployees));
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finished loading {}", tableName);
        }
        return;
    }

}
