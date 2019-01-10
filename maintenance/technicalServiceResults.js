;(function ($, window, document, undefined) {
    "use strict";
    let pluginName = 'technicalServiceResults';
    let element = null;
    let defaults = {};

    let methodsPrivate = {
        element: null,
        _isShow: false,
        _show: function () {
            moment.locale('ru');
            methodsPrivate._isShow = true;
        }

    };
    let methodsPublic = {
        init: function (el) {
            methodsPrivate.element = el;
            $(methodsPrivate.element).load('../../sec/maintenance/technicalServiceResult', function () {
                methodsPrivate._show();
            });
        },
        show: function () {
            methodsPrivate._show();
        },
        refresh: function () {
            //methodsPrivate._show();
        },
        resize: function () {
        },
        destroy: function () {
        }
    };

    function TechnicalService(element, options) {
        this.element = element;
        this.options = $.extend({}, defaults, options);
        this._defaults = defaults;
        this._name = pluginName;
        this.init(element);
    }

    TechnicalService.prototype.init = function (el) {
        element = $(this.element);
        methodsPrivate.element = el;
        $(this.element).load('../../sec/maintenance/technicalServiceResult', function () {
            methodsPrivate._show();
        });
    };

    $.fn[pluginName] = function (options) {
        //this.append = methods._append();
        $(this)[0].itso = $.extend({}, methodsPublic);
        this.methods = methodsPublic;
        return $.data(this, 'plugin_' + pluginName, new TechnicalService(this, options));
    };

})(jQuery, window, document);
