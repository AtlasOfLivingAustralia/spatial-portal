/**
 *
 * ALASpatial to do various front-end bits
 * This might eventually be replaced by the zk code
 *
 */
var ALASpatial = {
    jQuery: $,

    /**
     * init method 
     */
    init: function() {
        this.setupNamesSearch();
        this.setupProcessFormWizard(); 
    },

    /**
     * setupNamesSearch method to setup the AutoComplete
     */
    setupNamesSearch: function() {
        var flexboxOptions = {
            paging: {
                pageSize: 10,
                showSummary: true
            },
            minChars: 3,
            width: 350,
            displayValue: 'tname',
            hiddenValue: 'tlevel',
            resultTemplate: '<div class="col1" style="float: right">{tlevel}</div><div class="col2">{tname}</div>',
            resultsProperty: 'taxanames',
            totalProperty: 'totalCount',
            showArrow: false,
            onSelect: function() {
                $('input.ac_sp_data').val(this.value + "#" + this.getAttribute('hiddenValue'));
            }
        };

        $('#splist').flexbox('species/names', flexboxOptions);


        $('#getrecs').click(function () {
            var theQuery = $('#spdata').val();

            var theQueryList = theQuery.split("#");
            var theName = theQueryList[0];
            var theLevel = theQueryList[1];

            $.get("species/records", {
                query: $('#spdata').val()
            }, function(records) {
                $('#records').html(records);
            });
        });
    },

    /**
     * setupProcessFormWizard method to setup the wizard 
     */
    setupProcessFormWizard : function () {
        $("#theProcessForm").formwizard({
            //form wizard settings
            historyEnabled : false,
            formPluginEnabled: true,
            validationEnabled : true
        },
        {
        //validation settings
        },
        {
            // form plugin settings
            success: function (data) {
                var meResult = "Successfully generate Maxent results.";
                meResult += "<br />";
                meResult += "<a target='_blank' href='" + data.map + "'>View map</a>";
                meResult += "<br />";
                meResult += "<a target='_blank' href='" + data.info + "'>View generated Maxent information</a>";
                meResult += "<br />";
                meResult += "<a target='_blank' href='" + data.file + "'>Download generated grid</a>";
                $('#resultUrl').html(meResult);
            },
            beforeSubmit: function(data){
                //alert("about to send the following data: \n\n" + $.param(data));
                $('#resultUrl').html("<img src='images/loader.gif' /> Analysing data, please wait...");
            },
            dataType: 'json',
            resetForm: true
        }
        );


    }

};

/**
 * Start it all off on the page load 
 */
$(document).ready(function() {
    ALASpatial.init();
});
