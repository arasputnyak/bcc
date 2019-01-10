package bcc.itso.ru.controller;


import bcc.itso.ru.logic.services.interfaces.MaintenanceService;
import bcc.itso.ru.model.TechnicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sec/maintenance")
public class MaintenanceDatesProviderController {

    @Autowired
    private MaintenanceService maintenanceService;

    @RequestMapping(value = "/getDates", method = {RequestMethod.POST,RequestMethod.GET})
    public List<TechnicalService> getDates(){
      return maintenanceService.getTechnicalServiceResults();
    }

    @RequestMapping(value = "/save", method = {RequestMethod.POST,RequestMethod.GET}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public TechnicalService save(@RequestBody TechnicalService technicalService) {
        TechnicalService technicalServiceTmp = null;
        try {
            technicalServiceTmp = maintenanceService.save(technicalService);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return technicalServiceTmp;
    }
}
