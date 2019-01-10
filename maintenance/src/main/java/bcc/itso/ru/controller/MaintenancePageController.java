package bcc.itso.ru.controller;

import bcc.itso.ru.model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/sec/maintenance")
public class MaintenancePageController {
    @RequestMapping(value = "/technicalServiceResult", method = {RequestMethod.GET, RequestMethod.POST})
    public String getTechResultTable() {
        return "newTechnicalResults.html";
    }

    @RequestMapping(value = "/commonFuncs", method = {RequestMethod.GET, RequestMethod.POST})
    public String getCommonFuncs() {
        return "commonfunctions.js";
    }

    @RequestMapping(value = "/controller", method = {RequestMethod.GET, RequestMethod.POST})
    public String getController() {
        return "controller.js";
    }

}
