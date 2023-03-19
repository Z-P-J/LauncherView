package com.android.launcher3.database;

import java.util.HashMap;
import java.util.Map;

public class SQLite {

    private static final Map<String, IDatabase> sDBMap = new HashMap<>();

    static {
        sDBMap.put(HomepageDatabase.DATABASE_NAME, new HomepageDatabase());
    }

    public static <T extends ITable> T with(Class<T> clazz) {
        for (IDatabase database : sDBMap.values()) {
            ITable table = database.getTable(clazz);
            if (table != null) {
                return clazz.cast(table);
            }
        }
        throw new RuntimeException("Invalidate table! table class is " + clazz);
    }

    static IDatabase getDatabase(String dbName) {
        return sDBMap.get(dbName);
    }

    static void putDatabase(String dbName, IDatabase database) {
        sDBMap.put(dbName, database);
    }

}
