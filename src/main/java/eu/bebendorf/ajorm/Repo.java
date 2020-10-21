package eu.bebendorf.ajorm;

import eu.bebendorf.ajorm.exception.AJORMConfigurationException;
import eu.bebendorf.ajorm.util.MigrationTool;
import eu.bebendorf.ajorm.wrapper.SQL;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;

public class Repo<T extends Model> {

    public static <T extends Model> Repo<T> get(Class<T> model){
        return AJORM.repo(model);
    }

    private final TableInfo info;
    private SQL connection;

    public Repo(Class<T> clazz, SQL connection, AJORMConfig config) throws AJORMConfigurationException {
        this.info = new TableInfo(clazz, config);
        this.connection = connection;
    }

    public QueryBuilder<T> query(){
        return new QueryBuilder<>(this);
    }

    public QueryBuilder<T> where(String key, String op, Object value){
        return query().where(key, op, value);
    }

    public QueryBuilder<T> where(String key, Object value){
        return query().where(key, value);
    }

    public void save(T entry){
        if(entry.doesEntryExist()){
            update(entry);
        }else{
            create(entry);
        }
    }

    public void create(T entry){
        query().create(entry);
    }

    public void update(T entry){
        where(info.getIdField(), getId(entry)).update(entry);
    }

    public void delete(T entry){
        Timestamp timestamp = where(info.getIdField(), getId(entry)).delete();
        if(timestamp != null){
            try {
                info.getField(info.getSoftDeleteField()).set(entry, timestamp);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void restore(T entry){
        if(!info.isSoftDelete())
            return;
        where(info.getIdField(), getId(entry)).restore();
        try {
            info.getField(info.getSoftDeleteField()).set(entry, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void finalDelete(T entry){
        where(info.getIdField(), getId(entry)).finalDelete();
    }

    public T refresh(T entry){
        return where(info.getIdField(), getId(entry)).refresh(entry);
    }

    public T get(Object id){
        return where(info.getIdField(), id).get();
    }

    public List<T> all(){
        return query().all();
    }

    public Stream<T> stream(){
        return query().stream();
    }

    public int count(){
        return query().count();
    }

    private Object getId(T entry){
        if(entry == null)
            return null;
        try {
            Object id = info.getField(info.getIdField()).get(entry);
            if(id == null)
                return null;
            if(id.getClass().equals(Integer.class)){
                int intId = (Integer) id;
                if(intId == 0)
                    return null;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void migrate(){
        MigrationTool.migrate(connection, info);
    }

    public SQL getConnection(){
        return connection;
    }

    public TableInfo getInfo(){
        return info;
    }

}
