
const currentPageUrl = 'http://'+ location.hostname;
const serverUrl = "http://speedstor.net:40";
const CANVAS_INSTALL_URL = "fairmontschools.beta.instructure.com";

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
        insertAlert(document.getElementById("discussionLogin"), 'This mode is not implemented yet', "login-alert");
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


function insertAlert(parentElement, stringContent, id){
    
    var alert = document.createElement("DIV");
        
    alert.className = "alert alert-warning alert-dismissible fade show";
    alert.style = "font-size: 13px; padding-top: 8px; padding-bottom: 8px;";
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
 