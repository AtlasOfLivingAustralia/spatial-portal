var ALASpatial = {
    jQuery: $,

    init: function() {
        this.setupNamesSearch();
        this.setupProcessFormWizard(); 
    },

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

        //$('#splist').flexbox('/BioMaps2/GetNames?st=tsj&t=t', flexboxOptions);
        $('#splist').flexbox('species/names', flexboxOptions);


        $('#getrecs').click(function () {
            /*
            $.getJSON("species/records", {
                query: $('#spdata').val()
            }, function(records) {
                console.log(records);
            });
            */
            var theQuery = $('#spdata').val();
            //console.log("sending: " + theQuery);

            var theQueryList = theQuery.split("#");
            var theName = theQueryList[0];
            var theLevel = theQueryList[1];

            //console.log(theName + " - " + theLevel);

            //$("#records").load("species/records?a=test&query=" + theQuery + "&b=test");
            //$("#records").load("species/records?a=test&name=" + theName + "&level=" + theLevel + "&b=test");
            $.get("species/records", {
                query: $('#spdata').val()
            }, function(records) {
                //console.log(records);
                $('#records').html(records);
            });
        });
    },

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
                //alert("data successfully added");
                //console.log(data);
                var meResult = "Successfully generate Maxent results.";
                meResult += "<a href='output/maxent/species.html'>View output</a>";
                meResult += "<br />";
                meResult += "<a href='output/maxent/species.asc'>Download generated grid</a>";
                $('#resultUrl').html(meResult);
                //alert(meResult);
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

$(document).ready(function() {
    ALASpatial.init();
});
