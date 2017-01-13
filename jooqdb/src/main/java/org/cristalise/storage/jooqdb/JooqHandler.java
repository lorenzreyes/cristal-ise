package org.cristalise.storage.jooqdb;

import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.jooq.DSLContext;

public interface JooqHandler {
    /**
     * 
     * @param context
     */
    public void createTables(DSLContext context);

    /**
     * 
     * @param context
     * @param primaryKeys
     * @return
     */
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys);

    /**
     * 
     * @param context
     * @param uuid
     * @param obj
     * @return
     */
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj);
    
    /**
     * 
     * @param context
     * @param uuid
     * @param obj
     * @return
     */
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj);

    /**
     * 
     * @param context
     * @param uuid
     * @param primaryKeys
     */
    public void delete(DSLContext context, UUID uuid, String...primaryKeys);

    /**
     * 
     * @param context
     * @param uuid
     * @param obj
     * @return
     */
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj);

    /**
     * 
     * @param context
     * @param uuid
     * @param id
     * @return
     */
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys);
}
