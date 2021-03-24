var currentPageUrl = 'http://'+ location.hostname;
var pageExt = "";
var serverUrl = currentPageUrl + ":40";
var CANVAS_INSTALL_URL = "fairmontschools.beta.instructure.com";

//global vairable definition
var editor;
if(window.socketId){
}else{
    window.socketId = getCookie("socketId");
}
var accountId = getCookie("accountId");
var urlParams = new URLSearchParams(window.location.search);
if(urlParams.has("id")){
    var canvasId = urlParams.get("id");
}else{
    window.location.href = currentPageUrl+pageExt+"/choose.html";
}
var userId;
var ifCanvas;

//init
window.onload = function() {
    if(canvasId.indexOf("v") > 0) ifCanvas = true;
    else ifCanvas = false;

    fetch(serverUrl+"/initDiscussion?socketId="+window.socketId+"&id="+canvasId).then(responseText => responseText.json()).then(response => {
        if("success" in response){
            userId = response.userId;
                        
            if(ifCanvas || response.hasParticipant){
                fetch(serverUrl+"/view?socketId="+window.socketId+"&id="+canvasId).then(response => response.json()).then((json) =>{
                    this.discussionJson = json;
                    this.initApp();
                })
            }else{
                $('#setNameModal').modal({backdrop: "static"})
                $('#setNameModal').modal("show")
            }
        }else{
            if(ifCanvas) window.location.href = currentPageUrl+pageExt;
            else window.location.href = currentPageUrl+pageExt+"choose.html";
        }
    })
}

/**
 * Requires global variable: discussionJson
 */
function initApp(){
    editor = document.getElementById("textBox");
    document.execCommand('defaultParagraphSeparator', false, 'p'); 
    
    window.textBox = new TextBox("textBox", "messageToolbar", {
        "debug": true, 
        //"onChange": onChange,
        "onInput": onChange,
        onNewWord: textBox_onNewWord,
        onCtrlEnter: textBox_onSend
    });

    window.replyingTo = null;

    let ids = canvasId.split("v")
    let courseId = ids[0]
    let canvasDiscussionId = ids[1] 
    var canvasUrlLoc = CANVAS_INSTALL_URL+"/courses/"+courseId+"/discussion_topics/"+canvasDiscussionId;

    if(!ifCanvas) document.getElementById("shareUrl").value = currentPageUrl+pageExt+"/join.html?id="+canvasId;

    document.getElementById("discussionTopic").innerHTML = "<input type='text' id='topicInput' class='hiddenInput' onfocusout='changeTitle();' oninput='this.size = this.value.length - 2;' size='"+(discussionJson['title'].length-2)+"' value='"+discussionJson['title']+"'>&nbsp;&nbsp;&nbsp;&nbsp;"
                                                                + (ifCanvas ? "<button onclick='window.open(\""+canvasUrlLoc+"\");' class='btn btn-outline-secondary btn-sm'>In Canvas</button>" : "<button type='button' class='btn btn-outline-secondary btn-sm' data-toggle='modal' data-target='#inviteModal'>Invite</button>");
    document.getElementById("initialPromptParagraph").innerHTML = discussionJson['topic']

    window.messageFeed = new MessageFeed("messageFeed", {
        //options
    })

    var toBottomButton_wrap = document.getElementById("toBottomBtn-Wrap");
    //onscroll
    window.messageFeed.feedObject.onscroll = function(){

        if(window.messageFeed.feedObject.scrollTop < window.messageFeed.feedObject.scrollHeight - (window.messageFeed.feedObject.clientHeight * 2)){
            toBottomButton_wrap.style.right = "18px"
        }else{
            toBottomButton_wrap.style.right = "-100px"
        }
    }

    let firstParticipant = true
    Object.values(discussionJson.participants).map(user => {
        if(user.id == userId) user.display_name = user.display_name+"&nbsp; <span style='color: grey'>(You)</span>";
        addParticipant(user.id, user.display_name, user.avatar_image_url);
        if(firstParticipant){
            participantItem_onclick(user.id)
            firstParticipant = false;
        }
    })
    
    Object.values(discussionJson.online).map(online_id => {
        setParticipantOnline(online_id);
    })
    
    Object.values(discussionJson.view).map(entry => {
        var entryUserId = entry.user_id;
        if(entryUserId){
            var orderValue = getOrderValueUTC(entry.updated_at);
            
            var isSelf = entryUserId+"" == userId
            
            var userName = isSelf ? "You" : discussionJson.participants[entryUserId].display_name;
            var align = isSelf ? "right" : "left";
            window.messageFeed.pushMessage({
                id: entry.id,
                senderImg: discussionJson.participants[entryUserId].avatar_image_url,
                senderName: userName,
                senderId: entryUserId,
                align: align,
                message: entry.message,
                order: orderValue
            });
            
            document.getElementById("messageFeed-"+entry.id+"-replyButton").addEventListener("click", (event) => {
                setReply(entry.id);
            })

            //add replies
            if(entry.replies){
                entry.replies.map(replyEntry => {
                    let replyUserId = replyEntry.user_id+"";
                    replyIsSelf = replyUserId == userId;
                    
                    var replyUserName = replyIsSelf ? "You" : discussionJson.participants[replyUserId].display_name;
                    
                    var replyOrderValue = getOrderValueUTC(replyEntry.updated_at);
                    
                    var replyObject = {
                        replyToId: replyEntry.parent_id,
                        id: replyEntry.id,
                        senderName: replyUserName,
                        senderId: replyUserId,
                        message: replyEntry.message,
                        updateParentOrder: replyOrderValue
                    }
                    
                    if(replyIsSelf) replyObject['background'] = "#b7dfb7"
                    
                    window.messageFeed.pushReply(replyObject);
                })
            }
        }
    })
    
    window.messageFeed.reorderMessage();
    try{
        document.getElementById("messageFeed").scrollTo(0, document.getElementById("messageFeed").scrollHeight);
    }catch(err){
        let messageFeedElem = document.getElementById("messageFeed")
        messageFeedElem.scrollTop = messageFeedElem.scrollHeight;
    }
    
    setTimeout(() => {
        hideInitialPrompt();
    }, 2000);
    
    window.s_websocket = new S_WebSocket('ws://'+location.host+':40/socket?socketId='+window.socketId, {
        keepAlive: true,
        onMessage: socket_onMessage,
    });
    
    var applyDrag = document.getElementsByClassName("draggable");
    for(var i = 0; i < applyDrag.length; i++){
        dragElement(applyDrag[i]);
    }

    setParticipantOnline(userId);
}

