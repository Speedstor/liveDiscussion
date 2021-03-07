var currentPageUrl = 'http://'+ location.hostname;
var pageExt = "";
var serverUrl = currentPageUrl + ":40";
var CANVAS_INSTALL_URL = "fairmontschools.beta.instructure.com";

//check cookies exists, to foward to login or discussion
console.log("check login");
window.socketId = getCookie("socketId");
var accountId = getCookie("accountId");
if(window.socketId == null){
    if(accountId == null){
        window.location.href =  currentPageUrl+pageExt;
    }else{
        var response = fetch(serverUrl+"/checkLogin?socketId=update&accountId="+accountId).then(responseText => responseText.text()).then((response) => {
            if(response == "false"){
                window.location.href = currentPageUrl+pageExt;
            }else{
                window.socketId = response.substring(15, response.length);
                setCookie("socketId", window.socketId, 0);
            }    
        })
    }
}else{
    if(accountId == null){
        window.location.href =  currentPageUrl+pageExt;
    }else{
        var response = fetch(serverUrl+"/checkLogin?socketId="+window.socketId+"&accountId="+accountId).then(responseText => responseText.text()).then((response) => {
            console.log(response)
            if(response == "false"){
                window.location.href =  currentPageUrl+pageExt;
            }else if(response == "true"){

            }else if(response.substring(0, 14) == "outdatedSocket"){
                window.socketId = response.substring(15, response.length);
                setCookie("socketId", window.socketId, 0);
            }else{
                window.location.href =  currentPageUrl+pageExt;
            }
        })
    }
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
