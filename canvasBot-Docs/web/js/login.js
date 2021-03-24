var currentPageUrl = 'http://'+ location.hostname;
var serverUrl = currentPageUrl + ":40";
var pageExt = "";
var CANVAS_INSTALL_URL = "fairmontschools.beta.instructure.com";

var socket;
var serverToken;
var socketId;
var accountId; //same with serverId on the server side
var discussionUrl;
var discussionJson;

function displayLogin(type){
    var canvasLogin = document.getElementById("canvasLogin");
    var discussionLogin = document.getElementById("discussionLogin");
    var testLogin = document.getElementById("testLogin");
    if(type == "canvas"){
        canvasLogin.style.display = 'flex';
        discussionLogin.style.display = 'none';
        testLogin.style.display = 'none';
    }else if(type == "discussion"){
        canvasLogin.style.display = 'none';
        discussionLogin.style.display = 'block';
        testLogin.style.display = 'none';
    }else if(type == "test"){
        canvasLogin.style.display = 'none';
        discussionLogin.style.display = 'none';
        testLogin.style.display = 'block';
    }
}

function loginOnClick(){
    if(document.getElementById("canvasOption").checked){
        //TODO: could give key to encode the url
        let fetchLoginUrl = fetch(serverUrl+"/getLoginUrl?").then(response => response.json()).then((responseJson) => {
            let urlParams = new URLSearchParams(window.location.search);
            if(urlParams.has("canvas")){
                setCookie("discussionUrl", urlParams.get("canvas"), 0);
            }
            
            setCookie("socketId", responseJson.socketId, 0);
            setCookie("accountId", responseJson.accountId, 9999999);
            window.location.href = responseJson.url;
        })
    }else if(document.getElementById("discussionOption").checked){
        let deleteAlert = document.getElementById("login-alert");
        if(deleteAlert) deleteAlert.parentElement.removeChild(deleteAlert);

        var idNumbers = document.getElementsByClassName("inputDiscussionId");
        
        var inputDiscussionId = "";
        for(var i = 0; i < idNumbers.length; i++){
            if(idNumbers[i].value.length < 1){
                insertAlert(document.getElementById("discussionLogin"), 'Please provide a 6 digit code', "login-alert");
                return;
            }
            inputDiscussionId += idNumbers[i].value;
        }
        console.log(inputDiscussionId);

        var urlParameter;
        var accountId = getCookie("accountId");
        if(accountId == null) urlParameter = "";
        else urlParameter = "&accountId="+accountId;

        fetch(serverUrl+"/newUser?discussionId="+inputDiscussionId+urlParameter).then(response => response.json()).then((responseJson) => {
            if("error" in responseJson){
                insertAlert(document.getElementById("discussionLogin"), 'The code is not valid', "login-alert");
                return;
            }
            setCookie("socketId", responseJson.socketId, 0);
            setCookie("accountId", responseJson.accountId, 9999999);
        })
        window.location.href = currentPageUrl+pageExt+"/discussion.html?id="+inputDiscussionId;
    }else if(document.getElementById("testOption").checked){
        var canvasToken = document.getElementById("canvasToken-direct").value;
        fetch(serverUrl+"/tokenLogin?canvasToken="+canvasToken).then(response => response.json()).then((responseJson) => {
            let urlParams = new URLSearchParams(window.location.search);
            if(urlParams.has("canvas")){
                setCookie("discussionUrl", urlParams.get("canvas"), 0);
            }

            setCookie("socketId", responseJson.socketId, 0);
            setCookie("accountId", responseJson.accountId, 9999999);
            window.location.href = responseJson.url;
        })
    }
}

function createDiscussionRoom(){
    fetch(serverUrl+"/createDiscussion").then(response1 => response1.text()).then((newDiscussionId) => {
        fetch(serverUrl+"/newUser?discussionId="+newDiscussionId).then(response => response.json()).then((responseJson) => {
            setCookie("socketId", responseJson.socketId, 0);
            setCookie("accountId", responseJson.accountId, 9999999);
        })
        window.location.href = currentPageUrl+pageExt+"/discussion.html?id="+newDiscussionId;
    })
}

function insertAlert(parentElement, stringContent, id){
    var alert = document.createElement("DIV");
        
    alert.className = "alert alert-warning alert-dismissible fade show";
    alert.style = "font-size: 13px; padding-top: 8px; padding-bottom: 8px; margin-bottom: 5px; color: black;";
    alert.role = "alert";
    alert.id = id;

    alert.innerHTML = stringContent+
    '<button type="button" class="close" style="padding-top: 6px; padding-bottom: 2px;" data-dismiss="alert" aria-label="Close">'+
    '    <span aria-hidden="true">&times;</span>'+
    '</button>';

    parentElement.prepend(alert);
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
    document.cookie = name+'=; Max-Age=-99999999;';  
  }
 

  
window.onload = function(){
    document.getElementById("discussionIdWrapper").addEventListener("keydown", function(e){
        e.preventDefault();
        if(e.key == "Backspace"){
            e.target.value = "";
            if(e.target.previousElementSibling != null) e.target.previousElementSibling.focus();
            return;
        }
        if(isNaN(e.key)) {
            return;
        }
        e.target.value = e.key;
        if(e.target.nextElementSibling != null) e.target.nextElementSibling.focus();
    })
    
}