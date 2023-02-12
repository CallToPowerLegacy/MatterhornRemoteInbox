/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
var carouselSpeed = 8000;
var scrollTopAnimationTime = 100;
var app = null;
var language = "en";
var currPage = "#information";
var translations = new Array();

function translate(str, strIfNotFound) {
    return (translations[str] != undefined) ? translations[str] : strIfNotFound;
}

function checkLanguage() {
    if (window.location.search) {
        if (window.location.search == "?lang=german") {
            language = "ger";
            $("#lang_en").removeClass("language-active");
            $("#lang_ger").addClass("language-active");
        } else {
            language = "en";
            $("#lang_ger").removeClass("language-active");
            $("#lang_en").addClass("language-active");
        }
    } else {
        language = "en";
        $("#lang_ger").removeClass("language-active");
        $("#lang_en").addClass("language-active");
    }
}

function checkHash() {
    var hash = null;
    if (window.location.hash) {
        if (window.location.hash == "#information") {
            displayInformation();
            hash = "#information";
        } else if (window.location.hash == "#news") {
            displayNews();
            hash = "#news";
        } else if (window.location.hash == "#features") {
            displayFeatures();
            hash = "#features";
        } else if (window.location.hash == "#download") {
            displayDownload();
            hash = "#download";
        } else if (window.location.hash == "#faq") {
            displayFAQ();
            hash = "#faq";
        } else if (window.location.hash == "#register") {
            displayRegister();
            hash = "#register";
        } else if (window.location.hash == "#contact") {
            displayContact();
            hash = "#contact";
        }
    } else {
        displayInformation();
        hash = "#information";
    }
    if (hash != null) {
        $(".lang_link").each(function() {
            this.hash = hash;
        });
    }
    displayChanged();
}

function parseNewsAlert() {
    if (app && app.newsalert && (app.newsalert.trim().length > 0) && (app.newsalert.trim().indexOf("<!--") == -1)) {
        $("#newsAlert_content").html(app.newsalert);
        $(".newsAlert").show();
    } else {
        $(".newsAlert").hide();
    }
}

function parseInformation() {
    if (app) {
        if(app.information1) {
            $("#information1_content").html(
                app.information1
            );
        } else {
            $("#information1_content").hide();
        }
        if(app.information2) {
            $("#information2_content").html(
                app.information2
            );
        } else {
            $("#information2_content").hide();
        }
    }
}

function parseScreenshots() {
    var available = app && app.screenshots && (app.screenshots.length > 0);
    $("#screenshots_content").html("");
    if (available) {
        $("#screenshots_content").append('<div id="screenshots_carousel" class="carousel slide" data-ride="carousel">');
        $("#screenshots_carousel").append('<ol id="carousel_indicators" class="carousel-indicators">');
        var inserted = 0;
        for (var i = 0; i < app.screenshots.length; ++i) {
            if (app.screenshots[i].description && app.screenshots[i].url) {
                $("#carousel_indicators").append(
                    '<li data-target="#screenshots_carousel" data-slide-to="' + i + '" class="' + ((inserted == 0) ? 'active' : '') + '"></li>');
                ++inserted;
            }
        }
        $("#screenshots_carousel").append("</ol>");
        $("#screenshots_carousel").append('<div id="carousel_inner" class="carousel-inner">');
        for (var i = 0; i < app.screenshots.length; ++i) {
            if (app.screenshots[i].description && app.screenshots[i].url && app.screenshots[i].width) {
                $("#carousel_inner").append(
                    '<div class="item ' + ((i == 0) ? 'active' : '') + '">' +
                    '<img src="' + app.screenshots[i].url + '" alt="' + app.screenshots[i].description + '" width="' + app.screenshots[i].width + '%">' +
                    '<div class="carousel-caption">' +
                    app.screenshots[i].description +
                    '</div>' +
                    '</div>'
                );
            }
        }
        $("#screenshots_carousel").append(
            '</div>' +
            '<a class="left carousel-control" href="#screenshots_carousel" role="button" data-slide="prev">' +
            '<span class="glyphicon glyphicon-chevron-left"></span>' +
            '</a>' +
            '<a class="right carousel-control" href="#screenshots_carousel" role="button" data-slide="next">' +
            '<span class="glyphicon glyphicon-chevron-right"></span>' +
            '</a>');
        $("#screenshots_carousel").append("</div>");
        $("#screenshots_carousel").carousel({
            interval: carouselSpeed
        });
    } else {
        $("#screenshots_content").hide();
    }
}