function setName(){
    var userName = document.getElementById("userName").value;
    if(userName == "") userName = "Guest";

    fetch(serverUrl+"/join?socketId="+window.socketId+"&userName="+userName).then(response => response.json()).then((responseJson) => {
        console.log(responseJson);
        if("error" in responseJson){
            return;
        }
        fetch(serverUrl+"/view?socketId="+window.socketId+"&id="+canvasId).then(response => response.json()).then((discussJson) =>{
            this.discussionJson = discussJson;
            this.initApp();
            $("#setNameModal").modal('hide');
        })
    })
}

function onGoogleSignIn(googleUser){
    let url = googleUser.getBasicProfile().getImageUrl();
    fetch(serverUrl+"/updateImage?socketId="+window.socketId+"&imageUrl="+url);
    
    var auth2 = gapi.auth2.getAuthInstance();
    auth2.signOut().then(function () {
      console.log('User signed out.');
    });
}

const textBox_onSend = () => {
    if(window.replyingTo){
        fetch(serverUrl+"/reply?socketId="+window.socketId+"&discussionId="+canvasId, {
            method: "POST",
            body: JSON.stringify({
                replyTo: window.replyingTo,
                message: window.textBox.getHtml()
            })
        }).then(response => response.json()).then(responseJson => {
            let senderId = responseJson.user_id;
            var replyObject = {
                replyToId: responseJson.parent_id,
                id: responseJson.id,
                senderName: "You",
                senderId: senderId,
                message: responseJson.message,
                updateParentOrder: getOrderValueUTC(responseJson.updated_at),
                background: "#b7dfb7"
            }
            window.messageFeed.replaceReply(responseJson.parent_id, "r"+senderId, replyObject);
            
            if(!("replies" in discussionJson.view[responseJson.parent_id])) discussionJson.view[responseJson.parent_id].replies = [];
            discussionJson.view[responseJson.parent_id].replies.push(responseJson)

            window.s_websocket.send("resetsync ");
        })
        
        window.textBox.editor.style.width = "calc(100% - 190px)";
        window.replyIndicator.parentElement.removeChild(window.replyIndicator);
        window.replyIndicator = null;
        window.messageFeed.reorderMessage();
        window.replyingTo = null;
    }else{
        fetch(serverUrl+"/send?socketId="+window.socketId+"&discussionId="+canvasId, {
            method: "POST",
            body: JSON.stringify({
                message: window.textBox.getHtml()
            })
        }).then(response => response.json()).then(responseJson => {
            let senderId = responseJson.user_id;
            window.messageFeed.replaceMessage("0"+senderId, {
                id: responseJson.id,
                senderImg: discussionJson.participants[senderId].avatar_image_url,
                senderName: "You",
                senderId: senderId,
                align: "right",
                message: responseJson.message,
                order: getOrderValueUTC(responseJson.updated_at),
                callback: () => {
                    console.log("ahhhh")
                    document.getElementById("messageFeed-"+responseJson.id+"-replyButton").addEventListener("click", (event) => {
                        setReply(responseJson.id);
                    })
                }
            });

            discussionJson.view[responseJson.id] = responseJson;

            window.s_websocket.send("resetsync ");
        })
    }
    window.textBox.emptyTextbox();
    window.textBox.focus();
    //onChange();
}

