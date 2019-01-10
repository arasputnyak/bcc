'use strict';
;(function ($) {
    $.extend({
        dateRangePickerSettings: {
            singleDatePicker: true,
            timePicker: true,
            timePicker24Hour: true,
            autoUpdateInput: false,
            locale: {
                format: "DD.MM.YYYY HH:mm"
            }
        },


        load: function () {
            moment.locale('ru');
            $('#maintenanceTableId').bootstrapTable({
                height: $.getHeight(),
                width: $.getWidth(),
                silent: true,
                singleSelect: true
            });

            $('#entryTimeId').attr('readOnly', 'true');
            $('#exitTimeId').keypress(function (event) {
                event.preventDefault();
            });

            $.getOptionsEquip();
            $.getOptionsPers();
        },
        getHeight: function () {
            return $('#maintenanceContainer').actual('height') - 42;
        },
        getWidth: function () {
            return $('#maintenanceContainer').actual('width');
        },


        editRow: function (rowToChange) {
            $.createEntryDaterangePicker();
            if (rowToChange != null) {
                document.getElementById('entryTimeId').style.borderColor = '#DCDCDC';
                if (rowToChange.edit) {
                    $('#idRecord').val(rowToChange.id);
                    $('#jobDescriptionId').val(rowToChange.jobDescription);

                    $('#entryTimeId').val(rowToChange.entryTime);

                    if ($('#exitTimeId').data('daterangepicker') == undefined) {
                        $.createExitDaterangePicker($('#entryTimeId').val());
                    }

                    $('#exitTimeId').val(rowToChange.exitTime);

                    let workers = [];
                    rowToChange.persons.forEach(function (person) {
                        workers.push(person.personId);
                    });
                    $('#workerSelectorId').val(workers).trigger('change');

                    let equipments = [];
                    rowToChange.equipments.forEach(function (equip) {
                        equipments.push(equip.id);
                    });
                    $('#equipmentSelectorId').val(equipments).trigger('change');

                } else {
                    $('#idRecordRO').val(rowToChange.id);

                    $('.entry-div').append('<div class="border-time">' + rowToChange.entryTime + '</div>');
                    $('.exit-div').append('<div class="border-time">' + rowToChange.exitTime + '</div>');
                    $('.jobDiscr-div').append('<div class="border-scroll">' + rowToChange.jobDescription + '</div>');

                    let workers = rowToChange.persons;
                    let workerVal = '';
                    for (let i = 0; i < workers.length; i++) {
                        workerVal += workers[i].fullName + ', ' + workers[i].organization + (i == (workers.length - 1) ? '' : ('; ' + '</br>'));
                    }

                    let equipments = rowToChange.equipments;
                    let equipVal = '';
                    for (let i = 0; i < equipments.length; i++) {
                        equipVal += equipments[i].name + ', ' + equipments[i].location + (i == (equipments.length - 1) ? '' : ('; ' + '</br>'));
                    }

                    $('.equip-div').append('<div class="border-scroll">' + equipVal + '</div>');
                    $('.worker-div').append('<div class="border-scroll">' + workerVal + '</div>');

                    $('#maintenanceModalRO').modal('show');
                    return;
                }
            } else {
                $.clearData();
                document.getElementById('entryTimeId').style.borderColor = 'red';
            }
            $('#maintenanceModal').modal('show');
        },
        saveForm: function () {
            if ($.checkFields()) {
                let rowData = $.getData();
                console.error(rowData);

                let edit = false;
                for (let key in rowData) {
                    if ((rowData[key] == '' || rowData[key] == null) && key != 'id') {
                        edit = true;
                        break;
                    }
                }

                if (!edit) {
                    $.confirm({
                        'title': 'Внимание!',
                        'message': 'Редактирование будет запрещено после сохранения.',
                        'buttons': {
                            'Отменить': {
                                class: '_blue col-sm-3 pull-right',
                                action: function () {
                                    $('#confirmOverlay').modal('hide');
                                }
                            },
                            'Принять': {
                                class: '_blue col-sm-4 pull-right',
                                action: function () {
                                    $('#confirmOverlay').modal('hide');
                                    $('#maintenanceModal').modal('hide');
                                    $.insertRow(rowData);
                                    $.removeEntryDaterangePicker();
                                    $.removeExitDaterangePicker();
                                }
                            }
                        }
                    });
                } else {
                    $.insertRow(rowData);
                    $('#maintenanceModal').modal('hide');
                    $.removeEntryDaterangePicker();
                    $.removeExitDaterangePicker();
                }
            } else {
                $.confirm({
                    'title': 'Внимание!',
                    'message': 'Необходимо заполнить обязательные поля (*).',
                    'buttons': {
                        'Ок': {
                            class: '_blue col-sm-4 pull-right',
                            action: function () {
                                $('#confirmOverlay').modal('hide');
                            }
                        }
                    }
                });
            }
        },
        cancel: function () {
            $('#maintenanceModal').modal('hide');
            $.clearData();
            $.removeEntryDaterangePicker();
            $.removeExitDaterangePicker();
        },
        checkFields: function () {
            let check = $('.required').get().some(function (element, index, array) {
                return element.value == '';
            });
            return !check;
        },
        compareDates: function (val1, val2) {
            if (val1 == '' || val2 == '') return true;

            let startDate = moment(val1, 'DD.MM.YYYY HH:mm');
            let endDate = moment(val2, 'DD.MM.YYYY HH:mm');

            return endDate.isAfter(startDate);
        },
        getData: function () {
            let rowId = $('#idRecord').val();
            let entryTime = $('#entryTimeId').val();
            let exitTime = $('#exitTimeId').val() == '' ? null : $('#exitTimeId').val();
            let jobDescription = $('#jobDescriptionId').val();

            let equipments = $('#equipmentSelectorId').val() == '' ? null : $('#equipmentSelectorId').val();
            let workers = $('#workerSelectorId').val() == '' ? null : $('#workerSelectorId').val();

            let rowData = {
                id: rowId,
                entryTime: entryTime,
                exitTime: exitTime,
                jobDescription: jobDescription,
                equipmentIds: equipments,
                personIds: workers
            };
            return rowData;
        },
        insertRow: function (rowData) {
            $.ajax({
                type: 'POST',
                url: '/itso/sec/maintenance/save',
                contentType: 'application/json; charset=utf-8',
                data: JSON.stringify(rowData)
            }).done(function (response) {
                if (response == null) {
                    $.confirm({
                        'title': 'Внимание!',
                        'message': 'Ошибка на сервере, обратитесь к системному администратору.',
                        'buttons': {
                            'Ок': {
                                class: '_blue col-sm-4 pull-right',
                                action: function () {
                                    $('#confirmOverlay').modal('hide');
                                }
                            }
                        }
                    });
                    return;
                }

                let rowToInsert = $('#maintenanceTableId').bootstrapTable('getRowByUniqueId', response.id);
                if (rowToInsert == null) {
                    $('#maintenanceTableId').bootstrapTable('insertRow', response);
                } else {
                    rowToInsert.entryTime = response.entryTime;
                    rowToInsert.exitTime = response.exitTime;
                    rowToInsert.edit = response.edit;
                    rowToInsert.jobDescription = response.jobDescription;
                    rowToInsert.prefix = response.prefix;
                    rowToInsert.equipments = response.equipments;
                    rowToInsert.persons = response.persons;
                }
                $('#maintenanceTableId').bootstrapTable('refresh');
                console.error(response);
            });
        },
        getOptionsPers: function () {
            let s = $('#workerSelectorId');
            $.ajax({
                type: 'get',
                url: '/itso/sec/dict/getPersons'
            }).done(function (workers) {
                let selected = document.getElementById('workerSelectorId');
                for (let i = 0; i < workers.length; i++) {
                    let option = document.createElement('option');
                    let worker = workers[i].fullName + ', ' + workers[i].organization;

                    option.value = workers[i].personId;
                    option.text = worker;
                    $(option).attr('workerId', workers[i].personId);
                    $(option).attr('fullname', workers[i].fullName);
                    $(option).attr('organization', workers[i].organization);

                    selected.add(option);
                }
                s.select2({
                    placeholder: 'Выберите сотрудника',
                    multiple: true,
                    allowClear: true
                });

                s.on('select2:select', function (e) {
                    let selected = s.val();
                    if (selected[0] == '') {
                        selected.shift();
                    }
                    s.val(selected).trigger('change');
                });
            }).fail(function () {
                alert('ALARM_Pers');
            });

        },
        getOptionsEquip: function () {
            let s = $('#equipmentSelectorId');
            $.ajax({
                type: 'get',
                url: '/itso/technicalServiceResults/getEquipmentsList'
            }).done(function (response) {
                let equipments = response.records;
                let selected = document.getElementById('equipmentSelectorId');
                for (let i = 0; i < equipments.length; i++) {
                    let option = document.createElement('option');

                    let name = equipments[i].name;
                    let position = equipments[i].position.join(', ');
                    let factoryNumber = equipments[i].factoryNumber;
                    let year = equipments[i].year;
                    let info = [factoryNumber, year].filter(function (v) {
                        return v;
                    }).join(', ');
                    let equipment = name + ', ' + position + ((info != null && info.length > 0) ? ' (' + info + ')' : '');

                    option.value = equipments[i].uuid;
                    option.text = equipment;
                    $(option).attr('name', name);
                    $(option).attr('position', position);
                    $(option).attr('factoryNumber', factoryNumber);
                    $(option).attr('year', year);

                    selected.add(option);
                }
                s.select2({
                    placeholder: 'Выберите оборудование',
                    multiple: true,
                    allowClear: true
                });

                s.on('select2:select', function (e) {
                    let selected = s.val();
                    if (selected[0] == '') {
                        selected.shift();
                    }
                    s.val(selected).trigger('change');
                });
            }).fail(function () {
                alert('ALARM_Equip');
            });
        },
        confirm: function (params) {
            let buttonHTML = '';
            $.each(params.buttons, function (name, obj) {
                buttonHTML += '<button class="button' + obj.class + '">' + name + '</button>';
            });

            $('.warning-title').text(params.title);
            $('.warning-message').text(params.message);
            $('#confirmButtons').append(buttonHTML);

            let buttons = $('#confirmButtons .button_blue'),
                i = 0;

            $.each(params.buttons, function (name, obj) {
                buttons.eq(i++).click(function () {
                    obj.action();
                    $('.warning-title').text('');
                    $('.warning-message').text('');
                    $('#confirmButtons').empty();
                    return false;
                });
            });

            $('#confirmOverlay').modal('show');
        },
        clearData: function () {
            $('#idRecord').val('');
            $('#jobDescriptionId').val('');

            $('#entryTimeId').val('');
            $('#exitTimeId').val('');

            $('#workerSelectorId').val(null).trigger('change');
            $('#equipmentSelectorId').val(null).trigger('change');
        },
        removeEntryDaterangePicker: function () {
            if ($('#entryTimeId').data('daterangepicker') != undefined) {
                $('#entryTimeId').data('daterangepicker').remove();
            }
        },
        createEntryDaterangePicker: function () {
            $.dateRangePickerSettings.startDate = new Date();
            $('#entryTimeId').daterangepicker($.dateRangePickerSettings).on('apply.daterangepicker', function (ev, picker) {
                $(this).val(picker.startDate.format($.dateRangePickerSettings.locale.format));

                if (!$.compareDates($('#entryTimeId').val(), $('#exitTimeId').val())) {
                    $('#exitTimeId').val('');
                }
                $.removeExitDaterangePicker();
                $.createExitDaterangePicker($('#entryTimeId').val());

                document.getElementById('entryTimeId').style.borderColor = '#DCDCDC';
            });

        },
        removeExitDaterangePicker: function () {
            if ($('#exitTimeId').data('daterangepicker') != undefined) {
                $('#exitTimeId').data('daterangepicker').remove();
            }
        },
        createExitDaterangePicker: function (start) {
            let newDateRangePickerSettings = {};
            for (let key in $.dateRangePickerSettings) {
                newDateRangePickerSettings[key] = $.dateRangePickerSettings[key];
            }
            newDateRangePickerSettings.minDate = start;
            newDateRangePickerSettings.startDate = new Date();
            $('#exitTimeId').daterangepicker(newDateRangePickerSettings).on('apply.daterangepicker', function (ev, picker) {
                $(this).val(picker.startDate.format($.dateRangePickerSettings.locale.format));
            });
        }
    });
})(jQuery);