function parseNews() {
    if (app && app.news) {
        for (var i = 0; i < app.news.length; ++i) {
            if (app.news[i] && app.news[i].name &&
                ((app.news[i].list_bugfixes && (app.news[i].list_bugfixes.length > 0)) || (app.news[i].list_features && (app.news[i].list_features.length > 0)))) {
                var bugfixes = "<ul>";
                for (var j = 0; j < app.news[i].list_bugfixes.length; ++j) {
                    bugfixes += "<li>" + app.news[i].list_bugfixes[j] + "</li>";
                }
                bugfixes += "</ul>";
                var features = "<ul>";
                for (var j = 0; j < app.news[i].list_features.length; ++j) {
                    features += "<li>" + app.news[i].list_features[j] + "</li>";
                }
                features += "</ul>";
                var inner = "";
                if (app.news[i].list_bugfixes.length > 0) {
                    inner += "<b>Bugfixes:</b>" + bugfixes;
                }
                if (app.news[i].list_features.length > 0) {
                    inner += "<b>Features:</b>" + features;
                }
                $("#news_content").append(
                    '<div class="panel panel-default">' +
                    '<div class="panel-heading">' +
                    '<h3 class="panel-title">' +
                    app.news[i].name +
                    '</h3>' +
                    '</div>' +
                    '<div class="panel-body">' +
                    inner +
                    '</div>' +
                    '</div>'
                );
                /*
                $("#news_content").append(
                    "<h3>" + app.news[i].name + "</h3>" +
                    "<div>" +
                    inner +
                    "</div>"
                );
                */
            }
        }
    }
}

function parseFeatures() {
    if (app && app.features && (app.features.length > 0)) {
        for (var i = 0; i < app.features.length; ++i) {
            if (app.features[i] && app.features[i].name && app.features[i].list && (app.features[i].list.length > 0)) {
                var features = "<div><p><ul class=\"newIn\">";
                for (var j = 0; j < app.features[i].list.length; ++j) {
                    features += "<li>" + app.features[i].list[j] + "</li>";
                }
                features += "</ul></p></div>";
                $("#features_content").append(
                    "<h3>" + app.features[i].name + "</h3>" + features
                );
            }
        }
    }
}