//TODO:!!!!!!!!!!!!!!
function changeTopic(){
    var newDiscussionTopic = document.getElementById("initialPromptParagraph").innerHTML;
    fetch(serverUrl+"/changeDiscussionTopic?discussionId="+canvasId+"&accountId="+accountId+"&newDiscussionTopic="+newDiscussionTopic).then(response => response.json()).then(responseJson => {
        console.log(responseJson);
    })
}

function changeTitle(){
    var newDiscussionTitle = document.getElementById("topicInput").value;
    fetch(serverUrl+"/changeDiscussionTitle?discussionId="+canvasId+"&accountId="+accountId+"&newDiscussionTitle="+newDiscussionTitle).then(response => response.json()).then(responseJson => {
        console.log(responseJson);
    })
}

var blockUsers = [];

const socket_onMessage = (jsonObject) => {
    if("sync" in jsonObject){
        let syncObject = jsonObject.sync;
        let replyTo = syncObject.replyTo;
        let senderId = syncObject.senderId;
        let message = syncObject.content;

        if(!blockUsers.includes(senderId)){
            let stickBottom = false;
            if(window.messageFeed.feedObject.scrollTop + window.messageFeed.feedObject.clientHeight > window.messageFeed.feedObject.scrollHeight - 200){
                stickBottom = true;
            }
    
            //if(senderId in participants) return;
    
            let isYourself = senderId == userId
            if(replyTo == "n"){
                //new message (typing)
                let align = isYourself ? "right" : "left";
                let name = isYourself ? "You" : discussionJson.participants[senderId].display_name;
                window.messageFeed.pushMessage({
                    id: "0"+senderId,
                    senderImg: discussionJson.participants[senderId].avatar_image_url,
                    senderName: name+" - typing...",
                    senderId: senderId,
                    align: align,
                    message: message,
                    disableReply: true,
                    typing: true
                })
                
                window.messageFeed.setBottom("0"+senderId);
            }else{
                //live reply to a message
                let name = isYourself ? "You" : discussionJson.participants[senderId].display_name;
                window.messageFeed.setBottom(replyTo);
                let replyObject = {
                    id: "r"+senderId,
                    replyToId: replyTo,
                    message: message,
                    senderName: name+" - typing...",
                    senderId: senderId,
                    typing: true
                }
                if(isYourself) replyObject["background"] = "#b7dfb7"
                window.messageFeed.pushReply(replyObject);
                window.messageFeed.setBottom(replyTo);
            }
    
            if(stickBottom) {
                window.messageFeed.feedObject.scrollTop = window.messageFeed.feedObject.scrollHeight
            }
    
            //TODO: make a looped timeout function that check if the message still exists, and if it exists, delete after a certain time frame
            //maybe that is not avery good idea
        }
    }

    if("endsync" in jsonObject){
        let endSyncObject = jsonObject.endsync;
        let replyTo = endSyncObject.replyTo;
        let senderId = endSyncObject.senderId;

        if(!blockUsers.includes(senderId)){
            if(replyTo == "n"){
                window.messageFeed.deleteMessage("0"+senderId);
            }else{
                window.messageFeed.deleteReply(replyTo, "r"+senderId);
                window.messageFeed.releaseBottom(replyTo);
                window.messageFeed.reorderMessage();
            }
        }
    }

    if("online" in jsonObject){
        setParticipantOnline(jsonObject.online.user_id)
    }

    if("offline" in jsonObject){
        setParticipantOffline(jsonObject.offline.user_id)
    }

    if("post" in jsonObject){
        let stickBottom = false;
        if(window.messageFeed.feedObject.scrollTop + window.messageFeed.feedObject.clientHeight > window.messageFeed.feedObject.scrollHeight - 200){
            stickBottom = true;
        }
        let topicJson = jsonObject.post;
        let senderId = topicJson.user_id;
        let isSelf = senderId == userId;

        if(!blockUsers.includes(senderId)) blockUsers.push(senderId);

        window.messageFeed.replaceMessage("0"+senderId, {
            id: topicJson.id,
            senderImg: discussionJson.participants[senderId].avatar_image_url,
            senderName: isSelf ? "You" : discussionJson.participants[senderId].display_name,
            senderId: senderId,
            align: isSelf ? "right" : "left",
            message: topicJson.message,
            order: getOrderValueUTC(topicJson.updated_at),
            callback: () => {
                document.getElementById("messageFeed-"+topicJson.id+"-replyButton").addEventListener("click", (event) => {
                    setReply(topicJson.id);
                })
            }
        });

        discussionJson.view[topicJson.id] = topicJson;

        window.messageFeed.reorderMessage();

        if(stickBottom) {
            window.messageFeed.feedObject.scrollTop = window.messageFeed.feedObject.scrollHeight
        }
    }

    if("reply" in jsonObject){
        let stickBottom = false;
        if(window.messageFeed.feedObject.scrollTop + window.messageFeed.feedObject.clientHeight > window.messageFeed.feedObject.scrollHeight - 200){
            stickBottom = true;
        }
        let replyJson = jsonObject.reply;
        let senderId = replyJson.user_id;
        let isSelf = senderId == userId;

        if(!blockUsers.includes(senderId)) blockUsers.push(senderId);

        var replyObject = {
            replyToId: replyJson.parent_id,
            id: replyJson.id,
            senderName: isSelf ? "You" : discussionJson.participants[senderId].display_name,
            senderId: senderId,
            message: replyJson.message,
            updateParentOrder: getOrderValueUTC(replyJson.updated_at),
        }
        if(isSelf) replyObject["background"] = "#b7dfb7"
        window.messageFeed.replaceReply(replyJson.parent_id, "r"+senderId, replyObject);

        if(!("replies" in discussionJson.view[replyJson.parent_id])) discussionJson.view[replyJson.parent_id].replies = []; 
        discussionJson.view[replyJson.parent_id].replies.add(replyJson);

        window.messageFeed.releaseBottom(window.replyingTo);
        
        setTimeout(() => {
            window.messageFeed.reorderMessage();
        }, 500);

        if(stickBottom) {
            window.messageFeed.feedObject.scrollTop = window.messageFeed.feedObject.scrollHeight
        }
    }

    if("newParticipant" in jsonObject){
        discussionJson.participants[jsonObject.newParticipant.id] = jsonObject.newParticipant;
        var user = jsonObject.newParticipant;
        addParticipant(user.id, user.display_name, user.avatar_image_url);
        setParticipantOnline(user.id);
    }

    if("resetsync" in jsonObject){
        let senderId = jsonObject.resetsync.senderId;
        let indexOf = blockUsers.indexOf(senderId);
        if(indexOf > -1){
            blockUsers.splice(indexOf, 1);
        }
    }

    if("profileImage" in jsonObject){
        let imageObject = jsonObject.profileImage;
        let user_id = imageObject.user_id;
        let url = imageObject.url;

        replaceParticipantImage(user_id, url);
    }

    
    if("newTitle" in jsonObject){
        document.getElementById("topicInput").value = jsonObject.newTitle;
        document.getElementById("topicInput").onfocusout();
    }

    if("newTopic" in jsonObject){
        document.getElementById("initialPromptParagraph").innerHTML = jsonObject.newTopic;
    }
}

