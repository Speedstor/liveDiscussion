class MessageFeed{
    constructor(id, option){
        this.feedObject = document.getElementById(id);

        this.feedObject.style.display = "block";
        this.feedObject.style.justifyContent = "flex-end";
        this.feedObject.style.flexDirection = "column";
        this.feedObject.style.overflow = "auto";
        this.feedObject.style.scrollBehavior = "smooth";

        this.feedObject.classList.add("scrollbar-grey")
        
        this.feedContainer = document.createElement("DIV");
        this.feedContainer.style.display = "flex";
        this.feedContainer.style.justifyContent = "flex-end";
        this.feedContainer.style.flexDirection = "column";
        this.feedContainer.style.paddingBottom = "61px";

        this.feedObject.appendChild(this.feedContainer);
        
        //Right now it stores elements, but it might be benificial to store id instead, because it will be copied a lot. But the consideration of location of obejcts instead, i think that is the case tho
        this.messageListObject = {};  //listed with order key

        this.defaultUserImg = option.hasOwnProperty("defaultUserImg") ? option.defaultUserImg : "./img/defaultUserImg.png";
        let orderValueDigits = option.hasOwnProperty("orderValueDigits") ? option.orderValueDigits : 18;
        this.maxOrderKey = "";
        for(var i = 0; i < orderValueDigits; i++) this.maxOrderKey += "9";

        this.trapBottom = [];

        console.log("MessageFeed initialized with element id: ("+id+")")
    }

    deleteMessage(id){
        let deleteElement = document.getElementById("messageFeed-"+id);
        if(deleteElement){
            let deleteArray = Object.keys(this.messageListObject).find(key => this.messageListObject[key] === deleteElement);
    
            for(var i = 0; i < deleteArray.length; i++){
                var key = deleteArray[i];   
                delete this.messageListObject[key]
            }
    
            deleteElement.parentNode.removeChild(deleteElement);
            return true;
        }else{
            return false;
        }
    }

    deleteReply(messageId, replyId){
        let deleteElement = document.getElementById("messageFeed-"+messageId+"-reply-"+replyId);
        if(deleteElement){
            deleteElement.parentNode.removeChild(deleteElement);
            return true;
        }else{
            return false;
        }
    }

    replaceMessage(originalId, messageObject){
        let originalObject = document.getElementById("messageFeed-"+originalId+"-inner")
        if(originalObject){
            let wrap = originalObject.parentElement.parentElement;
            let outer = wrap.parentElement;

            wrap.style.height = wrap.clientHeight+"px";
            wrap.style.width = wrap.clientWidth+"px";
            wrap.style.transition = "all 300ms ease-in";
            outer.style.height = outer.clientHeight+"px";

            setTimeout(() => {
                wrap.style.height = "0px";
                //wrap.style.width = "0px";
            },100)

            setTimeout(() => {
                this.deleteMessage(originalId);
                this.pushMessage(messageObject);
            }, 600)
        }else{
            this.pushMessage(messageObject)
        }
    }

    replaceReply(messageId, replyId, replyObject){
        let originalReplyObject = document.getElementById("messageFeed-"+messageId+"-reply-"+replyId+"-inner")
        if(originalReplyObject){
            originalReplyObject.parentElement.style.overflow = "hidden";
            let replyWrap = originalReplyObject.parentElement.parentElement;
            let replyOuter = replyWrap.parentElement;

            replyWrap.style.overflow = "hidden";
            replyWrap.style.height = replyWrap.clientHeight+"px";
            replyWrap.style.width = replyWrap.clientWidth+"px";
            replyWrap.style.transition = "all 200ms ease-in";
            replyOuter.style.height = replyOuter.clientHeight+"px";


            setTimeout(() => {
                replyWrap.style.height = "0px";
                //replyWrap.style.width = "0px";
            },200)

            setTimeout(() => {
                this.deleteReply(messageId, replyId);
                this.pushReply(replyObject);
            }, 800)
        }else{
            this.pushReply(replyObject);
        }
    }

    pushMessage(idOrMessageObject, senderImg, senderName, entryUserId, align, messageHtml, order, disableReply, typing, callback){
        let id;
        if(typeof idOrMessageObject === 'object'){
            let messageObject = idOrMessageObject
            if(!messageObject.hasOwnProperty("id")) return false;
            if(!messageObject.hasOwnProperty("message")) return false;
    
            senderImg = messageObject.hasOwnProperty("senderImg") ? messageObject.senderImg : this.defaultUserImg;
            senderName = messageObject.hasOwnProperty("senderName") ? messageObject.senderName : "";
            entryUserId = messageObject.hasOwnProperty("senderId") ? messageObject.senderId : -1;
            align = messageObject.hasOwnProperty("align") ? messageObject.align : "left";
            order = messageObject.hasOwnProperty("order") ? messageObject.order : this.maxOrderKey;
            disableReply = messageObject.hasOwnProperty("disableReply") ? messageObject.disableReply : false;
            typing = messageObject.hasOwnProperty("typing") ? messageObject.typing : false;
            callback = messageObject.hasOwnProperty("callback") ? messageObject.callback : null;
            messageHtml = messageObject.message;
            id = messageObject.id;
        }else{
            id = idOrMessageObject
        }

        var messageInner = document.getElementById("messageFeed-"+id+"-inner")
        if(messageInner){
            messageInner.innerHTML = messageHtml;
            return "messageFeed-"+id;
        }else{
            let right = true;
            let orderKey = this.getOrderKey(order, id);
            if(align == "left") right = false;

            align = right ? "right" : "left";

            var messageElement = document.createElement("DIV");
            messageElement.id = "messageFeed-"+id;
            messageElement.classList.add("messageFeedItem", right ? "messageFeedItem-right" : "messageFeedItem-left", "messageSender-"+entryUserId);
            messageElement.style.cssText = right ? "display: flex; margin-bottom: 35px; margin-top: 20px; justify-content: flex-end; flex-grow: 0; align-items: flex-end; padding-right: 30px; padding-left: 30px; flex-direction:row;" :
                                                        "display: flex; margin-bottom: 35px; margin-top: 20px; justify-content: flex-end; flex-grow: 0; align-items: flex-end; padding-right: 30px; padding-left: 30px; flex-direction:row-reverse;";
            messageElement.value = align+"-"+orderKey;
            messageElement.style.order = Object.keys(this.messageListObject).length + 1;

            var paddingLeft = right ? "31px" : "33px";
            var flexDirection = right ? 'row' : "row-reverse";
            messageElement.innerHTML = "<div id='messageFeed-"+id+"-container' value='"+align+"' style='width:100%; display: flex; flex-direction: column;'>"+
            "<div style='width:100%; display: flex; justify-content: flex-end; flex-grow: 0; align-items: flex-end; flex-direction: "+flexDirection+";'>"+
                "<div class='messageBubble-wrap' style='overflow:hidden; max-width: 61%; display: flex; flex-direction: column; align-items: flex-start;'><h6 style='font-weight: normal; margin: 0px; padding-left: "+paddingLeft +"; color: rgb(146, 146, 146); width: unset;'>"+senderName+"</h6>"+
                    "<div class='messageBubble messageBubble-"+align+" "+(typing ? "typing" : "")+"' ><div class='card-body' id='messageFeed-"+id+"-inner' style='padding: 12px 0px 4px 17px;'>"+ 
                    ""+messageHtml +
                    "</div>"+
                "</div>"+
                "</div>"+
                (disableReply ? "" : "<div class='mF-infoTag mF-infoTag-"+align+"' id='messageFeed-"+id+"-replyButton' style='order: -1;'>Click to reply</div>")+
            "<img class='participantImage-"+entryUserId+"' src='"+senderImg+"' style='width: 40px; order: 1; background: white; border-radius: 5px;'/></div></div>";

            this.feedContainer.appendChild(messageElement);
            
            if(callback) callback();
            
            this.messageListObject[orderKey] = messageElement;
            return messageElement.id;
        }
    }

    updateOrderKey(id, orderKey){
        let updateElement = document.getElementById("messageFeed-"+id);
        if(updateElement){
            let deleteArray = Object.keys(this.messageListObject).find(key => this.messageListObject[key] === updateElement);
    
            for(var i = 0; i < deleteArray.length; i++){
                var key = deleteArray[i];   
                delete this.messageListObject[key]
            }
    
            this.messageListObject[orderKey] = updateElement;
        }
    }

    setBottom(id){
        var focused = document.getElementById("messageFeed-"+id);
        if(focused){
            focused.style.order = Object.keys(this.messageListObject).length + 1;
            this.feedContainer.appendChild(focused);
            if(!this.trapBottom.includes(id))  this.trapBottom.push(id);
            return true;
        }else{
            return false;
        }
    }

    releaseBottom(id){
        let index = this.trapBottom.indexOf(id);
        while(index > -1){
            this.trapBottom.splice(index, 1);
            index = this.trapBottom.indexOf(id);
        }
    }

    reorderMessage(){
        var newMessageObject = {};
        Object.keys(this.messageListObject).sort().forEach((key) => {
            newMessageObject[key] = this.messageListObject[key];
        })
        this.messageListObject = newMessageObject;
        var orderBuffer = 0;
        Object.keys(this.messageListObject).forEach(key => {
            if(!this.trapBottom.includes(key)){
                if(this.messageListObject[key] != undefined){
                    orderBuffer++;
                    this.messageListObject[key].style.order = orderBuffer;
                }else{
                    delete this.messageListObject[key]
                }
            }
        })
    }

    getOrderKey(order, id){
        return order+""+id
    }

    /**
     * Add reply to a existing message, return false whem message does not exist 
     * 
     * @param {string | object} messageId  id for the message that is going to be inserted into
     * @param {string} replyId  the new id for the reply to access later
     * @param {string} senderName  the name of the person that replied
     * @param {string} senderId  id for the sender Id for skipping to it afterwards
     * @param {string} replyHtml  reply content in html
     * @param {string} updateMessageOrder update to the message order in the list that it is in
     * @returns {boolean} if message to reply to exists
     */
    pushReply(messageIdOrReplyObject, replyId, senderName, senderId, replyHtml, updateMessageOrder, typing, background){
        let messageId;
        if(typeof messageIdOrReplyObject === 'object'){
            let replyObject = messageIdOrReplyObject;
            if(!replyObject.hasOwnProperty("id")) return false;
            if(!replyObject.hasOwnProperty("replyToId")) return false;
            if(!replyObject.hasOwnProperty("message")) return false;

            messageId = replyObject.replyToId
            replyId = replyObject.id
            replyHtml = replyObject.message
            senderName = replyObject.hasOwnProperty("senderName") ? replyObject.senderName : "";
            senderId = replyObject.hasOwnProperty("senderId") ? replyObject.senderId : -1;
            updateMessageOrder = replyObject.hasOwnProperty("updateParentOrder") ? replyObject.updateParentOrder : null;
            typing = replyObject.hasOwnProperty("typing") ? replyObject.typing : false;
            background = replyObject.hasOwnProperty("background") ? replyObject.background : null;
        }else{
            messageId = messageIdOrReplyObject
        }

        var messageContainer = document.getElementById("messageFeed-"+messageId+"-container");
        if(messageContainer){
            let previousReplyObject = document.getElementById("messageFeed-"+messageId+"-reply-"+replyId+"-inner");
            let parentElementValue = document.getElementById("messageFeed-"+messageId).value.split("-");
            let alignValue = parentElementValue[0] == "right" ? "right" : "left";
            
            //update keyOrder in messageListObject
            if(updateMessageOrder){
                let newOrderKey = this.getOrderKey(updateMessageOrder, messageId)
                this.messageListObject[newOrderKey] = this.messageListObject[parentElementValue[1]];
                delete this.messageListObject[parentElementValue[1]];
                document.getElementById("messageFeed-"+messageId).value = parentElementValue[0]+"-"+newOrderKey;
            }
            
            if(previousReplyObject){
                previousReplyObject.innerHTML = replyHtml;
            }else{
                //check if it is align right or left
                var right = alignValue == "right" ? true : false;
    
                //make reply object and appendChild to messageContainer
                var replyDiv = document.createElement("DIV");
                
                replyDiv.classList.add("messageSender-"+senderId);
                replyDiv.id = "messageFeed-"+messageId+"-reply-"+replyId;
                replyDiv.style.cssText = right ? "display:flex; justify-content: flex-end; flex-direction: row;" :
                                            "display:flex; justify-content: flex-start; flex-direction: row;";
    
                var alignItems =  right ? "flex-end" : "flex-start";
                replyDiv.innerHTML = "<div style='padding-left: 80px; padding-right: 80px; max-width: calc(61% + 180px);  margin-top: 2px; display: flex; justify-content: flex-end; flex-grow: 0; align-items: flex-start; flex-direction: row;'>"+
                    "<div class='bubbleForThought bubbleForThought-"+alignValue+"'></div>"+
                    "<div style='display: flex; order: 2; flex-direction: column; align-items: "+alignItems+";'>"+
                        "<h6 style='font-size: 0.88rem; font-weight: normal; margin: 0px; padding-left: 33px; padding-right: 41px; color: rgb(146, 146, 146); width: unset;'>"+
                            senderName + "</h6>"+
                        "<div class='thoughtBubble thoughtBubble-"+alignValue+" "+(typing ? "typing" : "")+"' style='background: "+(background ? background : "")+";'>"+
                            "<div class='card-body' id='messageFeed-"+messageId+"-reply-"+replyId+"-inner' style='padding: 12px 10px 4px 17px;'>"+
                                replyHtml+
                            "</div>"+
                        "</div>"+
                    "</div>"+
                "</div>";
    
                messageContainer.appendChild(replyDiv);
            }
            return true;
        }else{
            //message does not exists yet
            return false;
        }
    }

    /**
     * Jump to the senderId's next message 
     * 
     * @param {number} senderId  * 
     * @return {string | boolean} messageObjectId , id for the message object
     */
    jumpToSenderNext(senderId){
        var possibleMessages = document.getElementsByClassName("messageSender-"+senderId);
        if(possibleMessages.length <= 0 ) return false;
        
        var validMessages = []; //messages that are more downwards of the scroll
        var uncountable = 0;

        var feedScrollPosition = this.feedObject.scrollTop;

        var inscrollableRegion = this.feedObject.scrollHeight - this.feedObject.clientHeight;

        
        for(var i = 0; i < possibleMessages.length; i++){
            let message = possibleMessages[i];
            if( getPosition(message) - 200 > inscrollableRegion) {
                uncountable++;
                continue;
            }

            if( (message.id.includes("reply") ? getPosition(message) - 310 : getPosition(message) - 100) > feedScrollPosition ) {
                validMessages.push(message);
            }
        }

        var findIndex;

        var scrollToItem;
        if(validMessages.length <= 0){
            scrollToItem = this.findLeastOrder(possibleMessages)
            findIndex = "1/" + possibleMessages.length;
        }else{
            scrollToItem = this.findLeastOrder(validMessages)
            if(validMessages.length == possibleMessages){
                findIndex = "1/" + possibleMessages.length;
            }else{
                findIndex = (possibleMessages.length - (validMessages.length + uncountable) + 1) + "/" + possibleMessages.length;
            }
        }
        this.feedObject.scrollTop = scrollToItem.id.includes("reply") ? getPosition(scrollToItem) - 300 : getPosition(scrollToItem) - 100;

        let flashMessage = scrollToItem.id.includes("reply") ? scrollToItem : scrollToItem.children[0].children[0]
        flashMessage.style.transition = "150ms ease-in all";
        flashMessage.style.background = "#f5f5002b";
        setTimeout(() => {
            flashMessage.style.background = "transparent";
        }, 1000)
        
        return findIndex;
    }

    findLeastOrder(elementArray){
        var leastIndex = 0;
        var orderBuffer = 999999;
        for(var i = 0; i < elementArray.length; i++){
            if(elementArray[i]){
                let isReply = elementArray[i].id.includes("reply")
                let element = isReply ? elementArray[i].parentElement.parentElement : elementArray[i]; 
                var focusedOrder = parseInt(element.style.order)
                if(focusedOrder < orderBuffer){
                    leastIndex = i;
                    orderBuffer = focusedOrder;
                }else if(focusedOrder == orderBuffer){
                    if(!isReply){
                        leastIndex = i;
                        orderBuffer = focusedOrder;
                    }
                }
            }
        }
        return elementArray[leastIndex]
    }

    findLargestOrder(elementArray){
        var mostIndex = 0;
        var orderBuffer = 0;
        for(var i = 0; i < elementArray.length; i++){
            var focusedOrder = parseInt(elementArray[i].style.order)
            if(focusedOrder > orderBuffer){
                mostIndex = i;
                orderBuffer = focusedOrder;
            }
        }
        return elementArray[mostIndex]
    }

    updateMessage(id, messageString){
        var messageInner = document.getElementById("messageFeed-"+id+"-inner")
        messageInner.innerHTML = messageString;
    }
}

function getPosition(object){
    var left = 0, top = 0;

    do {
        left += object.offsetLeft;
        top += object.offsetTop;
    } while ( object = object.offsetParent );

    return top;

}
