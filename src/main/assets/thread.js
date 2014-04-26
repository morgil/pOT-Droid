$(document).ready(function() {

    // bender loading
    if(api.isBenderEnabled() && api.downloadBenders()) {
        $(".bender:not([style])").each(function() {
            var bender = $(this).parents("section").first();
            var user_id = bender.attr("data-user-id");
            var user_avatar_id = bender.attr("data-user-avatar-id");
            var user_avatar_file = bender.attr("data-user-avatar");
            api.displayBender(parseInt(user_id,10), user_avatar_file, parseInt(user_avatar_id,10));
        });
    }

    // automatic image loader
    // Only images within or after the currently visible post are loaded, so
    // the visible position is not changed
    // also, we don't really need to see the pictures above.
    if(api.isLoadImages()) {
        loadAllImages();
    }

    if(api.isLoadVideos()) {
        loadAllVideos();
    }

    // manual image loader
    $("div.img i").click(function() {
        replaceImage($(this), "");
    });

    // manual image loader
    $("div.video i.vid").click(function() {
        replaceVideo($(this));
    });

    // manual video loader
    $("div.video i.link").click(function() {
        api.openUrl($(this).parent().attr('data-src'));
    });

    // manual image with link loader
    $("div.img-link i.link").click(function() {
        api.openUrl($(this).parent().attr('data-href'));
    });

    // manual image with link loader
    $("div.img-link i.img").click(function() {
        replaceImage($(this), $(this).parent().attr('data-href'));
    });

    $("div.spoiler i").click(function() {
        $(this).hide().parent().find('div').show();
    });

    $('i.menu-icon').click(function(e) {
        var post_id = parseInt($(this).closest('section').attr('data-id'));
        api.openTopicMenu(post_id);
    });

    $('i.menu-edit').click(function(e) {
        var post_id = parseInt($(this).closest('section').attr('data-id'));
        api.editPost(post_id);
    });

    $('i.menu-quote').click(function(e) {
        var post_id = parseInt($(this).closest('section').attr('data-id'));
        api.quotePost(post_id);
    });

    $('i.menu-bookmark').click(function(e) {
        var post_id = parseInt($(this).closest('section').attr('data-id'));
        api.bookmarkPost(post_id);
    });

    $('i.menu-link').click(function(e) {
        var post_id = parseInt($(this).closest('section').attr('data-id'));
        api.linkPost(post_id);
    });

    $('i.menu-pm').click(function(e) {
        var post_id = parseInt($(this).closest('section').attr('data-id'));
        api.pmAuthor(post_id);
    });

    $('a.author').click(function(e) {
        if($("a[name=" + getURLParameter(this, 'PID') + "]").length > 0) {
            e.preventDefault();
            $("*").css({ opacity: 1.0 });
            location.hash = "#";
            location.hash = "#" + getURLParameter(this, 'PID');
        }
    });

    // scroll to the last post, when there was one
    // to ensure correct scrolling, this should be the last JS call.
    setTimeout(function() {

        setupStyle();
        $(window).resize(function() {
            setupStyle();
        });

        if(api.getScroll() > 0) {
            document.location.href = "#" + api.getScroll();
            if(api.isDarkenOldPosts()) {
                $('a[name="' + api.getScroll() + '"]').parent().prevAll().addClass("oldpost");
            }
            if(api.isMarkNewPosts()) {
                $('a[name="' + api.getScroll() + '"]').parent().nextAll().addClass("newpost");
            }
        } else {
            window.scrollTo(0,0);
        }

        // register waypoints while scrolling over them
        // should be the last thing executed!
        $("header").waypoint(function() {
            api.registerScroll(parseInt($(this).parent().attr("data-id"),10));
        },{
            continuous: false,
        });


    }, 100);

});