function replaceParticipantImage(id, url){
    let imgs = document.getElementsByClassName("participantImage-"+id);
    for(var i = 0; i < imgs.length; i++){
        imgs[i].src = url;
    }
}

function messageFeedScrollBottom(){
    window.messageFeed.feedObject.scrollTop = window.messageFeed.feedObject.scrollHeight
}

function setReply(id, recursive){
    if(id == null){
        if(window.replyingTo){
            window.textBox.editor.style.width = "calc(100% - 190px)";
            window.s_websocket.send("endsync "+window.replyingTo);
            window.replyIndicator.parentElement.removeChild(window.replyIndicator);
            window.replyIndicator = null;
            window.messageFeed.deleteReply(window.replyingTo, "r"+userId);
            window.messageFeed.releaseBottom(window.replyingTo);
            window.messageFeed.reorderMessage();
            window.replyingTo = null;
            onChange();
        }
    }else {
        if(id+"" != window.replyingTo+""){
            if(window.replyingTo) setReply(null, true);
            //set message to bottom
            var ifMessageExist = window.messageFeed.setBottom(id);
            if(ifMessageExist == false){
                return false
            }
            let senderId = discussionJson.view[id+""].user_id+"";
            let senderName = discussionJson.participants[senderId].display_name.substring(0, 4);
            
            //add reply indicator
            var replyIndicator = document.createElement("BUTTON");
            replyIndicator.style.width = "126px";
            replyIndicator.type = "button"
            replyIndicator.classList.add("btn", "btn-info")
            replyIndicator.style.marginRight = "10px";
            replyIndicator.style.transition = "all 100ms ease-in";
            replyIndicator.id = senderName
            replyIndicator.innerHTML = "reply:: "+senderName+" <span class='badge badge-light' style=''>x</span>" + 
                "<span class='sr-only'>close</span>"
    
            window.replyIndicator = replyIndicator;
            window.replyingTo = id;
            
            replyIndicator.onmouseover = function(){
                this.innerHTML = "cancel <span class='badge badge-light' style=''>x</span>" + 
                "<span class='sr-only'>close</span>"
            }
    
            replyIndicator.onmouseout = function() {
                this.innerHTML = "reply:: "+this.id+" <span class='badge badge-light' style=''>x</span>" + 
                "<span class='sr-only'>close</span>"
            }
    
            replyIndicator.onclick = function(){
                setReply(null);
            }
    
            //set width
            window.textBox.editor.style.width = "calc(100% - 336px)";
    
            //add live bubble to messageFeed
            let hadMessage = window.messageFeed.deleteMessage("0"+userId)
            if(hadMessage && document.getElementById("liveCheckbox").checked) window.s_websocket.send("endsync n")
            window.messageFeed.pushReply({
                id: "r"+userId,
                replyToId: id,
                message: document.getElementById("liveCheckbox").checked ? "<p>"+window.textBox.getText().replace(/\n\n/g, "<br/>") + "</p>" : "<p></p>",
                senderName: "You - typing...",
                senderId: userId,
                typing: true,
                background: "#b7dfb7"
            })
    
            document.getElementById("textBox-prepend").appendChild(replyIndicator);

            
            window.s_websocket.send("sync "+id+" "+window.textBox.getHtml());
        }

    }
    
    try{
        document.getElementById("messageFeed").scrollTo(0, document.getElementById("messageFeed").scrollHeight);
    }catch(err){
        let messageFeedElem = document.getElementById("messageFeed")
        messageFeedElem.scrollTop = messageFeedElem.scrollHeight;
    }
}

