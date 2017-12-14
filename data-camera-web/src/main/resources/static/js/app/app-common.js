/**
 *  Belongs to data-camera-web
 *  Author: liye on 2017/12/13
 *  Description:
 */

function inAppPage(){
    var $loader = $("#app-loading");
    var $app_main_tab = $('#app-main-tab');
    $loader.fakeLoader({
        timeToHide: 1000,
        spinner:"spinner3",
        bgColor:"rgba(154, 154, 154, 1)"
    });
    var tab = getQueryString("tab");
    if (tab == null){
        initResourceOfExperimentPage();
    } else if (tab != null && tab == 2){
        $app_main_tab.find('li:eq(1) a').tab('show');
        initResourceOfAnalysisPage();
    }

    // -- tab change
    $app_main_tab.find('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        var page = e.target.getAttribute('href');
        if (page == "#app-experiment"){
            initResourceOfExperimentPage();
        } else if (page == "#app-analysis") {
            initResourceOfAnalysisPage();
        }
    });

    $('#app-analysis-tab').find('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        var page = e.target.getAttribute('href');
        var exp_id = page.split('-')[2];
        initExpContentChart(exp_id);
    });
}