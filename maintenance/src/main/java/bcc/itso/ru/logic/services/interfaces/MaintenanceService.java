package bcc.itso.ru.logic.services.interfaces;

import bcc.itso.ru.model.TechnicalService;

import java.util.List;

public interface MaintenanceService {
    public List<TechnicalService> getTechnicalServiceResults();
    public List<TechnicalService> getMaintenancesByDates(String refix, String reportDateStart,String  reportDateEnd);
    public TechnicalService save(TechnicalService technicalService) throws Exception ;
}