/**
 * get a number value of order for flex box from date
 * 
 * @param {string} utcString 
 */
function getOrderValueUTC(utcString){
    var orderDate = new Date(utcString);
    var dd = orderDate.getDate();
    var mm = orderDate.getMonth()+1; 
    var yyyy = orderDate.getFullYear();
    var hh = orderDate.getHours();
    var mmi = orderDate.getMinutes();
    var ss = orderDate.getSeconds();
    if(dd<10)  dd='0'+dd;
    if(mm<10)  mm="0"+mm;
    if(hh<10)  hh="0"+hh;
    if(mmi<10)  mmi="0"+mmi;
    if(ss<10)  ss="0"+ss;
    return ""+yyyy + mm + dd + hh + mmi + ss;
}


const fetchJson = async(url) => {
    return fetch(url).then(response => {
        return response.json()
    })
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

var textBox_characterChangeCount = 0;
var onChange = (type, pIndex, index, content) => {
    editor.style.height = "100%";
    if(editor.clientHeight != editor.scrollHeight){
        editor.style.height = "unset";
        editor.style.maxHeight = "300px";
        document.getElementById("messageBar").style.height = "unset";
    }

    var topValue = (-editor.clientHeight-40) > -75 ? "-75px" : (-editor.clientHeight-40)+"px"
    document.getElementById("messageToolbar").style.top = topValue;
    document.getElementById("messageFeed").style.height = "calc(100% - ("+(editor.clientHeight-80)+"px))";

    let messageFeedElem = document.getElementById("messageFeed")
    if(window.replyingTo){
        window.messageFeed.pushReply({
            id: "r"+userId,
            replyToId: window.replyingTo,
            message: document.getElementById("liveCheckbox").checked ? window.textBox.getHtml() : "<p></p>",
            senderName: "You - typing...",
            senderId: userId,
            typing: true
        })
        messageFeedElem.scrollTop = messageFeedElem.scrollHeight;
    }else{
        if(document.getElementById("liveCheckbox").checked){
            if(window.textBox.getText() == "\n" || window.textBox.getText() == ""){
                window.messageFeed.deleteMessage("0"+userId)
                window.s_websocket.send("endsync n")
            }else{
                window.messageFeed.pushMessage({
                    id: "0"+userId,
                    senderImg: discussionJson.participants[userId].avatar_image_url,
                    senderName: "You - typing...",
                    senderId: userId,
                    align: "right",
                    message: window.textBox.getHtml(),
                    disableReply: true,
                    typing: true
                })
                window.messageFeed.setBottom("0"+userId);
                try{
                    document.getElementById("messageFeed").scrollTo(0, document.getElementById("messageFeed").scrollHeight-100);
                }catch(err){
                }
            }
        }
        messageFeedElem.scrollTop = messageFeedElem.scrollHeight - 100;
    }

    if(document.getElementById("liveCheckbox").checked){
        textBox_characterChangeCount++;
        if(textBox_characterChangeCount > 10){
            sendTextBoxSync();
            textBox_characterChangeCount = 0;
        }else if(!newWord && window.textBox.getText() != "\n"){
            let textLength = window.textBox.getText().replace(/\n\n/g, "<br/>").length;
            setTimeout(() => {
                if(textLength == window.textBox.getText().replace(/\n\n/g, "<br/>").length){
                    sendTextBoxSync();

                    //timeout for live message
                    setTimeout(() => {
                        if(textLength == window.textBox.getText().replace(/\n\n/g, "<br/>").length){
                            if(window.replyingTo){
                                window.messageFeed.deleteReply(window.replyingTo, "r"+userId);
                                window.s_websocket.send("endsync "+window.replyingTo);
                            }else{
                                window.messageFeed.deleteMessage("0"+ userId);
                                window.s_websocket.send("endsync n");
                            }
                        }
                    }, 19000);
                }
            }, 1000);
        }
    }
    newWord = false;
}

var newWord = false;
//space and enter
var textBox_onNewWord = () => {
    if(document.getElementById("liveCheckbox").checked){
        sendTextBoxSync();
        textBox_characterChangeCount = 0;
        newWord = true;
    }
}

function sendTextBoxSync() {
    let messageId = window.replyingTo ? window.replyingTo : "n";
    window.s_websocket.send("sync "+messageId+" "+window.textBox.getHtml())

}

function toggleLiveMessage(state){
    if(state == null || state == undefined){
        if(document.getElementById("liveCheckbox").checked){
            toggleLiveMessage(true);
        }else{
            toggleLiveMessage(false);
        }
    }else if(state == false){
        if(window.replyingTo){
            //still keeps the reply bubble to indicate which message you are replying to
            window.messageFeed.pushReply({
                id: "r"+userId,
                replyToId: window.replyingTo,
                message: "<p></p>",
                senderName: "You - typing...",
                senderId: userId,
                typing: true
            })
            
            window.s_websocket.send("endsync "+window.replyingTo)   
        }else{
            window.messageFeed.deleteMessage("0"+userId)

            window.s_websocket.send("endsync n")   
        }
    }else{
        if(window.replyingTo){
            window.messageFeed.pushReply({
                id: "r"+userId,
                replyToId: window.replyingTo,
                message: "<p>"+window.textBox.getText().replace(/\n\n/g, "<br/>") + "</p>",
                senderName: "You - typing...",
                senderId: userId,
                typing: true
            })
            window.s_websocket.send("sync "+window.replyingTo+" "+window.textBox.getHtml());
        }else{
            window.messageFeed.pushMessage({
                id: "0"+userId,
                senderImg: discussionJson.participants[userId].avatar_image_url,
                senderName: "You - typing...",
                senderId: userId,
                align: "right",
                message: "<p>"+window.textBox.getText().replace(/\n\n/g, "<br/>") + "</p>",
                disableReply: true,
                typing: true
            })
            window.s_websocket.send("sync n "+window.textBox.getHtml());
        }
        try{
            document.getElementById("messageFeed").scrollTo(0, document.getElementById("messageFeed").scrollHeight-100);
        }catch(err){
            let messageFeedElem = document.getElementById("messageFeed")
            messageFeedElem.scrollTop = messageFeedElem.scrollHeight-100;
        }
    }
}

var getJSON = function(url, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'json';
    xhr.onload = function() {
      var status = xhr.status;
      if (status === 200) {
        callback(null, xhr.response);
      } else {
        callback(status, xhr.response);
      }
    };
    xhr.send();
};

function setParticipantOnline(id){
    document.getElementById("participant-"+id+"-indicator").style.color = "rgb(138, 216, 138)";
}
function setParticipantOffline(id){
    document.getElementById("participant-"+id+"-indicator").style.color = "#c68b8b";
}

var participantList;
function addParticipant(id, name, imageUrl){
    if(!participantList) participantList = document.getElementById("participantList");

    var item = document.createElement("div");
    item.id = "participant-"+id;
    item.classList.add("participantItem");
    item.onclick = function() {
        participantItem_onclick(id)
    };
    item.tabIndex = "0"; 
    item.innerHTML = "<div class='card-body' style='height: 100%; padding: 7px 16px;'>"+
    "    <img class='participantImage-"+id+"' src='"+imageUrl+"' style='height: 100%; display: inline-block; border-radius: 5px; border: 1.5px solid rgb(167, 167, 167)'/>"+
    "    <p class='align-content-md-end' style=\"max-width: 167px; height: 24px; text-overflow: ellipsis; overflow: hidden; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; display: inline-flex; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; color: rgb(53, 53, 53); padding-left: 19px;\">"+name+"</p>"+
    "    <i id='participant-"+id+"-indicator' class='fas fa-circle align-middle' style='margin-top: 12px; color:#c68b8b; font-size: 9px; float: right;'></i>"+
    "</div>";
    //#759075

    var itemCollapsible = document.createElement("DIV");
    
    itemCollapsible.classList.add("itemCollapsible");
    var skipOption = document.createElement("A");
    skipOption.classList.add("participantOption");
    skipOption.href = "#";
    skipOption.innerHTML = "<i class='fas fa-greater-than'></i> &nbsp;&nbsp;Skip to message<span id='participantCollapse-"+id+"-findIndex' style='float:right;'></span>";
    skipOption.onclick = function(){
        let findIndexSpan = document.getElementById("participantCollapse-"+id+"-findIndex")
        let spanText = window.messageFeed.jumpToSenderNext(id)
        if(spanText == false) spanText = "0/0";

        console.log(spanText);
        findIndexSpan.innerHTML = spanText;
        setTimeout(() => {
            if(findIndexSpan.innerHTML == spanText){
                findIndexSpan.innerHTML = "";
            }
        }, 1600)
    }

    var mentionOption = document.createElement("A");
    mentionOption.classList.add("participantOption");
    mentionOption.href = "#";
    mentionOption.innerHTML = "<i class='fas fa-at'></i> &nbsp;&nbsp;Mention";
    mentionOption.onclick = function(){
        window.textBox.pushHTML("<a href='#' class='mentionBlock mentionUser-"+id+"' id='textMention-userId-"+id+"'>@"+name+" </a>")
    }

    var idTag = document.createElement("SMALL");
    idTag.style.cssText = 'width: 100%; text-align: right; padding: 2px 14px 5px 10px; margin-top: -6px; color: rgb(104, 104, 104); display: inline-block; vertical-align: top;';
    idTag.innerHTML = "Id:&nbsp; "+id;

    itemCollapsible.appendChild(skipOption);
    itemCollapsible.appendChild(mentionOption)
    itemCollapsible.appendChild(idTag);

    participantList.appendChild(item);
    participantList.appendChild(itemCollapsible);
}

//#region button interactives
/**
 * expand participant Items
 * 
 * @param {string} id 
 */
function participantItem_onclick(id){
    let itemId = "participant-"+id;
    if(itemId == window.focusedParticipantItemId){
        document.getElementById(window.focusedParticipantItemId).classList.remove("active");
        window.focusedParticipantItemId = null;
        return true;
    }
    let item = document.getElementById(itemId);
    if(item){
        if(window.focusedParticipantItemId){
            document.getElementById(window.focusedParticipantItemId).classList.remove("active");
        }
        item.classList.add("active");
        window.focusedParticipantItemId = itemId;
        return true;
    }else{
        return false;
    }
}
//#endregion

function createCssClass(styleName, style){
    var styleElement = document.createElement('style');
    styleElement.type = 'text/css';
    styleElement.innerHTML = styleName+' { '+style+' }';
    document.getElementsByTagName('head')[0].appendChild(styleElement);
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
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/';
  }
 


/**
 * Static Straightforward Functions
 */

function hideInitialPrompt(){
    if(document.getElementById("initialPromptText").classList.contains("fa-chevron-up")){
        document.getElementById("initialPromptCard").style.height = "0px";
        document.getElementById("initialPromptText").classList.replace("fa-chevron-up", "fa-chevron-down");
    }else{
        document.getElementById("initialPromptCard").style.height = "fit-content";
        document.getElementById("initialPromptText").classList.replace("fa-chevron-down", "fa-chevron-up");
    }
}

function toolbarToggle(checkbox){
    if(checkbox.checked){
        document.getElementById("toolBarArrow").classList.add("fa-caret-square-down");
        document.getElementById("toolBarArrow").classList.remove("fa-caret-square-up");
        document.getElementById("messageToolbar").style.top = (-editor.clientHeight-40)+"px";
    }else{
        document.getElementById("toolBarArrow").classList.add("fa-caret-square-up");
        document.getElementById("toolBarArrow").classList.remove("fa-caret-square-down");
        document.getElementById("messageToolbar").style.top = "-29px";
    }
}

//https://www.w3schools.com/howto/howto_js_draggable.asp

function dragElement(elmnt) {
  var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
    // otherwise, move the DIV from anywhere inside the DIV:
    console.log(elmnt);
    elmnt.onmousedown = dragMouseDown;

  function dragMouseDown(e) {
    e = e || window.event;
    e.preventDefault();
    // get the mouse cursor position at startup:
    pos3 = e.clientX;
    pos4 = e.clientY;
    document.onmouseup = closeDragElement;
    // call a function whenever the cursor moves:
    document.onmousemove = elementDrag;
  }

  function elementDrag(e) {
    e = e || window.event;
    e.preventDefault();
    // calculate the new cursor position:
    pos1 = pos3 - e.clientX;
    pos2 = pos4 - e.clientY;
    pos3 = e.clientX;
    pos4 = e.clientY;
    // set the element's new position:
    elmnt.style.top = (elmnt.offsetTop - pos2) + "px";
    elmnt.style.left = (elmnt.offsetLeft - pos1) + "px";
  }

  function closeDragElement() {
    // stop moving when mouse button is released:
    document.onmouseup = null;
    document.onmousemove = null;
  }
}