function replaceImage(icon, link_target) {
    icon.attr('class', "fa fa-spinner spin");
    var el = icon.parent();
    var src = el.attr('data-src');
    var img = $('<img/>').attr('src',src).attr('alt',src);
    img.load(function() {
        if(link_target === "") {
            el.replaceWith(img);
        } else {
            var a = $("<a/>").attr("href", link_target).append(img);
            el.replaceWith(a);
        }

    });
}

function youtube_parser(url){
    var regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
    var match = url.match(regExp);
    if (match&&match[2].length==11){
        return match[2];
    } else {
        // error
    }
}

function replaceVideo(icon) {
    var el = icon.parent();
    var src = el.attr('data-src');

    if(el.hasClass("yt")) {
        var id = youtube_parser(src);
        var wrapper = $("<div class=\"videoWrapper\"></div>");
        var iframe = $("<iframe type=\"text/html\" frameborder=\"0\" allowfullscreen></iframe>")
            .attr("src", "http://www.youtube.com/embed/" + id);
        wrapper.append(iframe);
        el.replaceWith(wrapper);
    } else {
        var wrapper = $('<video controls width="100%"></video>').attr('src', src);
        var link = $("<a target='_blank'></a>").attr('href', src).text(src);
        wrapper.append(link);
        el.replaceWith(wrapper);
    }

}

// load the bender of user_id
function loadBender(user_id, path) {
    var el = $("section[data-user-id='"+user_id+"']");
    el.find("div.bender").css("background-image","url("+path+")");
}

// unveil dimmed posts
function unveil() {
    $("*").css({ opacity: 1.0 });
    window.scrollTo(0,0);
}

// scroll to the last post of UID uid
function scrollToLastPostByUID(uid) {
    var href = $("section[data-user-id="+uid+"]").last().find("a").first().attr("name");
    if(typeof href === "undefined")
        api.error("Kein Post auf dieser Seite");
    else
        document.location.href = "#" + href;
}

// load all images
function loadAllImages() {
    var all = [];
    if(api.getScroll() > 0) {
        var self = $('a[name="' + api.getScroll() + '"]').parent().find('div.img i, div.img-link i.img');
        var after = $('a[name="' + api.getScroll() + '"]').parent().nextAll().find('div.img i, div.img-link i.img');
        all = $.merge(self, after);
    } else {
        all = $('div.img i, div.img-link i.img');
    }
    all.each(function() {
        var href = $(this).parent().attr("data-href");
        if(typeof href === "undefined") {
            replaceImage($(this), "");
        } else {
            replaceImage($(this), href);
        }
    });
}

// load all videos
function loadAllVideos() {
    var all = [];
    if(api.getScroll() > 0) {
        var self = $('a[name="' + api.getScroll() + '"]').parent().find('div.video i.vid');
        var after = $('a[name="' + api.getScroll() + '"]').parent().nextAll().find('div.video i.vid');
        all = $.merge(self, after);
    } else {
        all = $('div.video i.vid');
    }
    all.each(function() {
        replaceVideo($(this), $(this).parent().attr("data-src"));
    });
}


function setupStyle() {

    if(api.isBenderEnabled()) {
        if(api.getBenderPosition() == 1) {
            $("header .bender").show();
        } else if(api.getBenderPosition() == 2) {
            $("article .bender").show();
        } else if($(window).width() > 400) {
            $("header .bender").hide();
            $("article .bender").show();
        } else {
            $("header .bender").show();
            $("article .bender").hide();
        }
    }

    if(api.getShowMenu() == 1) {
        $(".menu").show();
        $(".menu-icon").hide();
    } else if(api.getShowMenu() == 2) {
        $(".menu-icon").show();
        $(".menu").hide();
    } else if($(window).width() > 400) {
        $(".menu").show();
        $(".menu-icon").hide();
    } else {
        $(".menu-icon").show();
        $(".menu").hide();
    }

    // login shit
    if(!api.isLoggedIn()) {
        $(".login").hide();
    }
}

function getURLParameter(a, name) {
    return decodeURI(
        (RegExp(name + '=' + '(.+?)(&|$)').exec(a.search)||[,null])[1]
    );
}