function parseDownload() {
    if (app && app.downloadMetadata) {
        if (app.downloadMetadata.systemRequirements && app.downloadMetadata.systemRequirements.length > 0) {
            var sr = "<ul>";
            for (var i = 0; i < app.downloadMetadata.systemRequirements.length; ++i) {
                sr += "<li>" + app.downloadMetadata.systemRequirements[i] + "</li>";
            }
            sr += "</ul>";
            $("#systemRequirements_content").html(sr);
        } else {
            $("#systemRequirements").hide();
        }
        if (app.downloadMetadata.serverRequirements && app.downloadMetadata.serverRequirements.length > 0) {
            var sr = "<ul>";
            for (var i = 0; i < app.downloadMetadata.serverRequirements.length; ++i) {
                sr += "<li>" + app.downloadMetadata.serverRequirements[i] + "</li>";
            }
            sr += "</ul>";
            $("#serverRequirements_content").html(sr);
        } else {
            $("#serverRequirements").hide();
        }
        if (app.downloadMetadata.changelog) {
            $("#changelog_content").html('<a href="' + app.downloadMetadata.changelog + '" target="_blank">' + translate("changelog", "Changelog") + '</a>');
        } else {
            $("#changelog").hide();
        }
        if (app.downloadMetadata.eula) {
            $("#eula_content").html(translate("str_eula", 'With downloading one of the versions of the software you accept the EULA') + ': ' + '<a href="' + app.downloadMetadata.eula + '" target="_blank">' + translate("eula", "EULA") + '</a>.');
        } else {
            $("#eula").hide();
        }
    }
    if (app && app.downloads && (app.downloads.length > 0)) {
        var foundCurrentWindows = false;
        var foundCurrentCross = false;
        var foundBeta = false;
        var foundOld = false;
        for (var i = 0; i < app.downloads.length; ++i) {
            if (app.downloads[i] && app.downloads[i].type) {
                if (app.downloads[i].type == "currentWindows") {
                    if (app.downloads[i].name && app.downloads[i].url) {
                        foundCurrentWindows = true;
                        $("#downloads_currentWindowsData1").html("<a href=\"" + app.downloads[i].url + "\" class=\"button-download download-class\">Download</a>");
                        $("#downloads_currentWindowsData2").html(app.downloads[i].name);
                    }
                } else if (app.downloads[i].type == "currentCross") {
                    if (app.downloads[i].name && app.downloads[i].url) {
                        foundCurrentCross = true;
                        $("#downloads_currentCrossData1").html("<a href=\"" + app.downloads[i].url + "\" class=\"button-download-beta download-class\">Download</a>");
                        $("#downloads_currentCrossData2").html(app.downloads[i].name);
                    }
                } else if (app.downloads[i].type == "beta") {
                    if (app.downloads[i].name && app.downloads[i].url) {
                        foundBeta = true;
                        $("#downloads_betaData1").html("<a href=\"" + app.downloads[i].url + "\" class=\"button-download-beta download-class\">Download</a>");
                        $("#downloads_betaData2").html(app.downloads[i].name);
                    }
                } else if (app.downloads[i].type == "old" && app.downloads[i].list && (app.downloads[i].list.length > 0)) {
                    var oldDownloads = "";
                    for (var j = 0; j < app.downloads[i].list.length; ++j) {
                        if (app.downloads[i].list[j].name && app.downloads[i].list[j].url) {
                            foundOld = true;
                            $("#downloads_oldData").append(
                                "<li><a href=\"" + app.downloads[i].list[j].url + "\">" + app.downloads[i].list[j].name + "</a></li>"
                            );
                        }
                    }
                }
            }
        }
        if (!foundCurrentWindows) {
            $("#downloads_currentWindowsData1").hide();
            $("#downloads_currentWindowsData2").hide();
        } else {
            $("#downloads_currentWindowsData1").show();
            $("#downloads_currentWindowsData2").show();
        }
        if (!foundCurrentCross) {
            $("#downloads_currentCrossData1").hide();
            $("#downloads_currentCrossData2").hide();
        } else {
            $("#downloads_currentCrossData1").show();
            $("#downloads_currentCrossData2").show();
        }
        if (!foundBeta) {
            $("#downloads_betaData1").hide();
            $("#downloads_betaData2").hide();
        } else {
            $("#downloads_betaData1").show();
            $("#downloads_betaData2").show();
        }
        if (!foundOld) {
            $("#oldVersionDownloads, #downloads_oldData").hide();
        } else {
            $("#oldVersionDownloads, #downloads_oldData").show();
        }
    }
}

function parseFAQ() {
    if (app && app.faq) {
        for (var i in app.faq) {
            $("#faq_content").append(
                '<div class="panel panel-default">' +
                '<div class="panel-heading">' +
                '<h3 class="panel-title">' +
                app.faq[i].question +
                '</h3>' +
                '</div>' +
                '<div class="panel-body">' +
                app.faq[i].answer +
                '</div>' +
                '</div>'
            );
        }
    }
}

function displayChanged() {
    $("html, body").animate({
        scrollTop: 0
    }, scrollTopAnimationTime);
    if (currPage != null) {
        $(".lang_link").each(function() {
            this.hash = currPage;
        });
    }
}

function displayInformation() {
    $("#news, #features, #download, #faq, #register, #contact").hide();
    $("#btn_news, #btn_features, #btn_download, #btn_faq, #btn_register, #btn_contact").removeClass("active");
    $("#btn_information").addClass("active");
    $("#information").show();
    currPage = "#information";
    displayChanged();
}

function displayNews() {
    $("#information, #features, #download, #faq, #register, #contact").hide();
    $("#btn_information, #btn_features, #btn_download, #btn_faq, #btn_register, #btn_contact").removeClass("active");
    $("#btn_news").addClass("active");
    $("#news").show();
    currPage = "#news";
    displayChanged();
}

