'use strict';
;(function ($) {
    $('#buttonMaintenance').on('click', function () {
        $.editRow(null);
    });

    $('#cancelId').on('click', function () {
        $.cancel();
    });

    $('.close').on('click', function () {
        $.cancel();
    });

    $('#saveId').on('click', function () {
        $.saveForm();
    });

    $('#oKId').on('click', function () {
        $('.entry-div').empty();
        $('.exit-div').empty();
        $('.equip-div').empty();
        $('.jobDiscr-div').empty();
        $('.worker-div').empty();
        $('#maintenanceModalRO').modal('hide');
    });

    $('#maintenanceTableId').on('dbl-click-row.bs.table', function (e, row, $element) {
        // console.error(row);
        $.editRow(row);
    });

    $('#exitTimeId').on('click', function () {
       if ($('#entryTimeId').val() == '') {
           $.confirm({
               'title': 'Внимание!',
               'message': 'Сначала заполните время прибытия.',
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
    });

})(jQuery);