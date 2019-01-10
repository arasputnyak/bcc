package bcc.itso.ru.logic.services.implement;

import bcc.itso.ru.facade.DictionaryStore;
import bcc.itso.ru.logic.mapper.MaintenanceMapper;
import bcc.itso.ru.logic.services.interfaces.MaintenanceService;
import bcc.itso.ru.model.Equipment;
import bcc.itso.ru.model.Person;
import bcc.itso.ru.model.TechnicalService;
import bcc.itso.ru.utils.BasicServicePrefixKK;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("maintenanceServiceImpl")
public class MaintenanceServiceImpl implements MaintenanceService {
    protected static transient Logger logger = LoggerFactory.getLogger(MaintenanceServiceImpl.class);
    private static final String SCHEMA_PREFIX = BasicServicePrefixKK.getInstance().getCurrentSchemaPrefixStatic();

    @Autowired
    private DictionaryStore dictionaryStore;

    @Autowired
    private MaintenanceMapper maintenanceMapper;

    public List<TechnicalService> getMaintenancesByDates(String prefix, String reportDateStart, String reportDateEnd) {
        List<TechnicalService> technicalServices = maintenanceMapper.getMaintenancesByDates(prefix, reportDateStart, reportDateEnd);
        processing(prefix, technicalServices);
        return technicalServices;
    }

    public List<TechnicalService> getTechnicalServiceResults() {
        List<TechnicalService> technicalServices = maintenanceMapper.getMaintenances(SCHEMA_PREFIX);
        processing(SCHEMA_PREFIX,technicalServices);
        return technicalServices;
    }

    private void processing(String prefix,List<TechnicalService> technicalServices) {
        if (technicalServices != null && technicalServices.size() > 0) {
            technicalServices.parallelStream().forEach(technicalService -> {

                technicalService.setEdit(false);
                technicalService.setPrefix(prefix);
                if (technicalService.getExitTime() == null || technicalService.getExitTime().trim().length() <= 0) {
                    technicalService.setEdit(true);
                }
                if (technicalService.getJobDescription() == null || technicalService.getJobDescription().trim().trim().length() <= 0) {
                    technicalService.setEdit(true);
                }

                List<String> equipmentIds = maintenanceMapper.getEquipments(prefix, technicalService.getId());
                List<Equipment> equipments = new ArrayList<>();
                equipmentIds.parallelStream().forEach(equipmentId -> {
                    equipments.add(dictionaryStore.getEquipmentById(equipmentId));
                });

                List<Person> persons = new ArrayList<>();
                List<String> personIds = maintenanceMapper.getPersons(prefix, technicalService.getId());
                personIds.parallelStream().forEach(personId -> {
                    persons.add(dictionaryStore.getPersonById(personId));
                });

                technicalService.setEquipments(equipments);
                technicalService.setPersons(persons);

                if (technicalService.getEquipments() == null || technicalService.getEquipments().size() <= 0) {
                    technicalService.setEdit(true);
                }
                if (technicalService.getPersons() == null || technicalService.getPersons().size() <= 0) {
                    technicalService.setEdit(true);
                }

            });
        }
    }

    public TechnicalService save(TechnicalService technicalService) throws Exception {
        technicalService.setPrefix(SCHEMA_PREFIX);

        if (technicalService.getExitTime() != null)
            technicalService.setExitTime("'" + technicalService.getExitTime() + "'");
        else technicalService.setExitTime("NULL");

        return saveToBD(technicalService);
    }

    private TechnicalService saveToBD(TechnicalService technicalService) throws Exception {

        if (technicalService.getId().trim().length() <= 0) {
            maintenanceMapper.createTechnicalService(technicalService);
        } else {
            maintenanceMapper.updateTechnicalService(technicalService);
        }

        String maintenanceId = technicalService.getId();
        maintenanceMapper.deleteEquipments(SCHEMA_PREFIX, maintenanceId);
        maintenanceMapper.deletePersons(SCHEMA_PREFIX, maintenanceId);

        List<String> equipmentIds = technicalService.getEquipmentIds();
        List<String> personIds = technicalService.getPersonIds();

        List<Equipment> equipments = new ArrayList<>();
        List<Person> persons = new ArrayList<>();

        if (equipmentIds != null && equipmentIds.size() > 0) {
            equipmentIds.parallelStream().forEach(equipmentId -> {
                maintenanceMapper.createEquipment(SCHEMA_PREFIX, equipmentId, maintenanceId);
                equipments.add(dictionaryStore.getEquipmentById(equipmentId));
            });
        }

        if (personIds != null && personIds.size() > 0) {
            personIds.parallelStream().forEach(personId -> {
                maintenanceMapper.createPerson(SCHEMA_PREFIX, personId, maintenanceId);
                persons.add(dictionaryStore.getPersonById(personId));
            });
        }

        technicalService.setEquipments(equipments);
        technicalService.setPersons(persons);

        return technicalService;
    }
}