function displayFeatures() {
    $("#information, #news, #download, #faq, #register, #contact").hide();
    $("#btn_information, #btn_news, #btn_download, #btn_faq, #btn_register, #btn_contact").removeClass("active");
    $("#btn_features").addClass("active");
    $("#features").show();
    currPage = "#features";
    displayChanged();
}

function displayDownload() {
    $("#information, #news, #features, #faq, #register, #contact").hide();
    $("#btn_information, #btn_news, #btn_features, #btn_faq, #btn_register, #btn_contact").removeClass("active");
    $("#btn_download").addClass("active");
    $("#download").show();
    currPage = "#download";
    displayChanged();
}

function displayFAQ() {
    $("#information, #news, #features, #download, #register, #contact").hide();
    $("#btn_information, #btn_news, #btn_features, #btn_download, #btn_register, #btn_contact").removeClass("active");
    $("#btn_faq").addClass("active");
    $("#faq").show();
    currPage = "#faq";
    displayChanged();
}

function displayRegister() {
    $("#information, #news, #features, #download, #faq, #contact").hide();
    $("#btn_information, #btn_news, #btn_features, #btn_download, #btn_faq, #btn_contact").removeClass("active");
    $("#btn_register").addClass("active");
    $("#register").show();
    currPage = "#register";
    displayChanged();
}

function displayContact() {
    $("#information, #news, #features, #download, #faq, #register").hide();
    $("#btn_information, #btn_news, #btn_features, #btn_download, #btn_faq, #btn_register").removeClass("active");
    $("#btn_contact").addClass("active");
    $("#contact").show();
    currPage = "#contact";
    displayChanged();
}

function initTranslation() {
    for (k in translations) {
        $(".lang_" + k).html(translations[k]);
    }
}

function loadData(func) {
    checkLanguage();
    $.ajax({
        url: "data/data_" + language + ".json",
        dataType: "json"
    }).done(function(data) {
        app = data;
        if (app && app.translations) {
            var key = Object.keys(app.translations);
            for (var i = 0; i < key.length; i++) {
                var lang_value = key[i];
                translations[lang_value] = app.translations[lang_value];
            }
            initTranslation();
        }
        if (func) {
            func();
        }
        $("#initial_spinner").hide();
    }).fail(function(jqXHR, textStatus, errorThrown) {
        $("#newsAlert_content").html("Could not load data.");
        $(".newsAlert").show();
        $("#initial_spinner").hide();
    });
}

function clearAllSections() {
    $("#information_content, #screenshot_content, #news_content, #features_content, #faq_content").html("");
}

function parseData() {
    clearAllSections();
    parseNewsAlert();
    parseInformation();
    parseScreenshots();
    parseNews();
    parseFeatures();
    parseDownload();
    parseFAQ();
}

function collapseNavbar() {
    if ($(".navbar-toggle").css("display") == "block" && !$(this).siblings().length) {
        $(".navbar-collapse").collapse("hide");
    }
}

function registerClickHandlers() {
    $("#btn_information").click(displayInformation);
    $("#btn_news").click(displayNews);
    $("#btn_features").click(displayFeatures);
    $("#btn_download").click(displayDownload);
    $("#btn_faq").click(displayFAQ);
    $("#btn_register").click(displayRegister);
    $("#btn_contact").click(displayContact);

    $("body").click(function(e) {
        collapseNavbar();
    });
    $("#icon").click(function(e) {
        collapseNavbar();
    });
    $(".navbar-collapse a").click(function(e) {
        collapseNavbar();
    });
}

$(document).ready(function() {
    $("#initial_spinner").show();

    if ("onhashchange" in window) {
        $(window).bind("hashchange", function(e) {
            checkHash();
        });
    }

    checkHash();
    registerClickHandlers();
    loadData(parseData);
});

var _gaq = _gaq || [];
_gaq.push(["_setAccount", "UA-34094094-1"]);
_gaq.push(["_trackPageview"]);

(function() {
    var ga = document.createElement("script");
    ga.type = "text/javascript";
    ga.async = true;
    ga.src = ("https:" == document.location.protocol ? "https://ssl" : "http://www") + ".google-analytics.com/ga.js";
    var s = document.getElementsByTagName("script")[0];
    s.parentNode.insertBefore(ga, s);
})();
