var currentPageUrl = 'http://'+ location.hostname;
var pageExt = "";
var serverUrl = currentPageUrl + ":40";
var CANVAS_INSTALL_URL = "fairmontschools.beta.instructure.com";

window.onload = function(){
    var discussionCookie = getCookie("discussionUrl");
    if(discussionCookie != null && discussionCookie != "null"){
        setCookie("discussionUrl", "null", 0);
        setTimeout(function(){
            linkTo(discussionCookie);
        }, 300);
    }else{
        //if no cookies to forward
        var discussionListDiv = document.getElementById("discussionList");
        var response = fetch(serverUrl+"/listDiscussions?socketId="+window.socketId).then(response => response.json()).then((discussionList) => {
            console.log("testing");
            Object.entries(discussionList).forEach(([key, courseItem]) => {
                var courseList = document.createElement("DIV");
                courseList.classList.add("courseList", "item");
    
                var titleLi = document.createElement("li");
                titleLi.classList.add("list-group-item", "list-group-item-info");
                titleLi.innerHTML = courseItem.name;
                courseList.appendChild(titleLi);
    
                Object.entries(courseItem.discussions).forEach(([key, discussionItem]) => {
                    var discussionButton = document.createElement("BUTTON");
                    discussionButton.classList.add("list-group-item", "list-group-item-action", "liveDiscussionOption");
                    discussionButton.innerHTML = discussionItem.title+" <span>choose</span>"; 
                    discussionButton.onclick = function(){
                        linkTo(courseItem.id+"v"+discussionItem.id);
                    }
                    courseList.appendChild(discussionButton);
                }) 
    
                discussionListDiv.appendChild(courseList);
            });
        document.getElementById("loader").style.display = "none";
        })
    }
}

function directGo(){
    var courseId = document.getElementById("courseId").value;
    var discussionId = document.getElementById("discussionId").value;

    linkTo(courseId+"v"+discussionId);
}

function linkTo(urlPath){
    window.location.href = currentPageUrl+pageExt+"/discussion.html?id="+urlPath;
}


function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}


function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
  }
  function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
  }
  function eraseCookie(name) {   
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
  }

  function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
  }
 

function sleep(milliseconds) {
    const date = Date.now();
    let currentDate = null;
    do {
      currentDate = Date.now();
    } while (currentDate - date < milliseconds);
  }
