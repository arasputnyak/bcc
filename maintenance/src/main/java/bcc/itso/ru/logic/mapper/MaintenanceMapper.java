package bcc.itso.ru.logic.mapper;


import bcc.itso.ru.model.Equipment;
import bcc.itso.ru.model.Person;
import bcc.itso.ru.model.TechnicalService;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface MaintenanceMapper {
    @Results(id = "technicalService", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "entryTime", column = "entryTime"),
            @Result(property = "exitTime", column = "exitTime"),
            @Result(property = "jobDescription", column = "jobDescription")
    })
    @Select("SELECT " +
            "  ${currentSchema}public.maintenance.id::varchar as id," +
            "  CASE " +
            "    WHEN ${currentSchema}public.maintenance.entry_time IS NULL" +
            "      THEN ''" +
            "    ELSE  to_char(${currentSchema}public.maintenance.entry_time, 'DD.MM.YYYY HH24:MI')" +
            "  END AS \"entryTime\"," +
            "  CASE " +
            "    WHEN ${currentSchema}public.maintenance.exit_time IS NULL" +
            "      THEN ''" +
            "    ELSE  to_char(${currentSchema}public.maintenance.exit_time, 'DD.MM.YYYY HH24:MI')" +
            "  END AS \"exitTime\"," +
            "  CASE " +
            "    WHEN ${currentSchema}public.maintenance.job_description IS NULL" +
            "      THEN ''" +
            "    ELSE  ${currentSchema}public.maintenance.job_description" +
            "  END AS \"jobDescription\" " +
            //   "  ${currentSchema}public.equipment_types.source " +  // ???
            "FROM  " +
            "${currentSchema}public.maintenance  " +
            "ORDER BY ${currentSchema}public.maintenance.entry_time DESC")
    public List<TechnicalService> getMaintenances(@Param("currentSchema") String currentSchema);

    /* @Results(id="equipment", value = {
             @Result(property = "id", column = "equipment_id"),
             @Result(property = "maintenanceId", column = "maintenance_id")
     })*/
    @ResultType(String.class)
    @Select("SELECT " +
            " ${currentSchema}public.maintenance_equipments.equipment_id::varchar AS \"equipmentId\" " +
            " FROM  ${currentSchema}public.maintenance_equipments" +
            " WHERE " +
            "${currentSchema}public.maintenance_equipments.maintenance_id = '${maintenanceId}'"
    )
    public List<String> getEquipments(@Param("currentSchema") String currentSchema, @Param("maintenanceId") String maintenanceId);

    /*    @Results(id="person", value = {
                @Result(property = "personId", column = "person_id"),
                @Result(property = "maintenanceId", column = "maintenance_id")
        })*/
    @ResultType(String.class)
    @Select("SELECT " +
            " ${currentSchema}public.maintenance_persons.person_id::varchar AS \"personId\" " +
            " FROM " +
            "${currentSchema}public.maintenance_persons" +
            " WHERE " +
            "${currentSchema}public.maintenance_persons.maintenance_id = '${maintenanceId}'"
    )
    public List<String> getPersons(@Param("currentSchema") String currentSchema, @Param("maintenanceId") String maintenanceId);

    @ResultMap("technicalService")
    @Select("SELECT " +
            "  ${prefix}public.maintenance.id::varchar as id," +
            "  CASE " +
            "    WHEN ${prefix}public.maintenance.entry_time IS NULL" +
            "      THEN ''" +
            "    ELSE  to_char(${prefix}public.maintenance.entry_time, 'DD.MM.YYYY HH24:MI')" +
            "  END AS \"entryTime\"," +
            "  CASE " +
            "    WHEN ${prefix}public.maintenance.exit_time IS NULL" +
            "      THEN ''" +
            "    ELSE  to_char(${prefix}public.maintenance.exit_time, 'DD.MM.YYYY HH24:MI')" +
            "  END AS \"exitTime\"," +
            "  CASE " +
            "    WHEN ${prefix}public.maintenance.job_description IS NULL" +
            "      THEN ''" +
            "    ELSE  ${prefix}public.maintenance.job_description" +
            "  END AS \"jobDescription\" " +
            // "  ${prefix}public.equipment_types.source " +
            " FROM " +
            "  ${prefix}public.maintenance" +
            "  WHERE ${prefix}public.maintenance.entry_time BETWEEN '${reportDateStart}' AND '${reportDateEnd}'" +
            "  ORDER BY ${prefix}public.maintenance.entry_time DESC")
    public List<TechnicalService> getMaintenancesByDates(@Param("prefix") String prefix, @Param("reportDateStart") String reportDateStart, @Param("reportDateEnd") String reportDateEnd);

    @Insert("INSERT INTO ${prefix}public.maintenance( entry_time, exit_time, job_description ) VALUES ( '${entryTime}', ${exitTime}, '${jobDescription}' )")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void createTechnicalService(TechnicalService technicalService) throws Exception;

    @Insert("INSERT INTO ${prefix}public.maintenance_equipments ( equipment_id, maintenance_id ) VALUES ( '${equipmentId}', '${maintenanceId}' )")
    public void createEquipment(@Param("prefix") String prefix, @Param("equipmentId") String equipmentId, @Param("maintenanceId") String maintenanceId);

    @Insert("INSERT INTO ${prefix}public.maintenance_persons ( person_id, maintenance_id ) VALUES ( '${personId}', '${maintenanceId}' )")
    //@Options(useGeneratedKeys = true, keyProperty = "id")
    public void createPerson(@Param("prefix") String prefix, @Param("personId") String personId, @Param("maintenanceId") String maintenanceId);

    @Update("UPDATE ${prefix}public.maintenance SET entry_time='${entryTime}', exit_time=${exitTime}, job_description='${jobDescription}' WHERE ${prefix}public.maintenance.id = '${id}'")
    public void updateTechnicalService(TechnicalService technicalService) throws Exception;

    @Delete("DELETE FROM ${prefix}public.maintenance_equipments WHERE maintenance_id = '${maintenanceId}'")
    public void deleteEquipments(@Param("prefix") String prefix, @Param("maintenanceId") String maintenanceId) throws Exception;

    @Delete("DELETE FROM ${prefix}public.maintenance_persons WHERE maintenance_id = '${maintenanceId}'")
    public void deletePersons(@Param("prefix") String prefix, @Param("maintenanceId") String maintenanceId) throws Exception;
}
